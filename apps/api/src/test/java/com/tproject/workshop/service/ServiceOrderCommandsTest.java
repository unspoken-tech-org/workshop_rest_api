package com.tproject.workshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tproject.workshop.dto.device.DeviceInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.errorhandling.ErrorCode;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.ServiceOrderConflictException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceOrderCommandsTest {

    private static final int DEVICE_ID = 1;
    private static final Timestamp NOW = Timestamp.from(TestClock.FIXED.instant());

    @Mock private DeviceRepository deviceRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerDeviceRepository customerDeviceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ColorService colorService;
    @Mock private TypesBrandsModelsService typeBrandModelService;
    @Mock private TechnicianService technicianService;
    @Mock private ObservabilityEventRecorder eventRecorder;
    @Spy private Clock clock = TestClock.FIXED;

    @InjectMocks private ServiceOrderCommands commands;

    private ServiceOrder device;

    @BeforeEach
    void setUp() {
        device = anOrder(DeviceTestBuilder.aDevice());
    }

    private static ServiceOrder anOrder(DeviceTestBuilder builder) {
        return builder.withId(DEVICE_ID)
                .withStatus(DeviceStatusEnum.EM_ANDAMENTO)
                .withBudgetFee(new BigDecimal("50.00"))
                .withServiceValue(null)
                .build();
    }

    private void stubFindById() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
    }

    @Test
    void changeStatus_shouldStampClosedAtAndRecordEvents_whenOrderCloses() {
        stubFindById();

        commands.changeStatus(DEVICE_ID, DeviceStatusEnum.ENTREGUE, "status");

        assertThat(device.getClosedAt()).isEqualTo(NOW);
        assertThat(device.getDepartureDate()).isEqualTo(NOW);
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_STATUS_CHANGED,
                        Map.of(
                                "status.previous", "em_andamento",
                                "status.new", "entregue",
                                "change.origin", "status"));
        verify(eventRecorder).record(eq(ObservabilityEventCatalog.DEVICE_CLOSED), anyMap());
    }

    @Test
    void changeStatus_shouldRejectReopeningClosedOrder() {
        device = closedOrder(DeviceStatusEnum.ENTREGUE);
        stubFindById();

        assertThatThrownBy(() -> commands.changeStatus(DEVICE_ID, DeviceStatusEnum.NOVO, "status"))
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_ORDER_CLOSED);
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.ENTREGUE);
        verifyNoInteractions(eventRecorder);
    }

    @Test
    void markUrgent_shouldRejectClosedOrderInsteadOfReopeningIt() {
        device = closedOrder(DeviceStatusEnum.DESCARTADO);
        stubFindById();

        assertThatThrownBy(() -> commands.markUrgent(DEVICE_ID, true))
                .isInstanceOf(ServiceOrderConflictException.class);
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.DESCARTADO);
        assertThat(device.isUrgency()).isFalse();
    }

    @Test
    void markUrgent_shouldRecordOnlyWhenUrgencyChanges() {
        device = anOrder(DeviceTestBuilder.aDevice().withUrgency(true));
        stubFindById();

        commands.markUrgent(DEVICE_ID, true);
        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());

        commands.markUrgent(DEVICE_ID, false);
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "normal"));
    }

    @Test
    void markRevision_shouldRejectClosedOrder() {
        device = closedOrder(DeviceStatusEnum.ENTREGUE);
        stubFindById();

        assertThatThrownBy(() -> commands.markRevision(DEVICE_ID, true))
                .isInstanceOf(ServiceOrderConflictException.class);
        assertThat(device.isRevision()).isFalse();
    }

    @Test
    void update_shouldRecordBudgetChangesWithBandsOnly() {
        stubFindById();

        commands.update(update(new BigDecimal("150.00"), new BigDecimal("600.00"), false));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field", "budget_fee",
                                "budget.change_type", "increase",
                                "budget.value_band", "medium"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field", "service_value",
                                "budget.change_type", "set",
                                "budget.value_band", "high"));
    }

    @Test
    void update_shouldRecordClearedAndDecrease() {
        stubFindById();
        device.setServiceValue(new BigDecimal("300.00"));

        commands.update(update(BigDecimal.ZERO, new BigDecimal("80.00"), false));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field", "budget_fee",
                                "budget.change_type", "cleared",
                                "budget.value_band", "zero"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                        Map.of(
                                "budget.field", "service_value",
                                "budget.change_type", "decrease",
                                "budget.value_band", "low"));
    }

    @Test
    void update_shouldNotRecordBudgetOrUrgency_whenUnchanged() {
        stubFindById();

        commands.update(update(new BigDecimal("50"), BigDecimal.ZERO, false));

        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED), anyMap());
        verify(eventRecorder, never())
                .record(eq(ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED), anyMap());
    }

    @Test
    void update_shouldRecordUrgencyChange() {
        stubFindById();

        commands.update(update(new BigDecimal("50.00"), null, true));

        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "urgent"));
    }

    @Test
    void update_shouldRefreshBudgetFeeCollectedFromPaidFees() {
        stubFindById();
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(eq(DEVICE_ID), any()))
                .thenReturn(new BigDecimal("50.00"));

        commands.update(update(new BigDecimal("50.00"), null, false));

        assertThat(device.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void update_shouldAcceptIdenticalPayloadOnClosedOrder() {
        Timestamp departure = Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 15, 30));
        device = closedOrder(DeviceStatusEnum.ENTREGUE, departure);
        device.setProblem("problema");
        stubFindById();

        commands.update(
                request(DeviceStatusEnum.ENTREGUE, new BigDecimal("50.00"), null, true, null));

        assertThat(device.getDepartureDate()).isEqualTo(departure);
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.ENTREGUE);
        assertThat(device.isUrgency()).isFalse();
    }

    @Test
    void update_shouldRejectRealChangeOnClosedOrder() {
        device = closedOrder(DeviceStatusEnum.ENTREGUE);
        device.setProblem("problema");
        stubFindById();
        DeviceUpdateInputDtoRecord request =
                request(DeviceStatusEnum.ENTREGUE, new BigDecimal("99.00"), null, false, null);

        assertThatThrownBy(() -> commands.update(request))
                .isInstanceOf(ServiceOrderConflictException.class);
    }

    @Test
    void update_shouldRejectReopeningClosedOrder() {
        device = closedOrder(DeviceStatusEnum.ENTREGUE);
        device.setProblem("problema");
        stubFindById();
        DeviceUpdateInputDtoRecord request =
                request(DeviceStatusEnum.EM_ANDAMENTO, new BigDecimal("50.00"), null, false, null);

        assertThatThrownBy(() -> commands.update(request))
                .isInstanceOf(ServiceOrderConflictException.class);
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.ENTREGUE);
        assertThat(device.getDepartureDate()).isNotNull();
    }

    @Test
    void update_shouldStampDepartureDate_whenClosingWithoutOne() {
        stubFindById();

        commands.update(
                request(DeviceStatusEnum.DESCARTADO, new BigDecimal("50.00"), null, false, null));

        assertThat(device.getDepartureDate()).isEqualTo(NOW);
        assertThat(device.getClosedAt()).isEqualTo(NOW);
        assertThat(device.getWarrantyDays()).isNull();
    }

    @Test
    void update_shouldRejectChangingRegisteredDepartureDate() {
        device =
                anOrder(
                        DeviceTestBuilder.aDevice()
                                .withEntryDate(
                                        Timestamp.valueOf(LocalDateTime.of(2026, 5, 1, 0, 0)))
                                .withDepartureDate(
                                        Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 0, 0))));
        device.setProblem("problema");
        stubFindById();
        DeviceUpdateInputDtoRecord request =
                request(
                        DeviceStatusEnum.EM_ANDAMENTO,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        LocalDateTime.of(2026, 5, 11, 0, 0));

        assertThatThrownBy(() -> commands.update(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_shouldRejectDepartureDateBeforeEntryDate() {
        device =
                anOrder(
                        DeviceTestBuilder.aDevice()
                                .withEntryDate(
                                        Timestamp.valueOf(LocalDateTime.of(2026, 5, 10, 10, 0))));
        stubFindById();
        DeviceUpdateInputDtoRecord request =
                request(
                        DeviceStatusEnum.ENTREGUE,
                        new BigDecimal("50.00"),
                        null,
                        false,
                        LocalDateTime.of(2026, 5, 9, 10, 0));

        assertThatThrownBy(() -> commands.update(request)).isInstanceOf(BadRequestException.class);
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.EM_ANDAMENTO);
    }

    @Test
    void update_shouldResetOnlyUrgencyAndKeepRevision_withUppercaseHistory() {
        device = anOrder(DeviceTestBuilder.aDevice().withUrgency(true).withRevision(true));
        stubFindById();

        commands.update(
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
        assertThat(device.isRevision()).isTrue();
        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.PRONTO);
        assertThat(device.getDeviceHistory())
                .extracting(
                        ServiceOrderHistory::getFieldName,
                        ServiceOrderHistory::getOldValue,
                        ServiceOrderHistory::getNewValue)
                .containsExactlyInAnyOrder(
                        tuple(DeviceHistoryFieldEnum.STATUS.getField(), "EM_ANDAMENTO", "PRONTO"),
                        tuple(DeviceHistoryFieldEnum.URGENCY.getField(), "true", "false"));
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                        Map.of("device.urgency", "normal"));
    }

    @Test
    void create_shouldDeduplicateEquivalentColorsBeforePersisting() {
        Customer customer = customer(1);
        BrandsModelsTypes bmt = new BrandsModelsTypes();
        bmt.setId(5);
        Color blue = new Color(3, "azul", null, null);

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(typeBrandModelService.createOrReturnExistentBrandModelType(any())).thenReturn(bmt);
        when(colorService.createOrReturnExistentColor("azul")).thenReturn(blue);
        when(colorService.createOrReturnExistentColor("Azul")).thenReturn(blue);
        when(customerDeviceRepository.save(any(CustomerDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubSaveOrder();

        int id = commands.create(input(1, null, false));

        assertThat(id).isEqualTo(99);
        ServiceOrder order = savedOrder();
        assertThat(order.getColorIds()).containsExactly(3);
        assertThat(order.getCustomerDevice().getColorIds()).containsExactly(3);
        assertThat(order.getCustomerDevice().getCustomer()).isSameAs(customer);
        assertThat(order.getCustomerDevice().getBrandsModelsTypes()).isSameAs(bmt);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getBrandsModelsTypes()).isSameAs(bmt);
        assertThat(order.getWarrantyDays()).isEqualTo(ServiceOrder.DEFAULT_WARRANTY_DAYS);
        assertThat(order.getDeviceStatus()).isEqualTo(DeviceStatusEnum.NOVO);
        assertThat(order.getEntryDate()).isEqualTo(NOW);
        assertThat(order.getOriginOrder()).isNull();
        assertThat(order.isRevision()).isFalse();
    }

    @Test
    void create_shouldReuseOriginDeviceAndOpenRevision_whenOriginOrderIsGiven() {
        Customer customer = customer(1);
        CustomerDevice physical = physicalDevice(customer);
        ServiceOrder origin = closedOrder(DeviceStatusEnum.ENTREGUE);
        origin.assignCustomerDevice(physical);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(origin));
        when(deviceRepository.existsByCustomerDeviceIdAndDeviceStatus(
                        40, DeviceStatusEnum.DESCARTADO))
                .thenReturn(false);
        stubSaveOrder();

        commands.create(input(1, DEVICE_ID, true));

        ServiceOrder order = savedOrder();
        assertThat(order.getCustomerDevice()).isSameAs(physical);
        assertThat(order.getOriginOrder()).isSameAs(origin);
        assertThat(order.isRevision()).isTrue();
        assertThat(order.getDeviceHistory())
                .extracting(ServiceOrderHistory::getFieldName, ServiceOrderHistory::getNewValue)
                .contains(tuple(DeviceHistoryFieldEnum.REVISION.getField(), "true"));
        verifyNoInteractions(customerDeviceRepository, typeBrandModelService, colorService);
    }

    @Test
    void create_shouldRejectOriginOrderOfAnotherCustomer() {
        ServiceOrder origin = closedOrder(DeviceStatusEnum.ENTREGUE);
        origin.assignCustomerDevice(physicalDevice(customer(2)));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer(1)));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(origin));
        DeviceInputDtoRecord request = input(1, DEVICE_ID, true);

        assertThatThrownBy(() -> commands.create(request)).isInstanceOf(BadRequestException.class);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectDisposedOriginOrder() {
        Customer customer = customer(1);
        ServiceOrder origin = closedOrder(DeviceStatusEnum.DESCARTADO);
        origin.assignCustomerDevice(physicalDevice(customer));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(origin));
        when(deviceRepository.existsByCustomerDeviceIdAndDeviceStatus(
                        40, DeviceStatusEnum.DESCARTADO))
                .thenReturn(true);
        DeviceInputDtoRecord request = input(1, DEVICE_ID, false);

        assertThatThrownBy(() -> commands.create(request))
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_DEVICE_DISPOSED);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectDeliveredOrigin_whenDeviceWasDiscardedLater() {
        Customer customer = customer(1);
        ServiceOrder origin = closedOrder(DeviceStatusEnum.ENTREGUE);
        origin.assignCustomerDevice(physicalDevice(customer));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(origin));
        when(deviceRepository.existsByCustomerDeviceIdAndDeviceStatus(
                        40, DeviceStatusEnum.DESCARTADO))
                .thenReturn(true);
        DeviceInputDtoRecord request = input(1, DEVICE_ID, true);

        assertThatThrownBy(() -> commands.create(request))
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_DEVICE_DISPOSED);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void ensureOpenForContact_shouldRejectClosedOrder() {
        device = closedOrder(DeviceStatusEnum.ENTREGUE);
        stubFindById();

        assertThatThrownBy(() -> commands.ensureOpenForContact(DEVICE_ID))
                .isInstanceOf(ServiceOrderConflictException.class);
    }

    @Test
    void applyContactStatus_shouldChangeStatusWithContactOrigin() {
        stubFindById();

        commands.applyContactStatus(DEVICE_ID, DeviceStatusEnum.AGUARDANDO);

        assertThat(device.getDeviceStatus()).isEqualTo(DeviceStatusEnum.AGUARDANDO);
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.DEVICE_STATUS_CHANGED,
                        Map.of(
                                "status.previous", "em_andamento",
                                "status.new", "aguardando",
                                "change.origin", "customer_contact"));
    }

    private static ServiceOrder closedOrder(DeviceStatusEnum status) {
        return closedOrder(status, NOW);
    }

    private static ServiceOrder closedOrder(DeviceStatusEnum status, Timestamp departure) {
        return DeviceTestBuilder.aDevice()
                .withId(DEVICE_ID)
                .withStatus(status)
                .withBudgetFee(new BigDecimal("50.00"))
                .withServiceValue(null)
                .withEntryDate(Timestamp.valueOf(LocalDateTime.of(2025, 12, 1, 10, 0)))
                .withDepartureDate(departure)
                .withClosedAt(departure)
                .build();
    }

    private static Customer customer(int id) {
        Customer customer = new Customer();
        customer.setIdCustomer(id);
        return customer;
    }

    private static CustomerDevice physicalDevice(Customer customer) {
        CustomerDevice physical = new CustomerDevice(customer, new BrandsModelsTypes(), List.of(3));
        physical.setId(40);
        return physical;
    }

    private void stubSaveOrder() {
        when(deviceRepository.save(any(ServiceOrder.class)))
                .thenAnswer(invocation -> {
                    ServiceOrder saved = invocation.getArgument(0);
                    saved.setId(99);
                    return saved;
                });
    }

    private ServiceOrder savedOrder() {
        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(deviceRepository).save(captor.capture());
        return captor.getValue();
    }

    private static DeviceInputDtoRecord input(
            int customerId, Integer originOrderId, boolean revision) {
        return new DeviceInputDtoRecord(
                customerId,
                new TypeBrandModelInputDtoRecord("Celular", "Samsung", "A54"),
                null,
                "Tela quebrada",
                null,
                new BigDecimal("45.00"),
                List.of("azul", "Azul"),
                false,
                originOrderId,
                revision);
    }

    private static DeviceUpdateInputDtoRecord update(
            BigDecimal budgetFee, BigDecimal serviceValue, boolean urgency) {
        return request(DeviceStatusEnum.EM_ANDAMENTO, budgetFee, serviceValue, urgency, null);
    }

    private static DeviceUpdateInputDtoRecord request(
            DeviceStatusEnum status,
            BigDecimal budgetFee,
            BigDecimal serviceValue,
            boolean urgency,
            LocalDateTime departureDate) {
        return new DeviceUpdateInputDtoRecord(
                DEVICE_ID,
                status,
                "problema",
                null,
                null,
                budgetFee,
                serviceValue,
                urgency,
                false,
                null,
                null,
                departureDate);
    }
}
