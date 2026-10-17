package com.tproject.workshop.service;

import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceEventListener {

    private final DeviceRepository deviceRepository;

    @EventListener
    @Transactional
    @Async
    public void onDeviceViewed(DeviceViewedEvent event) {
        log.info("Device {} viewed, updating lastViewedAt", event.getDeviceId());
        // Async listener: an exception here has no caller to answer and would be logged as ERROR without requestId.
        Optional<Device> device = deviceRepository.findById(event.getDeviceId());
        if (device.isEmpty()) {
            log.warn("Device {} not found, lastViewedAt not updated", event.getDeviceId());
            return;
        }

        device.get().setLastViewedAt(Timestamp.from(Instant.now()));
        deviceRepository.save(device.get());
    }
}
