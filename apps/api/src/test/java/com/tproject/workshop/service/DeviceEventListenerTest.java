package com.tproject.workshop.service;

import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceEventListenerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceEventListener deviceEventListener;

    @Test
    void shouldUpdateLastViewedAtWhenDeviceExists() {
        Device device = new Device();
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));

        deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, 1));

        assertThat(device.getLastViewedAt()).isNotNull();
        verify(deviceRepository).save(device);
    }

    @Test
    void shouldNotThrowWhenDeviceDoesNotExist() {
        when(deviceRepository.findById(999)).thenReturn(Optional.empty());

        assertThatCode(() -> deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, 999)))
                .doesNotThrowAnyException();

        verify(deviceRepository, never()).save(any());
    }
}
