package com.tproject.workshop.service;

import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.repository.DeviceRepository;
import java.sql.Timestamp;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceEventListener {

    private final DeviceRepository deviceRepository;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void onDeviceViewed(DeviceViewedEvent event) {
        try {
            log.debug("Device {} viewed, updating lastViewedAt", event.getDeviceId());
            int updated =
                    deviceRepository.updateLastViewedAt(
                            event.getDeviceId(), Timestamp.from(clock.instant()));
            if (updated == 0) {
                log.warn("Device {} not found, lastViewedAt not updated", event.getDeviceId());
            }
        } catch (Exception ex) {
            // Async listener: log at warn so unhandled non-request ERROR logs do not trigger deploy
            // rollback.
            log.warn("Failed to update lastViewedAt for device {}", event.getDeviceId(), ex);
        }
    }
}
