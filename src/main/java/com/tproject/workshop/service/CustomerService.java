package com.tproject.workshop.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.CustomerPhone;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.CustomerPhoneRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.PhoneRepository;
import com.tproject.workshop.utils.UtilsString;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PhoneRepository phoneRepository;
    private final CustomerPhoneRepository customerPhoneRepository;


    public CustomerOutputDto findById(int id) {
        return customerRepository.findCustomerById(id).orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));
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
