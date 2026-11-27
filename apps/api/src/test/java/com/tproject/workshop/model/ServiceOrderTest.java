package com.tproject.workshop.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.errorhandling.ErrorCode;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.ServiceOrderConflictException;
import com.tproject.workshop.integration.support.TestClock;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Pure unit tests of the aggregate: decisions 2, 3 and 6 to 9, no Spring and no database. */
class ServiceOrderTest {

    private static final Instant NOW = TestClock.FIXED.instant();
    private static final Duration WINDOW = Duration.ofHours(4);

    private static ServiceOrder openOrder() {
        Customer customer = new Customer();
        customer.setIdCustomer(1);
        CustomerDevice device = new CustomerDevice(customer, new BrandsModelsTypes(), List.of(3));
        return ServiceOrder.open(
                device,
                null,
                false,
                "Não liga",
                null,
                new BigDecimal("50.00"),
                false,
                null,
                NOW.minus(Duration.ofDays(2)));
    }

    private static ServiceOrder closedOrder(DeviceStatusEnum status) {
        ServiceOrder order = openOrder();
        order.changeStatus(DeviceStatusEnum.PRONTO, NOW.minus(Duration.ofHours(1)));
        order.changeStatus(status, NOW);
        return order;
    }

    @Test
    void open_shouldStartAsNewWithHistoryAndDefaultWarranty() {
        ServiceOrder order = openOrder();

        assertThat(order.getDeviceStatus()).isEqualTo(DeviceStatusEnum.NOVO);
        assertThat(order.getWarrantyDays()).isEqualTo(ServiceOrder.DEFAULT_WARRANTY_DAYS);
        assertThat(order.getCustomer().getIdCustomer()).isEqualTo(1);
        assertThat(order.getDeviceHistory())
                .extracting(ServiceOrderHistory::getFieldName, ServiceOrderHistory::getNewValue)
                .containsExactly(tuple(DeviceHistoryFieldEnum.STATUS.getField(), "NOVO"));
    }

    @Test
    void open_shouldLinkOriginAndMarkRevision() {
        ServiceOrder origin = closedOrder(DeviceStatusEnum.ENTREGUE);

        ServiceOrder revision =
                ServiceOrder.open(
                        origin.getCustomerDevice(),
                        origin,
                        true,
                        "Mesmo defeito",
                        null,
                        null,
                        false,
                        null,
                        NOW);

        assertThat(revision.getOriginOrder()).isSameAs(origin);
        assertThat(revision.getCustomerDevice()).isSameAs(origin.getCustomerDevice());
        assertThat(revision.isRevision()).isTrue();
        assertThat(revision.getBudgetFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void changeStatus_shouldStampDepartureAndClosedAt_whenClosing() {
        ServiceOrder order = openOrder();

        order.changeStatus(DeviceStatusEnum.ENTREGUE, NOW);

        assertThat(order.getDepartureDate()).isEqualTo(Timestamp.from(NOW));
        assertThat(order.getClosedAt()).isEqualTo(Timestamp.from(NOW));
        assertThat(order.isClosed()).isTrue();
    }

    @Test
    void changeStatus_shouldKeepRevisionAndResetUrgency_whenClosing() {
        ServiceOrder order = openOrder();
        order.markUrgent(true);
        order.markRevision(true);

        order.changeStatus(DeviceStatusEnum.ENTREGUE, NOW);

        assertThat(order.isUrgency()).isFalse();
        assertThat(order.isRevision()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"NAO_APROVADO", "DESCARTADO"})
    void changeStatus_shouldClearWarranty_whenStatusHasNoWarranty(DeviceStatusEnum status) {
        ServiceOrder order = openOrder();

        order.changeStatus(status, NOW);

        assertThat(order.getWarrantyDays()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeviceStatusEnum.class,
            names = {"ENTREGUE", "DESCARTADO"})
    void closedOrder_shouldRejectOperationalChanges(DeviceStatusEnum status) {
        ServiceOrder order = closedOrder(status);

        assertConflict(() -> order.changeStatus(DeviceStatusEnum.NOVO, NOW));
        assertConflict(() -> order.markUrgent(true));
        assertConflict(() -> order.markRevision(!order.isRevision()));
        assertConflict(() ->
                order.updateDetails(
                        "Outro defeito", null, null, new BigDecimal("50.00"), null, null));
        assertThat(order.getDeviceStatus()).isEqualTo(status);
    }

    @Test
    void closedOrder_shouldAcceptRequestThatRepeatsPersistedValues() {
        ServiceOrder order = closedOrder(DeviceStatusEnum.ENTREGUE);

        assertThat(order.changeStatus(DeviceStatusEnum.ENTREGUE, NOW)).isFalse();
        assertThat(order.markUrgent(false)).isFalse();
        order.updateDetails("Não liga", null, null, new BigDecimal("50"), null, null);
        order.changeDates(null, order.getDepartureDate().toLocalDateTime());
    }

    @Test
    void changeDates_shouldRejectChangingRegisteredDepartureDate() {
        ServiceOrder order = openOrder();
        LocalDateTime departure = LocalDateTime.of(2026, 6, 1, 10, 0);
        order.changeDates(null, departure);

        order.changeDates(null, departure);
        order.changeDates(null, null);

        assertThatThrownBy(() -> order.changeDates(null, departure.plusDays(1)))
                .isInstanceOf(BadRequestException.class);
        assertThat(order.getDepartureDate()).isEqualTo(Timestamp.valueOf(departure));
    }

    @Test
    void changeDates_shouldRejectDepartureBeforeEntry() {
        ServiceOrder order = openOrder();
        LocalDateTime entry = order.getEntryDate().toLocalDateTime();

        assertThatThrownBy(() -> order.changeDates(null, entry.minusDays(1)))
                .isInstanceOf(BadRequestException.class);
        order.changeDates(null, entry);
    }

    @Test
    void changeWarrantyDays_shouldBeBlockedAfterDelivery() {
        ServiceOrder order = openOrder();
        order.changeWarrantyDays(30);
        assertThat(order.getWarrantyDays()).isEqualTo(30);
        order.changeWarrantyDays(null);
        assertThat(order.getWarrantyDays()).isNull();
        assertThatThrownBy(() -> order.changeWarrantyDays(-1))
                .isInstanceOf(BadRequestException.class);

        ServiceOrder delivered = closedOrder(DeviceStatusEnum.ENTREGUE);
        assertConflict(() -> delivered.changeWarrantyDays(10));
    }

    @Test
    void undoClose_shouldRestorePreviousStatusAndClearDeparture_withinWindow() {
        ServiceOrder order = closedOrder(DeviceStatusEnum.DESCARTADO);

        order.undoClose(NOW.plus(WINDOW), WINDOW);

        assertThat(order.getDeviceStatus()).isEqualTo(DeviceStatusEnum.PRONTO);
        assertThat(order.getDepartureDate()).isNull();
        assertThat(order.getClosedAt()).isNull();
        assertThat(order.getWarrantyDays()).isEqualTo(ServiceOrder.DEFAULT_WARRANTY_DAYS);
        assertThat(order.getDeviceHistory())
                .extracting(
                        ServiceOrderHistory::getFieldName,
                        ServiceOrderHistory::getOldValue,
                        ServiceOrderHistory::getNewValue)
                .contains(
                        tuple(DeviceHistoryFieldEnum.STATUS.getField(), "DESCARTADO", "PRONTO"),
                        tuple(
                                DeviceHistoryFieldEnum.DEPARTURE_DATE.getField(),
                                Timestamp.from(NOW).toLocalDateTime().toString(),
                                ""));
    }

    @Test
    void undoClose_shouldRejectAfterWindow() {
        ServiceOrder order = closedOrder(DeviceStatusEnum.ENTREGUE);

        assertThatThrownBy(() -> order.undoClose(NOW.plus(WINDOW).plusSeconds(1), WINDOW))
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNDO_CLOSE_EXPIRED);
        assertThat(order.getDeviceStatus()).isEqualTo(DeviceStatusEnum.ENTREGUE);
    }

    @Test
    void undoClose_shouldRejectOpenOrder() {
        ServiceOrder order = openOrder();

        assertThatThrownBy(() -> order.undoClose(NOW, WINDOW))
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_ORDER_NOT_CLOSED);
    }

    @Test
    void refreshBudgetFeeCollected_shouldRequirePositiveFeeFullyPaid() {
        ServiceOrder order = openOrder();

        order.refreshBudgetFeeCollected(new BigDecimal("49.99"));
        assertThat(order.isBudgetFeeCollected()).isFalse();
        order.refreshBudgetFeeCollected(new BigDecimal("50.00"));
        assertThat(order.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void moveToCustomerDevice_shouldCopyLegacyColumnsAndRecordHistory() {
        ServiceOrder order = openOrder();
        order.getCustomerDevice().setId(10);
        Customer customer = order.getCustomer();
        CustomerDevice target = new CustomerDevice(customer, new BrandsModelsTypes(), List.of(7));
        target.setId(20);

        order.moveToCustomerDevice(target);

        assertThat(order.getCustomerDevice()).isSameAs(target);
        assertThat(order.getColorIds()).containsExactly(7);
        assertThat(order.getDeviceHistory())
                .extracting(
                        ServiceOrderHistory::getFieldName,
                        ServiceOrderHistory::getOldValue,
                        ServiceOrderHistory::getNewValue)
                .contains(tuple(DeviceHistoryFieldEnum.CUSTOMER_DEVICE.getField(), "10", "20"));
    }

    private static void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ServiceOrderConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SERVICE_ORDER_CLOSED);
    }
}
