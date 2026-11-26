package com.tproject.workshop.support.fixtures;

import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.model.CustomerContact;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Test data builder for {@link CustomerContact}. */
public final class CustomerContactTestBuilder {

    private Integer id = 1;
    private Integer deviceId = 1;
    private String type = "pessoalmente";
    private DeviceStatusEnum deviceStatus = DeviceStatusEnum.EM_ANDAMENTO;
    private Timestamp lastContact = Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0));

    private CustomerContactTestBuilder() {}

    public static CustomerContactTestBuilder aCustomerContact() {
        return new CustomerContactTestBuilder();
    }

    public CustomerContactTestBuilder withId(Integer id) {
        this.id = id;
        return this;
    }

    public CustomerContactTestBuilder withDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public CustomerContactTestBuilder withStatus(DeviceStatusEnum deviceStatus) {
        this.deviceStatus = deviceStatus;
        return this;
    }

    public CustomerContact build() {
        CustomerContact contact = new CustomerContact();
        contact.setId(id);
        contact.setDeviceId(deviceId);
        contact.setType(type);
        contact.setDeviceStatus(deviceStatus);
        contact.setLastContact(lastContact);
        return contact;
    }
}
