package com.tproject.workshop.service;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.repository.CustomerContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerContactServiceTest {

    private static final int DEVICE_ID = 1;

    @Mock
    private CustomerContactRepository customerContactRepository;
    @Mock
    private DeviceService deviceService;
    @Mock
    private TechnicianService technicianService;
    @Mock
    private PhoneService phoneService;

    @InjectMocks
    private CustomerContactService customerContactService;

    @BeforeEach
    void setUp() {
        when(technicianService.findByIdDto(anyInt())).thenReturn(new TechnicianResponseDto(2, "Tecnico", "T002"));
        when(customerContactRepository.save(any(CustomerContact.class))).thenAnswer(invocation -> {
            CustomerContact contact = invocation.getArgument(0);
            if (contact.getId() == null) {
                contact.setId(10);
            }
            return contact;
        });
    }

    @Test
    void save_shouldUpdateDeviceStatus_whenNewContactIsTheLatest() {
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenAnswer(invocation -> Optional.of(contactWithId(10, DeviceStatusEnum.PRONTO)));

        customerContactService.save(input("pronto", "2026-06-03T10:00:00"));

        verify(deviceService).ensureDeviceExists(DEVICE_ID);
        verify(deviceService).updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.PRONTO);
    }

    @Test
    void save_shouldNotUpdateDeviceStatus_whenBackdatedContactIsNotTheLatest() {
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(contactWithId(5, DeviceStatusEnum.ENTREGUE)));

        customerContactService.save(input("em_andamento", "2024-01-01T10:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
    }

    @Test
    void save_shouldThrowNotFound_whenDeviceDoesNotExist() {
        doThrow(new NotFoundException("Aparelho com id 1 não encontrado"))
                .when(deviceService).ensureDeviceExists(DEVICE_ID);

        assertThatThrownBy(() -> customerContactService.save(input("pronto", "2026-06-03T10:00:00")))
                .isInstanceOf(NotFoundException.class);
        verify(customerContactRepository, never()).save(any());
    }

    @Test
    void update_shouldNotRevertDeviceStatus_whenEditingAnOldContact() {
        CustomerContact oldContact = contactWithId(3, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(3)).thenReturn(Optional.of(oldContact));
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(contactWithId(7, DeviceStatusEnum.ENTREGUE)));

        customerContactService.update(3, input("aguardando", "2024-01-10T09:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
    }

    @Test
    void update_shouldNotTouchDeviceStatus_whenContactStatusDidNotChange() {
        CustomerContact latestContact = contactWithId(7, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(7)).thenReturn(Optional.of(latestContact));
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(latestContact));

        customerContactService.update(7, input("em_andamento", "2026-06-03T10:00:00"));

        verify(deviceService, never()).updateDeviceStatus(anyInt(), any(DeviceStatusEnum.class));
    }

    @Test
    void update_shouldUpdateDeviceStatus_whenLatestContactStatusChanges() {
        CustomerContact latestContact = contactWithId(7, DeviceStatusEnum.EM_ANDAMENTO);
        when(customerContactRepository.findById(7)).thenReturn(Optional.of(latestContact));
        when(customerContactRepository.findFirstByDeviceIdOrderByLastContactDescIdDesc(DEVICE_ID))
                .thenReturn(Optional.of(latestContact));

        customerContactService.update(7, input("pronto", "2026-06-03T10:00:00"));

        verify(deviceService).updateDeviceStatus(DEVICE_ID, DeviceStatusEnum.PRONTO);
    }

    private static CustomerContactInputDto input(String deviceStatus, String contactDate) {
        return new CustomerContactInputDto(DEVICE_ID, "pessoalmente", 2, null, "teste", true,
                deviceStatus, contactDate);
    }

    private static CustomerContact contactWithId(int id, DeviceStatusEnum status) {
        CustomerContact contact = new CustomerContact();
        contact.setId(id);
        contact.setDeviceId(DEVICE_ID);
        contact.setType("pessoalmente");
        contact.setDeviceStatus(status);
        contact.setLastContact(Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)));
        return contact;
    }
}
