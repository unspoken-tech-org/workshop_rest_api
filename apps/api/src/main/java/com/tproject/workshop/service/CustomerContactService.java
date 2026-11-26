package com.tproject.workshop.service;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.contact.CustomerContactOutputDto;
import com.tproject.workshop.dto.technician.TechnicianResponseDto;
import com.tproject.workshop.enums.ContactChannel;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventContract;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.CustomerContact;
import com.tproject.workshop.repository.CustomerContactRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerContactService {

    private final CustomerContactRepository customerContactRepository;
    private final DeviceService deviceService;
    private final TechnicianService technicianService;
    private final PhoneService phoneService;
    private final ObservabilityEventRecorder eventRecorder;
    private final Clock clock;

    @Transactional
    public CustomerContactOutputDto save(CustomerContactInputDto contact) {
        TechnicianResponseDto technician = technicianService.findByIdDto(contact.technicianId());
        DeviceStatusEnum status = contact.deviceStatus();
        Optional<ContactChannel> channel = ContactChannel.fromNullable(contact.contactType());
        validatePhoneIfNeeded(channel.orElse(null), contact.phoneNumber());
        deviceService.ensureDeviceExists(contact.deviceId());

        CustomerContact newContact = new CustomerContact();
        mapContactFields(newContact, contact, channel.orElse(null), status);
        CustomerContact saved = customerContactRepository.save(newContact);
        updateDeviceStatusIfLatestContact(saved);
        recordContact(
                ObservabilityEventCatalog.CUSTOMER_CONTACT_REGISTERED,
                channel.orElse(null),
                saved,
                status);

        return toDto(saved, technician.name());
    }

    @Transactional
    public CustomerContactOutputDto update(int id, CustomerContactInputDto contact) {
        CustomerContact existingContact =
                customerContactRepository
                        .findById(id)
                        .orElseThrow(() -> NotFoundException.of("Contato", id));

        DeviceStatusEnum previousStatus = existingContact.getDeviceStatus();
        TechnicianResponseDto technician = technicianService.findByIdDto(contact.technicianId());
        DeviceStatusEnum status = contact.deviceStatus();
        Optional<ContactChannel> channel = ContactChannel.fromNullable(contact.contactType());
        validatePhoneIfNeeded(channel.orElse(null), contact.phoneNumber());
        deviceService.ensureDeviceExists(contact.deviceId());

        mapContactFields(existingContact, contact, channel.orElse(null), status);
        CustomerContact saved = customerContactRepository.save(existingContact);
        if (previousStatus != status) {
            updateDeviceStatusIfLatestContact(saved);
        }
        recordContact(
                ObservabilityEventCatalog.CUSTOMER_CONTACT_UPDATED,
                channel.orElse(null),
                saved,
                status);

        return toDto(saved, technician.name());
    }

    /**
     * O status informado no contato representa o aparelho no momento daquele contato. Só o contato
     * mais recente (por data, com desempate pelo id) pode refletir no status atual do aparelho;
     * corrigir ou registrar um contato antigo não reverte, por exemplo, um aparelho ENTREGUE.
     */
    private void updateDeviceStatusIfLatestContact(CustomerContact contact) {
        boolean isLatestContact =
                customerContactRepository
                        .findFirstByDeviceIdOrderByLastContactDescIdDesc(contact.getDeviceId())
                        .map(latest -> latest.getId().equals(contact.getId()))
                        .orElse(false);

        if (isLatestContact) {
            deviceService.updateDeviceStatus(contact.getDeviceId(), contact.getDeviceStatus());
        }
    }

    private void recordContact(
            ObservabilityEventContract contract,
            ContactChannel channel,
            CustomerContact contact,
            DeviceStatusEnum status) {
        String contactType = channel != null ? channel.observabilityValue() : "other";
        eventRecorder.record(
                contract,
                Map.of(
                        "contact.type", contactType,
                        "contact.made", String.valueOf(contact.isHasMadeContact()),
                        "status.new", status.name().toLowerCase(Locale.ROOT)));
    }

    private void validatePhoneIfNeeded(ContactChannel channel, String phoneNumber) {
        if (channel != null && channel.requiresPhone()) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                throw new BadRequestException(
                        "O número de telefone deve ser fornecido para os tipos de contato: ligacao, mensagem");
            }
            phoneService.findByNumber(phoneNumber);
        }
    }

    private void mapContactFields(
            CustomerContact contact,
            CustomerContactInputDto dto,
            ContactChannel channel,
            DeviceStatusEnum status) {
        contact.setDeviceId(dto.deviceId());
        contact.setTechnicianId(dto.technicianId());
        contact.setHasMadeContact(dto.contactStatus());
        contact.setConversation(dto.message());
        contact.setType(channel != null ? channel.wireValue() : null);
        contact.setPhone(dto.phoneNumber());
        contact.setLastContact(parseContactDate(dto.contactDate()));
        contact.setDeviceStatus(status);
    }

    private Timestamp parseContactDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new BadRequestException(
                    "Formato de data inválido: '%s'. Use o padrão ISO 8601 (ex: 2025-08-31T19:55:13)",
                    rawDate);
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(rawDate));
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime localDateTime =
                        OffsetDateTime.parse(rawDate)
                                .atZoneSameInstant(clock.getZone())
                                .toLocalDateTime();
                return Timestamp.valueOf(localDateTime);
            } catch (DateTimeParseException e) {
                throw new BadRequestException(
                        String.format(
                                "Formato de data inválido: '%s'. Use o padrão ISO 8601 (ex: 2025-08-31T19:55:13)",
                                rawDate));
            }
        }
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
                model.getDeviceStatus());
    }
}
