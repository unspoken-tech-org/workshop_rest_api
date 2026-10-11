package com.tproject.workshop.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeviceViewedEvent extends ApplicationEvent {
    private final int deviceId;

    public DeviceViewedEvent(Object source, int deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
} 