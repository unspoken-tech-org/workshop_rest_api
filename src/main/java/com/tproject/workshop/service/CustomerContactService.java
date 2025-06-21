package com.tproject.workshop.service;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.model.DeviceStatus;
import com.tproject.workshop.repository.CustomerContactRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository customerContactRepository;
    private final DeviceStatusService deviceStatusService;
    private final DeviceService deviceService;
    private final TechnicianService technicianService;
    private final PhoneService phoneService;

    private final List<String> contactTypesThatNeedPhone = List.of("ligacao", "mensagem");


    public CustomerContact save(CustomerContactInputDto contact) {
        deviceService.findDeviceById(contact.deviceId());
        technicianService.findById(contact.technicianId());
        DeviceStatus status = deviceStatusService.findByStatus(contact.deviceStatus());
        
        if (contactTypesThatNeedPhone.contains(UtilsString.normalizeString(contact.contactType()))) {
            if (contact.phoneNumber() == null || contact.phoneNumber().isEmpty()) {
                String joinedNeededContactTypes = String.join(", ", contactTypesThatNeedPhone);
                throw new BadRequestException(
                        String.format("O numero de telefone deve ser fornecido para os tipos de contato: %s", joinedNeededContactTypes)
                );
            }
            phoneService.findByNumber(contact.phoneNumber());
        }

        CustomerContact newContact = new CustomerContact();
        newContact.setDeviceId(contact.deviceId());
        newContact.setTechnicianId(contact.technicianId());
        newContact.setHasMadeContact(contact.contactStatus());
        newContact.setConversation(contact.message());
        newContact.setType(contact.contactType());
        newContact.setPhone(contact.phoneNumber());
        newContact.setLastContact(Timestamp.valueOf(LocalDateTime.parse(contact.contactDate())));
        newContact.setDeviceStatus(status);

        return customerContactRepository.save(newContact);
    }

}
