package com.tproject.workshop.support.fixtures;

import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.integration.support.TestClock;
import com.tproject.workshop.model.Device;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

/** Test data builder for {@link Device}. */
public final class DeviceTestBuilder {

    private Integer id = 1;
    private DeviceStatusEnum status = DeviceStatusEnum.EM_ANDAMENTO;
    private Timestamp entryDate = Timestamp.from(TestClock.DEFAULT_INSTANT.minusSeconds(172800));
    private BigDecimal budgetFee = new BigDecimal("50.00");
    private BigDecimal serviceValue = new BigDecimal("200.00");
    private boolean budgetFeeCollected = false;

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

    public Device build() {
        Device device = new Device();
        device.setId(id);
        device.setDeviceStatus(status);
        device.setEntryDate(entryDate);
        device.setBudgetFee(budgetFee);
        device.setServiceValue(serviceValue);
        device.setBudgetFeeCollected(budgetFeeCollected);
        device.setDeviceHistory(new ArrayList<>());
        return device;
    }
}
