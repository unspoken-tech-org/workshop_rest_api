package com.tproject.workshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tproject.workshop.dto.device.DeviceInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceStatusInputRecord;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUrgencyInputRecord;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.integration.support.TestClock;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.CustomerDevice;
import com.tproject.workshop.model.ServiceOrder;
import com.tproject.workshop.model.ServiceOrderHistory;
import com.tproject.workshop.repository.CustomerDeviceRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import com.tproject.workshop.support.fixtures.DeviceTestBuilder;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final int DEVICE_ID = 1;

    @Mock private DeviceRepository deviceRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerDeviceRepository customerDeviceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ColorService colorService;
    @Mock private TypesBrandsModelsService typeBrandModelService;
    @Mock private TechnicianService technicianService;
    @Mock private ObservabilityEventRecorder eventRecorder;
    @Spy private Clock clock = TestClock.FIXED;

    @InjectMocks private DeviceService deviceService;

    private ServiceOrder device;

    @BeforeEach
    void setUp() {
        device =
                DeviceTestBuilder.aDevice()
                        .withId(DEVICE_ID)
                        .withStatus(DeviceStatusEnum.EM_ANDAMENTO)
                        .withBudgetFee(new BigDecimal("50.00"))
                        .withServiceValue(null)
                        .build();
    }

    private void stubFindById() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
    }

    private void stubDeviceLookup() {
        stubFindById();
        when(deviceRepository.findByDeviceId(anyInt()))
                .thenReturn(Optional.of(mock(DeviceOutputDto.class)));
    }

    @Test
    void shouldStampClosedAt_whenStatusBecomesClosed() {
        stubFindById();

        deviceService.updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.ENTREGUE);

        assertThat(device.getClosedAt()).isEqualTo(Timestamp.from(TestClock.FIXED.instant()));
    }

    @Test
    void shouldClearClosedAt_whenClosedOrderIsReopened() {
        device.setDeviceStatus(DeviceStatusEnum.ENTREGUE);
        device.setClosedAt(Timestamp.from(TestClock.FIXED.instant()));
        stubFindById();

        deviceService.updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.NOVO);

        assertThat(device.getClosedAt()).isNull();
    }

    @Test
    void updateDevice_shouldRecordBudgetChangesWithBandsOnly() {
        stubDeviceLookup();
        deviceService.updateDevice(
                update(new BigDecimal("150.00"), new BigDecimal("600.00"), false));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field",
                                "budget_fee",
                                "budget.change_type",
                                "increase",
                                "budget.value_band",
                                "medium"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field",
                                "service_value",
                                "budget.change_type",
                                "set",
                                "budget.value_band",
                                "high"));
    }

    @Test
    void updateDevice_shouldRecordClearedAndDecrease() {
        stubDeviceLookup();
        device.setServiceValue(new BigDecimal("300.00"));

        deviceService.updateDevice(update(BigDecimal.ZERO, new BigDecimal("80.00"), false));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field",
                                "budget_fee",
                                "budget.change_type",
                                "cleared",
                                "budget.value_band",
                                "zero"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field",
                                "service_value",
                                "budget.change_type",
                                "decrease",
                                "budget.value_band",
                                "low"));
    }

    @Test
    void updateDevice_shouldNotRecordBudgetOrUrgency_whenUnchanged() {
        stubDeviceLookup();
        deviceService.updateDevice(update(new BigDecimal("50"), BigDecimal.ZERO, false));

        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED), anyMap());
        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());
    }

    @Test
    void updateDevice_shouldRecordUrgencyChange() {
        stubDeviceLookup();
        deviceService.updateDevice(update(new BigDecimal("50.00"), null, true));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "urgent"));
    }

    @Test
    void updateDeviceUrgency_shouldRecordOnlyWhenUrgencyChanges() {
        stubDeviceLookup();
        device.setUrgency(true);

        deviceService.updateDeviceUrgency(DEVICE_ID, new DeviceUrgencyInputRecord(true));
        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());

        deviceService.updateDeviceUrgency(DEVICE_ID, new DeviceUrgencyInputRecord(false));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "normal"));
        verify(deviceRepository, times(2)).saveAndFlush(any(ServiceOrder.class));
    }

    @Test
    void updateDevice_shouldPreservePersistedDepartureDateWhenOmitted() {
        stubDeviceLookup();
        LocalDateTime entry = LocalDateTime.of(2026, 5, 1, 10, 0);
        Timestamp persistedDeparture = Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 15, 30));
        device.setEntryDate(Timestamp.valueOf(entry));
        device.setDepartureDate(persistedDeparture);
        device.setDeviceStatus(DeviceStatusEnum.ENTREGUE);

        deviceService.updateDevice(
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.ENTREGUE,
                        "problema",
                        "obs atualizada",
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        null,
                        null));

        assertThat(device.getDepartureDate()).isEqualTo(persistedDeparture);
    }

    @Test
    void updateDevice_shouldSetDepartureDateToNowWhenOmittedAndCurrentlyNull() {
        stubDeviceLookup();
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0)));
        device.setDepartureDate(null);

        deviceService.updateDevice(
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.DESCARTADO,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        null,
                        null));

        assertThat(device.getDepartureDate()).isNotNull();
    }

    @Test
    void updateDevice_shouldRejectDepartureDateBeforeExistingEntryDate() {
        stubFindById();
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 10, 0)));

        DeviceUpdateInputDtoRecord request =
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.ENTREGUE,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        null,
                        LocalDateTime.of(2026, 5, 9, 10, 0));

        assertThatThrownBy(() -> deviceService.updateDevice(request))
                .isInstanceOf(BadRequestException.class);
        verify(deviceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDevice_shouldRejectEntryDateAfterExistingDepartureDate() {
        stubFindById();
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 10, 0)));
        device.setDepartureDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 10, 0)));

        DeviceUpdateInputDtoRecord request =
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.ENTREGUE,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        LocalDateTime.of(2026, 5, 11, 10, 0),
                        null);

        assertThatThrownBy(() -> deviceService.updateDevice(request))
                .isInstanceOf(BadRequestException.class);
        verify(deviceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDevice_shouldAllowDepartureDateEqualToEntryDate() {
        stubDeviceLookup();
        LocalDateTime sameInstant = LocalDateTime.of(2026, 5, 10, 10, 0);
        device.setEntryDate(Timestamp.valueOf(sameInstant));

        deviceService.updateDevice(
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.ENTREGUE,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        sameInstant,
                        sameInstant));

        assertThat(device.getDepartureDate()).isEqualTo(Timestamp.valueOf(sameInstant));
        verify(deviceRepository).saveAndFlush(device);
    }

    @Test
    void updateDevice_shouldResetUrgencyAndRevisionWithCanonicalUppercaseHistory() {
        stubDeviceLookup();
        device.setUrgency(true);
        device.setRevision(true);

        deviceService.updateDevice(
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.PRONTO,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        true,
                        true,
                        null,
                        null,
                        null));

        assertThat(device.isUrgency()).isFalse();
        assertThat(device.isRevision()).isFalse();
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.PRONTO);
        assertThat(device.getDeviceHistory())
                .extracting(
                        ServiceOrderHistory::getFieldName,
                        ServiceOrderHistory::getOldValue,
                        ServiceOrderHistory::getNewValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.STATUS.getField(), "EM_ANDAMENTO", "PRONTO"),
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.URGENCY.getField(), "true", "false"),
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.REVISION.getField(), "true", "false"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "normal"));
    }

    @Test
    void updateDevice_shouldClearDepartureDateWhenReopeningClosedDevice() {
        stubDeviceLookup();
        device.setDeviceStatus(DeviceStatusEnum.ENTREGUE);
        device.setEntryDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 10, 0)));
        device.setDepartureDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 15, 0)));

        deviceService.updateDevice(
                new DeviceUpdateInputDtoRecord(
                        DEVICE_ID,
                        DeviceStatusEnum.EM_ANDAMENTO,
                        "problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        false,
                        null,
                        LocalDateTime.of(2026, 5, 12, 10, 0),
                        null));

        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.EM_ANDAMENTO);
        assertThat(device.getDepartureDate()).isNull();
    }

    @Test
    void updateDeviceStatus_shouldRecordResetHistoryAndClearDepartureDateWhenReopened() {
        stubDeviceLookup();
        device.setUrgency(true);
        device.setRevision(true);

        deviceService.updateDeviceStatus(
                DEVICE_ID, new DeviceStatusInputRecord(DeviceStatusEnum.ENTREGUE));

        assertThat(device.isUrgency()).isFalse();
        assertThat(device.isRevision()).isFalse();
        assertThat(device.getDepartureDate()).isNotNull();
        assertThat(device.getDeviceHistory())
                .extracting(
                        ServiceOrderHistory::getFieldName,
                        ServiceOrderHistory::getOldValue,
                        ServiceOrderHistory::getNewValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.STATUS.getField(),
                                "EM_ANDAMENTO",
                                "ENTREGUE"),
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.URGENCY.getField(), "true", "false"),
                        org.assertj.core.groups.Tuple.tuple(
                                DeviceHistoryFieldEnum.REVISION.getField(), "true", "false"));

        deviceService.updateDeviceStatus(
                DEVICE_ID, new DeviceStatusInputRecord(DeviceStatusEnum.AGUARDANDO));

        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.AGUARDANDO);
        assertThat(device.getDepartureDate()).isNull();
    }

    @Test
    void updateDeviceUrgency_shouldClearDepartureDateWhenReopeningClosedDevice() {
        stubDeviceLookup();
        device.setDeviceStatus(DeviceStatusEnum.DESCARTADO);
        device.setUrgency(false);
        device.setDepartureDate(Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 15, 0)));

        deviceService.updateDeviceUrgency(DEVICE_ID, new DeviceUrgencyInputRecord(true));

        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.NOVO);
        assertThat(device.isUrgency()).isTrue();
        assertThat(device.getDepartureDate()).isNull();
    }

    @Test
    void createDevice_shouldDeduplicateEquivalentColorsBeforePersisting() {
        Customer customer = new Customer();
        customer.setIdCustomer(1);
        BrandsModelsTypes bmt = new BrandsModelsTypes();
        bmt.setId(5);
        Color blue = new Color(3, "azul", null, null);

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(typeBrandModelService.createOrReturnExistentBrandModelType(any())).thenReturn(bmt);
        when(colorService.createOrReturnExistentColor("azul")).thenReturn(blue);
        when(colorService.createOrReturnExistentColor("Azul")).thenReturn(blue);
        when(customerDeviceRepository.save(any(CustomerDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceRepository.saveAndFlush(any(ServiceOrder.class)))
                .thenAnswer(invocation -> {
                    ServiceOrder saved = invocation.getArgument(0);
                    saved.setId(99);
                    return saved;
                });

        deviceService.createDevice(
                new DeviceInputDtoRecord(
                        1,
                        new TypeBrandModelInputDtoRecord("Celular", "Samsung", "A54"),
                        null,
                        "Tela quebrada",
                        null,
                        new BigDecimal("45.00"),
                        List.of("azul", "Azul"),
                        false));

        var deviceCaptor = org.mockito.ArgumentCaptor.forClass(ServiceOrder.class);
        verify(deviceRepository).saveAndFlush(deviceCaptor.capture());
        ServiceOrder order = deviceCaptor.getValue();
        assertThat(order.getColorIds()).containsExactly(3);
        assertThat(order.getCustomerDevice().getColorIds()).containsExactly(3);
        assertThat(order.getCustomerDevice().getCustomer()).isSameAs(customer);
        assertThat(order.getCustomerDevice().getBrandsModelsTypes()).isSameAs(bmt);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getBrandsModelsTypes()).isSameAs(bmt);
        assertThat(order.getWarrantyDays()).isEqualTo(ServiceOrder.DEFAULT_WARRANTY_DAYS);
    }

    private static DeviceUpdateInputDtoRecord update(
            BigDecimal budgetFee, BigDecimal serviceValue, boolean urgency) {
        return new DeviceUpdateInputDtoRecord(
                DEVICE_ID,
                DeviceStatusEnum.EM_ANDAMENTO,
                "problema",
                null,
                null,
                budgetFee,
                serviceValue,
                urgency,
                false,
                null,
                null,
                null);
    }
}
