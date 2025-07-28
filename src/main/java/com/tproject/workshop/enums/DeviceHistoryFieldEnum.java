package com.tproject.workshop.enums;

import lombok.Getter;

@Getter
public enum DeviceHistoryFieldEnum {
    URGENCY("urgency"),
    REVISION("revision"),
    STATUS("status");

    private final String field;

    DeviceHistoryFieldEnum(String field) {
        this.field = field;
    }
}
