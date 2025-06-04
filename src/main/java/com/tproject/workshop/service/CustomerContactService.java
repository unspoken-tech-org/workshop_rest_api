package com.tproject.workshop.service;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.model.DeviceStatus;
import com.tproject.workshop.repository.CustomerContactRepository;
import com.tproject.workshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository customerContactRepository;

    private final DeviceStatusService deviceStatusService;
    private final PaymentRepository paymentRepository;

    public CustomerContact save(CustomerContactInputDto contact) {
        CustomerContact newContact = new CustomerContact();
        DeviceStatus status = deviceStatusService.findByStatus(contact.deviceStatus());

        newContact.setDeviceId(contact.deviceId());
        newContact.setTechnicianId(contact.technicianId());
        newContact.setHasMadeContact(contact.contactStatus());
        newContact.setConversation(contact.message());
        newContact.setType(contact.contactType());
        newContact.setPhoneId(contact.phoneNumberId());
        newContact.setLastContact(Timestamp.valueOf(LocalDateTime.parse(contact.contactDate())));
        newContact.setDeviceStatus(status);

        return customerContactRepository.save(newContact);
    }

}
