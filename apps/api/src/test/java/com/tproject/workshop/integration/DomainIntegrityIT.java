package com.tproject.workshop.integration;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import com.tproject.workshop.service.CustomerService;
import com.tproject.workshop.service.DeviceEventListener;
import com.tproject.workshop.service.TypesBrandsModelsService;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql({"/test-scripts/cleanTestData.sql"})
public class DomainIntegrityIT extends AbstractIntegrationLiveTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TypesBrandsModelsService typesBrandsModelsService;

    @Autowired
    private DeviceEventListener deviceEventListener;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Per-customer phone alias should isolate Customer A from Customer B editing a shared phone")
    void shouldIsolatePhoneAliasPerCustomer() {
        String sharedNumber = "11988887777";

        CustomerOutputDto customerA = customerService.saveCustomer(new InputCustomerDtoRecord(
                "Cliente A", "31781477051", "M", "a@example.com",
                List.of(new InputPhoneDto(null, sharedNumber, "Casa A", true))));

        CustomerOutputDto customerB = customerService.saveCustomer(new InputCustomerDtoRecord(
                "Cliente B", "75386544020", "F", "b@example.com",
                List.of(new InputPhoneDto(null, sharedNumber, "Trabalho B", true))));

        // Customer B updates their alias for the shared phone
        customerService.updateCustomer(customerB.getCustomerId(), new InputCustomerDtoRecord(
                "Cliente B", "75386544020", "F", "b@example.com",
                List.of(new InputPhoneDto(null, sharedNumber, "Celular B", true))));

        CustomerOutputDto reloadedA = customerService.findById(customerA.getCustomerId());
        CustomerOutputDto reloadedB = customerService.findById(customerB.getCustomerId());
        CustomerOutputDto reloadedAJpa = customerService.findByIdUsingJpa(customerA.getCustomerId());

        assertThat(reloadedA.getPhones()).extracting("name").containsExactly("Casa A");
        assertThat(reloadedAJpa.getPhones()).extracting("name").containsExactly("Casa A");
        assertThat(reloadedB.getPhones()).extracting("name").containsExactly("Celular B");
    }

    @Test
    @DisplayName("DeviceEventListener should update last_viewed_at without modifying last_update or version")
    void shouldUpdateLastViewedAtWithoutTouchingLastUpdateOrVersion() {
        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .name("Viewer Customer")
                .cpf("46203912042")
                .gender("M")
                .email("viewer@example.com")
                .build());

        BrandsModelsTypes bmt = typesBrandsModelsService.createOrReturnExistentBrandModelType(
                new TypeBrandModelInputDtoRecord("Celular", "Samsung", "A54"));

        Device device = new Device();
        device.setCustomer(customer);
        device.setBrandsModelsTypes(bmt);
        device.setColorIds(List.of());
        device.setProblem("Tela");
        device.setBudgetFee(BigDecimal.ZERO);
        device.setDeviceStatus(DeviceStatusEnum.NOVO);
        device.setEntryDate(Timestamp.from(Instant.now()));
        Device saved = deviceRepository.saveAndFlush(device);

        Timestamp originalLastUpdate = saved.getLastUpdate();
        Long originalVersion = saved.getVersion();

        deviceEventListener.onDeviceViewed(new DeviceViewedEvent(this, saved.getId()));
        entityManager.clear();

        Device afterView = deviceRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterView.getLastViewedAt()).isNotNull();
        assertThat(afterView.getLastUpdate()).isEqualTo(originalLastUpdate);
        assertThat(afterView.getVersion()).isEqualTo(originalVersion);
    }

    @Test
    @DisplayName("Optimistic locking @Version should reject stale updates on Device, Customer, and Payment")
    void shouldRejectStaleVersionUpdatesWithOptimisticLocking() {
        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .name("Versioned Customer")
                .cpf("45931651055")
                .gender("F")
                .email("version@example.com")
                .build());

        Customer staleCustomer = Customer.builder()
                .idCustomer(customer.getIdCustomer())
                .name("Stale Name")
                .cpf(customer.getCpf())
                .gender(customer.getGender())
                .email(customer.getEmail())
                .createdAt(customer.getCreatedAt())
                .version(customer.getVersion())
                .build();

        customer.setName("Updated Name");
        customerRepository.saveAndFlush(customer);

        assertThatThrownBy(() -> customerRepository.saveAndFlush(staleCustomer))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        BrandsModelsTypes bmt = typesBrandsModelsService.createOrReturnExistentBrandModelType(
                new TypeBrandModelInputDtoRecord("Notebook", "Dell", "Inspiron"));

        Device device = new Device();
        device.setCustomer(customer);
        device.setBrandsModelsTypes(bmt);
        device.setColorIds(List.of());
        device.setProblem("Teclado");
        device.setBudgetFee(BigDecimal.ZERO);
        device.setDeviceStatus(DeviceStatusEnum.NOVO);
        device.setEntryDate(Timestamp.from(Instant.now()));
        Device savedDevice = deviceRepository.saveAndFlush(device);

        Device staleDevice = new Device();
        staleDevice.setId(savedDevice.getId());
        staleDevice.setCustomer(customer);
        staleDevice.setBrandsModelsTypes(bmt);
        staleDevice.setColorIds(List.of());
        staleDevice.setProblem("Stale Problem");
        staleDevice.setBudgetFee(BigDecimal.ZERO);
        staleDevice.setDeviceStatus(DeviceStatusEnum.NOVO);
        staleDevice.setEntryDate(savedDevice.getEntryDate());
        staleDevice.setVersion(savedDevice.getVersion());

        savedDevice.setProblem("Updated Problem");
        deviceRepository.saveAndFlush(savedDevice);

        assertThatThrownBy(() -> deviceRepository.saveAndFlush(staleDevice))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Payment payment = new Payment();
        payment.setDevice(savedDevice);
        payment.setPaymentDate(Timestamp.from(Instant.now()));
        payment.setPaymentType(PaymentMethodEnum.PIX);
        payment.setPaymentValue(new BigDecimal("100.00"));
        payment.setCategory(PaymentCategoryEnum.SERVICOS);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        Payment stalePayment = new Payment();
        stalePayment.setId(savedPayment.getId());
        stalePayment.setDevice(savedDevice);
        stalePayment.setVersion(savedPayment.getVersion());

        savedPayment.setDevice(savedDevice);
        savedPayment.setUpdatedAt(Timestamp.from(Instant.now()));
        paymentRepository.saveAndFlush(savedPayment);

        // If updatedAt changed, version bumped to 1 while stalePayment still has version 0
        assertThat(paymentRepository.findById(savedPayment.getId()).orElseThrow().getVersion())
                .isGreaterThan(stalePayment.getVersion());
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(stalePayment))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
