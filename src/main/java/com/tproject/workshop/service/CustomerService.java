package com.tproject.workshop.service;

import com.tproject.workshop.dto.cellphone.CellPhoneOutputDeviceDto;
import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.PhoneRepository;
import com.tproject.workshop.utils.UtilsString;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PhoneRepository phoneRepository;


    public CustomerOutputDto findById(int id) {
        return customerRepository.findCustomerById(id).orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));
    }

    public List<CustomerListOutputDto> searchCustomers(CustomerFilterDto filters) {
        return customerRepository.findCustomersByFilter(filters);
    }

    @Transactional
    public CustomerOutputDto saveCustomer(InputCustomerDtoRecord inputCustomerDto) {
        String cpfOnlyDigits = inputCustomerDto.cpf().replaceAll("\\D", "");
        customerRepository.findFirstByCpf(cpfOnlyDigits).ifPresent(customer -> {
            throw new EntityAlreadyExistsException(String.format("O cpf %s já está em uso", cpfOnlyDigits));
        });

        List<InputPhoneDto> phones = Optional.ofNullable(inputCustomerDto.phones()).orElse(new ArrayList<>());
        validatePhones(phones, null);

        Customer customer = Customer.builder()
                .cpf(cpfOnlyDigits)
                .name(inputCustomerDto.name())
                .email(inputCustomerDto.email())
                .gender(inputCustomerDto.gender())
                .phones(new ArrayList<>())
                .build();

        if (!phones.isEmpty()) {
            List<Phone> newPhones = phones.stream()
                    .map(phoneDto -> {
                        Phone newPhone = new Phone();
                        newPhone.setCustomer(customer);
                        newPhone.setName(phoneDto.name());
                        newPhone.setNumber(phoneDto.number());
                        newPhone.setMain(phoneDto.isPrimary());
                        return newPhone;
                    })
                    .toList();
            customer.getPhones().addAll(newPhones);
        }

        Customer savedCustomer = customerRepository.save(customer);

        return toDto(savedCustomer);
    }

    @Transactional
    public CustomerOutputDto updateCustomer(int id, InputCustomerDtoRecord inputCustomerDto) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));

        String cpfOnlyDigits = inputCustomerDto.cpf().replaceAll("\\D", "");

        customerRepository.findFirstByCpf(cpfOnlyDigits).ifPresent(customer -> {
            if (customer.getIdCustomer() != id) {
                throw new EntityAlreadyExistsException(String.format("O cpf %s já está em uso", cpfOnlyDigits));
            }
        });

        List<InputPhoneDto> phones = Optional.ofNullable(inputCustomerDto.phones()).orElse(new ArrayList<>());
        validatePhones(phones, id);

        existingCustomer.setCpf(cpfOnlyDigits);
        existingCustomer.setName(inputCustomerDto.name());
        existingCustomer.setEmail(inputCustomerDto.email());
        existingCustomer.setGender(inputCustomerDto.gender());

        this.syncPhones(existingCustomer, inputCustomerDto.phones());

        customerRepository.save(existingCustomer);

        customerRepository.flush();

        return findById(id);
    }

    private void syncPhones(Customer customer, List<InputPhoneDto> phoneDtos) {
        if (phoneDtos == null) {
            customer.getPhones().clear();
            return;
        }

        Map<Integer, Phone> currentPhonesMap = customer.getPhones().stream()
                .collect(Collectors.toMap(Phone::getIdCellphone, Function.identity()));

        List<Phone> phonesToKeep = new ArrayList<>();

        for (InputPhoneDto phoneDto : phoneDtos) {
            Phone phone;
            if (phoneDto.id() != null) {
                phone = currentPhonesMap.get(phoneDto.id());
                if (phone == null) {
                    throw new NotFoundException(String.format("Telefone com id %d não encontrado ou não pertence a este cliente.", phoneDto.id()));
                }
            } else {
                phone = new Phone();
                phone.setCustomer(customer);
            }

            phone.setName(phoneDto.name());
            phone.setNumber(phoneDto.number());
            phone.setMain(phoneDto.isPrimary());
            phonesToKeep.add(phone);
        }

        customer.getPhones().clear();
        customer.getPhones().addAll(phonesToKeep);
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
                        Customer customer = p.getCustomer();
                        if (customerId != null && customerId.equals(customer.getIdCustomer())) return;

                        String number = p.getNumber();
                        String customerName = UtilsString.capitalizeEachWord(customer.getName());
                        throw new EntityAlreadyExistsException(String.format("O numero %s já está cadastrado para o cliente %s", number, customerName));
                    }
            );
        }
    }

    private CustomerOutputDto toDto(Customer customer) {
        var phonesDtoList = customer.getPhones().stream().map(phone -> CellPhoneOutputDeviceDto.builder()
                .id(phone.getIdCellphone())
                .number(phone.getNumber())
                .main(phone.isMain())
                .name(phone.getName())
                .build()
        ).toList();

        return CustomerOutputDto.builder()
                .customerId(customer.getIdCustomer())
                .name(customer.getName())
                .insertDate(customer.getInsertDate().toLocalDateTime().toString())
                .email(customer.getEmail())
                .gender(customer.getGender())
                .cpf(customer.getCpf())
                .phones(phonesDtoList)
                .build();
    }
}
