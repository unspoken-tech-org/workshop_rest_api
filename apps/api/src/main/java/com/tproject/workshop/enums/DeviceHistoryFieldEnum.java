package com.tproject.workshop.enums;

import lombok.Getter;

@Getter
public enum DeviceHistoryFieldEnum {
    URGENCY("urgency"),
    REVISION("revision"),
    STATUS("device_status"),
    DEPARTURE_DATE("departure_date"),
    CUSTOMER_DEVICE("customer_device");

    private final String field;

    DeviceHistoryFieldEnum(String field) {
        this.field = field;
    }
}
