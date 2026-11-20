package com.tproject.workshop.service;

import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceEventListenerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceEventListener deviceEventListener;

    @Test
    void shouldUpdateLastViewedAtWhenDeviceExists() {
        when(deviceRepository.updateLastViewedAt(eq(1), any(Timestamp.class))).thenReturn(1);

        deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, 1));

        verify(deviceRepository).updateLastViewedAt(eq(1), any(Timestamp.class));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldNotThrowWhenDeviceDoesNotExist() {
        when(deviceRepository.updateLastViewedAt(eq(999), any(Timestamp.class))).thenReturn(0);

        assertThatCode(() -> deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, 999)))
                .doesNotThrowAnyException();

        verify(deviceRepository).updateLastViewedAt(eq(999), any(Timestamp.class));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldSwallowExceptionAndNotPropagateOutOfAsyncListener() {
        when(deviceRepository.updateLastViewedAt(eq(1), any(Timestamp.class)))
                .thenThrow(new RuntimeException("db connection lost"));

        assertThatCode(() -> deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, 1)))
                .doesNotThrowAnyException();
    }
}
