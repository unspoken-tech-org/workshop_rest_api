package com.tproject.workshop.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.integration.support.TestClock;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.repository.CustomerContactRepository;
import com.tproject.workshop.support.fixtures.CustomerContactTestBuilder;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerContactServiceTest {

    private static final int DEVICE_ID = 1;

    @Mock private CustomerContactRepository customerContactRepository;
    @Mock private DeviceService deviceService;
    @Mock private TechnicianService technicianService;
    @Mock private PhoneService phoneService;
    @Mock private ObservabilityEventRecorder eventRecorder;
    @Spy private Clock clock = TestClock.FIXED;

    @InjectMocks private CustomerContactService customerContactService;

    private void stubTechnician() {
        when(technicianService.findByIdDto(anyInt()))
                .thenReturn(new TechnicianResponseDto(2, "Tecnico", "T002"));
    }

    private void stubSaveAndTechnician() {
        stubTechnician();
        stubSave();
    }

    private void stubSave() {
        when(customerContactRepository.save(any(CustomerContact.class)))
                .thenAnswer(invocation -> {
                    CustomerContact contact = invocation.getArgument(0);
                    if (contact.getId() == null) {
                        contact.setId(10);
                    }
                    return contact;
                });
    }

    @Test
    void save_shouldUpdateDeviceStatus_whenNewContactIsTheLatest() {
        stubSaveAndTechnician();
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenAnswer(invocation -> Optional.of(contactWithId(10, DeviceStatusEnum.PRONTO)));

        customerContactService.save(input("pronto", "2026-06-03T10:00:00"));

        verify(deviceService).ensureDeviceExists(DEVICE_ID);
        verify(deviceService).updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.PRONTO);
    }

    @Test
    void save_shouldNotUpdateDeviceStatus_whenBackdatedContactIsNotTheLatest() {
        stubSaveAndTechnician();
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(contactWithId(5, DeviceStatusEnum.ENTREGUE)));

        customerContactService.save(input("em_andamento", "2024-01-01T10:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
    }

    @Test
    void save_shouldThrowNotFound_whenDeviceDoesNotExist() {
        doThrow(new NotFoundException("Aparelho com id 1 não encontrado"))
                .when(deviceService)
                .ensureDeviceExists(DEVICE_ID);

        assertThatThrownBy(
                        () -> customerContactService.save(input("pronto", "2026-06-03T10:00:00")))
                .isInstanceOf(NotFoundException.class);
        verify(customerContactRepository, never()).save(any());
    }

    @Test
    void update_shouldNotRevertDeviceStatus_whenEditingAnOldContact() {
        stubSaveAndTechnician();
        CustomerContact oldContact = contactWithId(3, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(3)).thenReturn(Optional.of(oldContact));
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(contactWithId(7, DeviceStatusEnum.ENTREGUE)));

        customerContactService.update(3, input("aguardando", "2024-01-10T09:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
    }

    @Test
    void update_shouldNotTouchDeviceStatus_whenContactStatusDidNotChange() {
        stubSaveAndTechnician();
        CustomerContact latestContact = contactWithId(7, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(7)).thenReturn(Optional.of(latestContact));

        customerContactService.update(7, input("em_andamento", "2026-06-03T10:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
        verify(customerContactRepository, never())
                .findFirstByDeviceIdOrderByLastContactDescIdDesc(anyInt());
    }

    @Test
    void update_shouldUpdateDeviceStatus_whenLatestContactStatusChanges() {
        stubSaveAndTechnician();
        CustomerContact latestContact = contactWithId(7, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(7)).thenReturn(Optional.of(latestContact));
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(latestContact));

        customerContactService.update(7, input("pronto", "2026-06-03T10:00:00"));

        verify(deviceService).updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.PRONTO);
    }

    @Test
    void save_shouldHandleNullContactTypeWithoutNpeAndParseUtcZDate() {
        stubSaveAndTechnician();
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenAnswer(invocation ->
                        Optional.of(contactWithId(10, DeviceStatusEnum.EM_ANDAMENTO)));

        CustomerContactInputDto dto =
                new CustomerContactInputDto(
                        DEVICE_ID,
                        null,
                        2,
                        null,
                        "teste",
                        true,
                        "em_andamento",
                        "2025-08-31T19:55:13Z");

        var saved = customerContactService.save(dto);

        Timestamp expectedTimestamp =
                Timestamp.valueOf(
                        OffsetDateTime.parse("2025-08-31T19:55:13Z")
                                .atZoneSameInstant(TestClock.DEFAULT_ZONE)
                                .toLocalDateTime());
        Assertions.assertThat(saved.lastContact()).isEqualTo(expectedTimestamp.toLocalDateTime());
        verify(phoneService, never()).findByNumber(anyString());
    }

    @Test
    void save_shouldNormalizeContactChannelAndRequirePhoneForLigacao() {
        CustomerContactInputDto dto =
                new CustomerContactInputDto(
                        DEVICE_ID,
                        "Ligação",
                        2,
                        null,
                        "teste",
                        true,
                        "em_andamento",
                        "2025-08-31T19:55:13");

        assertThatThrownBy(() -> customerContactService.save(dto))
                .isInstanceOf(BadRequestException.class);
        verify(customerContactRepository, never()).save(any());
    }

    @Test
    void save_shouldRejectUnknownContactChannel() {
        CustomerContactInputDto dto =
                new CustomerContactInputDto(
                        DEVICE_ID,
                        "pombo_correio",
                        2,
                        null,
                        "teste",
                        true,
                        "em_andamento",
                        "2025-08-31T19:55:13");

        assertThatThrownBy(() -> customerContactService.save(dto))
                .isInstanceOf(BadRequestException.class);
        verify(customerContactRepository, never()).save(any());
    }

    private static CustomerContactInputDto input(String deviceStatus, String contactDate) {
        return new CustomerContactInputDto(
                DEVICE_ID, "pessoalmente", 2, null, "teste", true, deviceStatus, contactDate);
    }

    private static CustomerContact contactWithId(int id, DeviceStatusEnum status) {
        return CustomerContactTestBuilder.aCustomerContact()
                .withId(id)
                .withDeviceId(DEVICE_ID)
                .withStatus(status)
                .build();
    }
}
