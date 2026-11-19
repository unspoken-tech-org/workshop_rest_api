package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUrgencyInputRecord;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

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

    private static DeviceUpdateInputDtoRecord update(BigDecimal budgetFee, BigDecimal serviceValue, boolean urgency) {
        return new DeviceUpdateInputDtoRecord(DEVICE_ID, "em_andamento", "problema", null, null,
                budgetFee, serviceValue, urgency, false, null, null, null);
    }
}
