package com.tproject.workshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.CustomerPhone;
import com.tproject.workshop.model.Phone;
import com.tproject.workshop.repository.CustomerPhoneRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PhoneRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String CPF_IN_USE = "O CPF informado já está em uso";

    @Mock private CustomerRepository customerRepository;
    @Mock private PhoneRepository phoneRepository;
    @Mock private CustomerPhoneRepository customerPhoneRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ObservabilityEventRecorder eventRecorder;

    @InjectMocks private CustomerService customerService;

    private static InputPhoneDto phone(String number, boolean primary) {
        return new InputPhoneDto(null, "alias", number, primary);
    }

    private static InputCustomerDtoRecord input(String cpf, InputPhoneDto... phones) {
        return new InputCustomerDtoRecord(
                "Maria", cpf, "feminino", "maria@example.com", List.of(phones));
    }

    @Test
    @DisplayName("findById returns the customer read by the repository")
    void shouldReturnCustomer_whenIdExists() {
        CustomerOutputDto dto = CustomerOutputDto.builder().customerId(3).name("Maria").build();
        when(customerRepository.findCustomerById(3)).thenReturn(Optional.of(dto));

        assertThat(customerService.findById(3)).isSameAs(dto);
    }

    @Test
    @DisplayName("findById throws NotFoundException for an unknown id")
    void shouldThrowNotFound_whenIdDoesNotExist() {
        when(customerRepository.findCustomerById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findByIdUsingJpa throws NotFoundException for an unknown id")
    void shouldThrowNotFound_whenJpaLookupDoesNotFindCustomer() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findByIdUsingJpa(99))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("saveCustomer rejects a CPF that is already registered")
    void shouldRejectCustomer_whenCpfAlreadyExists() {
        when(customerRepository.findFirstByCpf("75386544020"))
                .thenReturn(Optional.of(Customer.builder().idCustomer(1).build()));

        assertThatThrownBy(() ->
                        customerService.saveCustomer(
                                input("753.865.440-20", phone("11999991111", true))))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage(CPF_IN_USE);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveCustomer requires at least one phone")
    void shouldRejectCustomer_whenThereIsNoPhone() {
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.saveCustomer(input("75386544020")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("É necessário cadastrar ao menos um telefone");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveCustomer rejects more than one primary phone")
    void shouldRejectCustomer_whenThereAreTwoPrimaryPhones() {
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        customerService.saveCustomer(
                                input(
                                        "75386544020",
                                        phone("11999991111", true),
                                        phone("11999992222", true))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Não é possível ter mais de um telefone primário");
    }

    @Test
    @DisplayName("saveCustomer rejects repeated phone numbers")
    void shouldRejectCustomer_whenPhoneNumbersAreRepeated() {
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        customerService.saveCustomer(
                                input(
                                        "75386544020",
                                        phone("11999991111", true),
                                        phone("11999991111", false))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Não é possível ter telefones com números iguais");
    }

    @Test
    @DisplayName("saveCustomer rejects a primary phone already primary for another customer")
    void shouldRejectCustomer_whenPrimaryPhoneBelongsToAnotherCustomer() {
        Phone existing = Phone.builder().idCellphone(5).number("11999991111").build();
        Customer owner = Customer.builder().idCustomer(8).build();
        CustomerPhone association = new CustomerPhone(owner, existing, true, "alias");
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.empty());
        when(phoneRepository.findByNumber("11999991111")).thenReturn(Optional.of(existing));
        when(customerPhoneRepository.findByPhone_IdCellphone(5)).thenReturn(List.of(association));

        assertThatThrownBy(() ->
                        customerService.saveCustomer(
                                input("75386544020", phone("11999991111", true))))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage(
                        "O número informado já está cadastrado como principal para outro cliente");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveCustomer stores digits-only CPF, links phones and records the event")
    void shouldSaveCustomer_whenRequestIsValid() {
        Phone created = Phone.builder().idCellphone(11).number("11999991111").build();
        Customer saved = Customer.builder().idCustomer(7).build();
        CustomerOutputDto dto = CustomerOutputDto.builder().customerId(7).build();
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.empty());
        when(phoneRepository.findByNumber("11999991111"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(created));
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);
        when(customerPhoneRepository.findByCustomerIdAndPhoneNumber(7, "11999991111"))
                .thenReturn(Optional.empty());
        when(customerRepository.findCustomerById(7)).thenReturn(Optional.of(dto));

        CustomerOutputDto result =
                customerService.saveCustomer(input("753.865.440-20", phone("11999991111", true)));

        assertThat(result).isSameAs(dto);
        ArgumentCaptor<Customer> customer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customer.capture());
        assertThat(customer.getValue().getCpf()).isEqualTo("75386544020");
        verify(phoneRepository).insertIgnore("11999991111");
        verify(customerPhoneRepository).clearMainPhoneForCustomer(7);
        ArgumentCaptor<CustomerPhone> link = ArgumentCaptor.forClass(CustomerPhone.class);
        verify(customerPhoneRepository).save(link.capture());
        assertThat(link.getValue().isMain()).isTrue();
        assertThat(link.getValue().getPhoneAlias()).isEqualTo("alias");
        verify(customerRepository).flush();
        verify(eventRecorder)
                .record(
                        eq(ObservabilityEventCatalog.CUSTOMER_CREATED),
                        eq(Map.of("customer.phone_count_band", "1")));
    }

    @Test
    @DisplayName("updateCustomer throws NotFoundException for an unknown customer")
    void shouldThrowNotFound_whenUpdatingUnknownCustomer() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        customerService.updateCustomer(
                                99, input("75386544020", phone("11999991111", true))))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(phoneRepository);
    }

    @Test
    @DisplayName("updateCustomer rejects a CPF that belongs to another customer")
    void shouldRejectUpdate_whenCpfBelongsToAnotherCustomer() {
        when(customerRepository.findById(1))
                .thenReturn(Optional.of(Customer.builder().idCustomer(1).build()));
        when(customerRepository.findFirstByCpf("75386544020"))
                .thenReturn(Optional.of(Customer.builder().idCustomer(2).build()));

        assertThatThrownBy(() ->
                        customerService.updateCustomer(
                                1, input("75386544020", phone("11999991111", true))))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage(CPF_IN_USE);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCustomer accepts the customer's own CPF and own primary phone")
    void shouldUpdateCustomer_whenCpfAndPhoneBelongToTheSameCustomer() {
        Customer existing = Customer.builder().idCustomer(1).cpf("75386544020").build();
        Phone ownPhone = Phone.builder().idCellphone(5).number("11999991111").build();
        CustomerPhone ownAssociation = new CustomerPhone(existing, ownPhone, true, "old");
        CustomerOutputDto dto = CustomerOutputDto.builder().customerId(1).build();
        when(customerRepository.findById(1)).thenReturn(Optional.of(existing));
        when(customerRepository.findFirstByCpf("75386544020")).thenReturn(Optional.of(existing));
        when(phoneRepository.findByNumber("11999991111")).thenReturn(Optional.of(ownPhone));
        when(customerPhoneRepository.findByPhone_IdCellphone(5))
                .thenReturn(List.of(ownAssociation));
        when(customerPhoneRepository.findByCustomerIdAndPhoneNumber(1, "11999991111"))
                .thenReturn(Optional.empty());
        when(customerRepository.findCustomerById(1)).thenReturn(Optional.of(dto));

        CustomerOutputDto result =
                customerService.updateCustomer(
                        1, input("753.865.440-20", phone("11999991111", true)));

        assertThat(result).isSameAs(dto);
        verify(customerPhoneRepository).deleteByCustomerIdCustomer(1);
        verify(customerRepository).save(existing);
        assertThat(existing.getCpf()).isEqualTo("75386544020");
        verify(eventRecorder, never()).record(any(), anyMap());
    }
}
