package com.tproject.workshop.service;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.contact.CustomerContactOutputDto;
import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.repository.CustomerContactRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository customerContactRepository;
    private final DeviceService deviceService;
    private final TechnicianService technicianService;
    private final PhoneService phoneService;

    private final List<String> contactTypesThatNeedPhone = List.of("ligacao", "mensagem");

    @Transactional
    public CustomerContactOutputDto save(CustomerContactInputDto contact) {
        TechnicianResponseDto technician = technicianService.findByIdDto(contact.technicianId());
        DeviceStatusEnum status = DeviceStatusEnum.fromString(contact.deviceStatus());
        validatePhoneIfNeeded(contact.contactType(), contact.phoneNumber());
        deviceService.updateDeviceStatus(contact.deviceId(), status);

        CustomerContact newContact = new CustomerContact();
        mapContactFields(newContact, contact, status);
        CustomerContact saved = customerContactRepository.save(newContact);

        return toDto(saved, technician.name());
    }

    @Transactional
    public CustomerContactOutputDto update(int id, CustomerContactInputDto contact) {
        CustomerContact existingContact = customerContactRepository.findById(id).orElseThrow(() ->
                new NotFoundException(String.format("Contato com id %d não encontrado", id))
        );

        TechnicianResponseDto technician = technicianService.findByIdDto(contact.technicianId());
        DeviceStatusEnum status = DeviceStatusEnum.fromString(contact.deviceStatus());
        validatePhoneIfNeeded(contact.contactType(), contact.phoneNumber());
        deviceService.updateDeviceStatus(contact.deviceId(), status);

        mapContactFields(existingContact, contact, status);
        CustomerContact saved = customerContactRepository.save(existingContact);

        return toDto(saved, technician.name());
    }

    private void validatePhoneIfNeeded(String contactType, String phoneNumber) {
        if (contactTypesThatNeedPhone.contains(UtilsString.normalizeString(contactType))) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                String joinedNeededContactTypes = String.join(", ", contactTypesThatNeedPhone);
                throw new BadRequestException(
                        String.format("O numero de telefone deve ser fornecido para os tipos de contato: %s", joinedNeededContactTypes)
                );
            }
            phoneService.findByNumber(phoneNumber);
        }
    }

    private void mapContactFields(CustomerContact contact, CustomerContactInputDto dto, DeviceStatusEnum status) {
        contact.setDeviceId(dto.deviceId());
        contact.setTechnicianId(dto.technicianId());
        contact.setHasMadeContact(dto.contactStatus());
        contact.setConversation(dto.message());
        contact.setType(dto.contactType());
        contact.setPhone(dto.phoneNumber());
        try {
            contact.setLastContact(Timestamp.valueOf(LocalDateTime.parse(dto.contactDate())));
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    String.format("Formato de data inválido: '%s'. Use o padrão ISO 8601 (ex: 2025-08-31T19:55:13)", dto.contactDate())
            );
        }
        contact.setDeviceStatus(status);
    }

    private CustomerContactOutputDto toDto(CustomerContact model, String technicianName) {
        return new CustomerContactOutputDto(
                model.getId(),
                model.getDeviceId(),
                model.getTechnicianId(),
                technicianName,
                model.getPhone(),
                model.getType(),
                model.isHasMadeContact(),
                model.getLastContact() != null ? model.getLastContact().toLocalDateTime() : null,
                model.getConversation(),
                model.getDeviceStatus().name()
        );
    }

}
