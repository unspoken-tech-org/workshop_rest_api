package com.tproject.workshop.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tproject.workshop.dto.cellphone.CellPhoneOutputDeviceDto;
import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.dto.device.MinifiedDeviceTableOutputDto;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.CustomerPhone;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.CustomerPhoneRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PhoneRepository;
import com.tproject.workshop.utils.UtilsString;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PhoneRepository phoneRepository;
    private final CustomerPhoneRepository customerPhoneRepository;
    private final DeviceRepository deviceRepository;


    public CustomerOutputDto findById(int id) {
        return customerRepository.findCustomerById(id).orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));
    }

    /**
     * JPA-based implementation for benchmark comparison with JDBC.
     * Returns the same CustomerOutputDto but uses Hibernate instead of native SQL.
     */
    public CustomerOutputDto findByIdUsingJpa(int id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));

        List<CellPhoneOutputDeviceDto> phones = customer.getCustomerPhones().stream()
                .map(cp -> CellPhoneOutputDeviceDto.builder()
                        .id(cp.getPhone().getIdCellphone())
                        .number(cp.getPhone().getNumber())
                        .name(cp.getPhone().getPhoneAlias())
                        .main(cp.isMain())
                        .build())
                .toList();

        List<Device> devices = deviceRepository.findByCustomerIdCustomerOrderByEntryDateDesc(id);
        List<MinifiedDeviceTableOutputDto> customerDevices = devices.stream()
                .map(this::mapDeviceToMinifiedDto)
                .toList();

        return CustomerOutputDto.builder()
                .customerId(customer.getIdCustomer())
                .name(customer.getName())
                .cpf(customer.getCpf())
                .gender(customer.getGender())
                .email(customer.getEmail())
                .insertDate(customer.getInsertDate() != null ? customer.getInsertDate().toString() : null)
                .phones(phones)
                .customerDevices(customerDevices)
                .build();
    }

    private MinifiedDeviceTableOutputDto mapDeviceToMinifiedDto(Device device) {
        MinifiedDeviceTableOutputDto dto = new MinifiedDeviceTableOutputDto();
        dto.setDeviceId(device.getId());
        dto.setCustomerId(device.getCustomer().getIdCustomer());
        dto.setDeviceStatus(device.getDeviceStatus() != null ? device.getDeviceStatus().name() : null);
        dto.setProblem(device.getProblem());
        dto.setHasUrgency(device.isUrgency());
        dto.setRevision(device.isRevision());
        dto.setEntryDate(device.getEntryDate() != null ? device.getEntryDate().toLocalDateTime() : null);
        dto.setDepartureDate(device.getDepartureDate() != null ? device.getDepartureDate().toLocalDateTime() : null);

        // Build typeBrandModel string: "Type Brand | Model"
        BrandsModelsTypes bmt = device.getBrandsModelsTypes();
        if (bmt != null) {
            String type = bmt.getIdType() != null ? bmt.getIdType().getType() : "";
            String brand = bmt.getIdBrand() != null ? bmt.getIdBrand().getBrand() : "";
            String model = bmt.getIdModel() != null ? bmt.getIdModel().getModel() : "";
            dto.setTypeBrandModel(String.format("%s %s | %s", type, brand, model).trim());
        }

        return dto;
    }

    public Page<CustomerListOutputDto> searchCustomers(CustomerFilterDto filters) {
        return customerRepository.findCustomersByFilter(filters);
    }

    @Transactional
    public CustomerOutputDto saveCustomer(InputCustomerDtoRecord inputCustomerDto) {
        String cpfOnlyDigits = UtilsString.onlyDigits(inputCustomerDto.cpf());
        customerRepository.findFirstByCpf(cpfOnlyDigits).ifPresent(customer -> {
            String formattedCpf = UtilsString.formatCpf(cpfOnlyDigits);
            throw new EntityAlreadyExistsException(String.format("O CPF %s já está em uso", formattedCpf));
        });

        List<InputPhoneDto> phones = Optional.ofNullable(inputCustomerDto.phones()).orElse(new ArrayList<>());
        validatePhones(phones, null);

        Customer customer = Customer.builder()
                .cpf(cpfOnlyDigits)
                .name(inputCustomerDto.name())
                .email(inputCustomerDto.email())
                .gender(inputCustomerDto.gender())
                .customerPhones(new ArrayList<>())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        
        if (!phones.isEmpty()) {
            createCustomerPhones(savedCustomer, phones);
        }

        return findById(savedCustomer.getIdCustomer());
    }

    @Transactional
    public CustomerOutputDto updateCustomer(int id, InputCustomerDtoRecord inputCustomerDto) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));

        String cpfOnlyDigits = UtilsString.onlyDigits(inputCustomerDto.cpf());

        customerRepository.findFirstByCpf(cpfOnlyDigits).ifPresent(customer -> {
            if (customer.getIdCustomer() != id) {
                String formattedCpf = UtilsString.formatCpf(cpfOnlyDigits);
                throw new EntityAlreadyExistsException(String.format("O CPF %s já está em uso", formattedCpf));
            }
        });

        List<InputPhoneDto> phones = Optional.ofNullable(inputCustomerDto.phones()).orElse(new ArrayList<>());
        validatePhones(phones, id);

        existingCustomer.setCpf(cpfOnlyDigits);
        existingCustomer.setName(inputCustomerDto.name());
        existingCustomer.setEmail(inputCustomerDto.email());
        existingCustomer.setGender(inputCustomerDto.gender());

        this.syncCustomerPhones(existingCustomer, inputCustomerDto.phones());

        customerRepository.save(existingCustomer);

        customerRepository.flush();

        return findById(id);
    }

    private void createCustomerPhones(Customer customer, List<InputPhoneDto> phoneDtos) {
        for (InputPhoneDto phoneDto : phoneDtos) {
            Phone phone = phoneRepository.findByNumber(phoneDto.number())
                    .orElseGet(() -> {
                        Phone newPhone = Phone.builder()
                                .number(phoneDto.number())
                                .phoneAlias(phoneDto.name())
                                .build();
                        return phoneRepository.save(newPhone);
                    });

            if (phoneDto.name() != null && !phoneDto.name().isEmpty()) {
                phone.setPhoneAlias(phoneDto.name());
                phoneRepository.save(phone);
            }

            Optional<CustomerPhone> existingAssociation = customerPhoneRepository
                    .findByCustomerIdAndPhoneNumber(customer.getIdCustomer(), phone.getNumber());
            
            if (existingAssociation.isPresent()) {
                throw new EntityAlreadyExistsException(
                    String.format("O numero %s já está associado a este cliente", 
                    UtilsString.formatPhoneNumberBR(phone.getNumber())));
            }

            if (phoneDto.isPrimary()) {
                customerPhoneRepository.clearMainPhoneForCustomer(customer.getIdCustomer());
            }

            CustomerPhone customerPhone = new CustomerPhone(customer, phone, phoneDto.isPrimary());
            
            customerPhoneRepository.save(customerPhone);
        }
    }

    private void syncCustomerPhones(Customer customer, List<InputPhoneDto> phoneDtos) {
        if (phoneDtos == null || phoneDtos.isEmpty()) {
            customerPhoneRepository.deleteByCustomerIdCustomer(customer.getIdCustomer());
            return;
        }

        customerPhoneRepository.deleteByCustomerIdCustomer(customer.getIdCustomer());
        
        createCustomerPhones(customer, phoneDtos);
    }

    /**
     * Validate phone numbers for insert/update customer
     *
     * @param phones     numbers to be validated
     * @param customerId this param must be null on customer insertion
     * @throws BadRequestException          on:
     *                                      <p> - Empty phone list
     *                                      <p> - Multiple primary phones
     *                                      <p> - Equal phone numbers
     * @throws EntityAlreadyExistsException on updating customer:
     *                                      <p> - If new inserted phone already exists
     */
    private void validatePhones(List<InputPhoneDto> phones, Integer customerId) {

        if (phones.isEmpty()) {
            throw new BadRequestException("É necessário cadastrar ao menos um telefone");
        }

        boolean hasMultiplePrimaryPhones = phones.stream().filter(InputPhoneDto::isPrimary).count() > 1;
        if (hasMultiplePrimaryPhones) {
            throw new BadRequestException("Não é possível ter mais de um telefone primário");
        }

        Set<String> phoneNumbers = phones.stream().map(InputPhoneDto::number).collect(Collectors.toSet());
        if (phoneNumbers.size() != phones.size()) {
            throw new BadRequestException("Não é possível ter telefones com números iguais");
        }

        for (var phone : phones) {
            phoneRepository.findByNumber(phone.number()).ifPresent((p) -> {
                List<CustomerPhone> associations = customerPhoneRepository.findByPhone_IdCellphone(p.getIdCellphone());
                
                for (CustomerPhone association : associations) {
                    Customer associatedCustomer = association.getCustomer();
                    
                    // On customer update the phone may already exist for this customer
                    if (customerId != null && customerId.equals(associatedCustomer.getIdCustomer())) continue;

                    // Let pass secondary existent phone
                    if (!phone.isPrimary()) continue;

                    // If trying to use as primary a phone that's already primary for another customer
                    if (association.isMain()) {
                        String formattedNumber = UtilsString.formatPhoneNumberBR(p.getNumber());
                        String customerName = UtilsString.capitalizeEachWord(associatedCustomer.getName());
                        throw new EntityAlreadyExistsException(String.format("O numero %s já está cadastrado como principal para o cliente %s", formattedNumber, customerName));
                    }
                }
            });
        }
    }

}
