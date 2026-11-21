package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUrgencyInputRecord;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceServiceTest {

    private static final int DEVICE_ID = 1;

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ColorService colorService;
    @Mock
    private TypesBrandsModelsService typeBrandModelService;
    @Mock
    private TechnicianService technicianService;
    @Mock
    private ObservabilityEventRecorder eventRecorder;

    @InjectMocks
    private DeviceService deviceService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setId(DEVICE_ID);
        device.setDeviceStatus(DeviceStatusEnum.EM_ANDAMENTO);
        device.setDeviceHistory(new ArrayList<>());
        device.setBudgetFee(new BigDecimal("50.00"));
        device.setServiceValue(null);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(deviceRepository.findByDeviceId(anyInt())).thenReturn(Optional.of(mock(DeviceOutputDto.class)));
    }

    @Test
    void updateDevice_shouldRecordBudgetChangesWithBandsOnly() {
        deviceService.updateDevice(update(new BigDecimal("150.00"), new BigDecimal("600.00"), false));

        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED, Map.of(
                "budget.field", "budget_fee", "budget.change_type", "increase", "budget.value_band", "medium"));
        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED, Map.of(
                "budget.field", "service_value", "budget.change_type", "set", "budget.value_band", "high"));
    }

    @Test
    void updateDevice_shouldRecordClearedAndDecrease() {
        device.setServiceValue(new BigDecimal("300.00"));

        deviceService.updateDevice(update(BigDecimal.ZERO, new BigDecimal("80.00"), false));

        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED, Map.of(
                "budget.field", "budget_fee", "budget.change_type", "cleared", "budget.value_band", "zero"));
        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED, Map.of(
                "budget.field", "service_value", "budget.change_type", "decrease", "budget.value_band", "low"));
    }

    @Test
    void updateDevice_shouldNotRecordBudgetOrUrgency_whenUnchanged() {
        deviceService.updateDevice(update(new BigDecimal("50"), BigDecimal.ZERO, false));

        verify(eventRecorder, never()).record(eq(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED), anyMap());
        verify(eventRecorder, never()).record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());
    }

    @Test
    void updateDevice_shouldRecordUrgencyChange() {
        deviceService.updateDevice(update(new BigDecimal("50.00"), null, true));

        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                Map.of("device.urgency", "urgent"));
    }

    @Test
    void updateDeviceUrgency_shouldRecordOnlyWhenUrgencyChanges() {
        device.setUrgency(true);

        deviceService.updateDeviceUrgency(DEVICE_ID, new DeviceUrgencyInputRecord(true));
        verify(eventRecorder, never()).record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());

        deviceService.updateDeviceUrgency(DEVICE_ID, new DeviceUrgencyInputRecord(false));
        verify(eventRecorder).record(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                Map.of("device.urgency", "normal"));
        verify(deviceRepository, times(2)).saveAndFlush(any(Device.class));
    }

    @Test
    void updateDevice_shouldPreservePersistedDepartureDateWhenOmitted() {
        LocalDateTime entry = LocalDateTime.of(2026, 5, 1, 10, 0);
        Timestamp persistedDeparture = Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 15, 30));
        device.setEntryDate(Timestamp.valueOf(entry));
        device.setDepartureDate(persistedDeparture);
        device.setDeviceStatus(DeviceStatusEnum.ENTREGUE);

        deviceService.updateDevice(new DeviceUpdateInputDtoRecord(
                DEVICE_ID, "ENTREGUE", "problema", "obs atualizada", null,
                new BigDecimal("50.00"), null, false, false, null, null, null));

        assertThat(device.getDepartureDate()).isEqualTo(persistedDeparture);
    }

    @Test
    void updateDevice_shouldSetDepartureDateToNowWhenOmittedAndCurrentlyNull() {
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 10, 0)));
        device.setDepartureDate(null);

        deviceService.updateDevice(new DeviceUpdateInputDtoRecord(
                DEVICE_ID, "DESCARTADO", "problema", null, null,
                new BigDecimal("50.00"), null, false, false, null, null, null));

        assertThat(device.getDepartureDate()).isNotNull();
    }

    @Test
    void updateDevice_shouldRejectDepartureDateBeforeExistingEntryDate() {
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 10, 0)));

        DeviceUpdateInputDtoRecord request = new DeviceUpdateInputDtoRecord(
                DEVICE_ID, "ENTREGUE", "problema", null, null,
                new BigDecimal("50.00"), null, false, false, null,
                null, LocalDateTime.of(2026, 5, 9, 10, 0));

        assertThatThrownBy(() -> deviceService.updateDevice(request))
                .isInstanceOf(BadRequestException.class);
        verify(deviceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDevice_shouldRejectEntryDateAfterExistingDepartureDate() {
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 10, 0)));
        device.setDepartureDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 10, 0)));

        DeviceUpdateInputDtoRecord request = new DeviceUpdateInputDtoRecord(
                DEVICE_ID, "ENTREGUE", "problema", null, null,
                new BigDecimal("50.00"), null, false, false, null,
                LocalDateTime.of(2026, 5, 11, 10, 0), null);

        assertThatThrownBy(() -> deviceService.updateDevice(request))
                .isInstanceOf(BadRequestException.class);
        verify(deviceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDevice_shouldAllowDepartureDateEqualToEntryDate() {
        LocalDateTime sameInstant = LocalDateTime.of(2026, 5, 10, 10, 0);
        device.setEntryDate(Timestamp.valueOf(sameInstant));

        deviceService.updateDevice(new DeviceUpdateInputDtoRecord(
                DEVICE_ID, "ENTREGUE", "problema", null, null,
                new BigDecimal("50.00"), null, false, false, null,
                sameInstant, sameInstant));

        assertThat(device.getDepartureDate()).isEqualTo(Timestamp.valueOf(sameInstant));
        verify(deviceRepository).saveAndFlush(device);
    }

    private static DeviceUpdateInputDtoRecord update(BigDecimal budgetFee, BigDecimal serviceValue, boolean urgency) {
        return new DeviceUpdateInputDtoRecord(DEVICE_ID, "em_andamento", "problema", null, null,
                budgetFee, serviceValue, urgency, false, null, null, null);
    }
}
