package com.tproject.workshop.support.fixtures;

import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.integration.support.TestClock;
import com.tproject.workshop.model.ServiceOrder;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.springframework.test.util.ReflectionTestUtils;

/** Test data builder for {@link ServiceOrder}. */
public final class DeviceTestBuilder {

    private Integer id = 1;
    private DeviceStatusEnum status = DeviceStatusEnum.EM_ANDAMENTO;
    private Timestamp entryDate = Timestamp.from(TestClock.DEFAULT_INSTANT.minusSeconds(172800));
    private BigDecimal budgetFee = new BigDecimal("50.00");
    private BigDecimal serviceValue = new BigDecimal("200.00");
    private boolean budgetFeeCollected = false;
    private boolean urgency = false;
    private boolean revision = false;
    private Timestamp departureDate;
    private Timestamp closedAt;

    private DeviceTestBuilder() {}

    public static DeviceTestBuilder aDevice() {
        return new DeviceTestBuilder();
    }

    public DeviceTestBuilder withId(Integer id) {
        this.id = id;
        return this;
    }

    public DeviceTestBuilder withStatus(DeviceStatusEnum status) {
        this.status = status;
        return this;
    }

    public DeviceTestBuilder withEntryDate(Timestamp entryDate) {
        this.entryDate = entryDate;
        return this;
    }

    public DeviceTestBuilder withBudgetFee(BigDecimal budgetFee) {
        this.budgetFee = budgetFee;
        return this;
    }

    public DeviceTestBuilder withServiceValue(BigDecimal serviceValue) {
        this.serviceValue = serviceValue;
        return this;
    }

    public DeviceTestBuilder withBudgetFeeCollected(boolean budgetFeeCollected) {
        this.budgetFeeCollected = budgetFeeCollected;
        return this;
    }

    public DeviceTestBuilder withUrgency(boolean urgency) {
        this.urgency = urgency;
        return this;
    }

    public DeviceTestBuilder withRevision(boolean revision) {
        this.revision = revision;
        return this;
    }

    public DeviceTestBuilder withDepartureDate(Timestamp departureDate) {
        this.departureDate = departureDate;
        return this;
    }

    public DeviceTestBuilder withClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
        return this;
    }

    /** Fields guarded by the aggregate have no setter, so the builder writes them directly. */
    public ServiceOrder build() {
        ServiceOrder device = new ServiceOrder();
        device.setId(id);
        ReflectionTestUtils.setField(device, "deviceStatus", status);
        ReflectionTestUtils.setField(device, "entryDate", entryDate);
        ReflectionTestUtils.setField(device, "departureDate", departureDate);
        ReflectionTestUtils.setField(device, "closedAt", closedAt);
        ReflectionTestUtils.setField(device, "urgency", urgency);
        ReflectionTestUtils.setField(device, "revision", revision);
        device.setBudgetFee(budgetFee);
        device.setServiceValue(serviceValue);
        device.setBudgetFeeCollected(budgetFeeCollected);
        return device;
    }
}
