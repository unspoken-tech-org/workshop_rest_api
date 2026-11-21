package com.tproject.workshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
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
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;

@Sql({"/test-scripts/cleanTestData.sql"})
public class DomainIntegrityIT extends AbstractIntegrationLiveTest {

    @Autowired private CustomerService customerService;

    @Autowired private CustomerRepository customerRepository;

    @Autowired private DeviceRepository deviceRepository;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private TypesBrandsModelsService typesBrandsModelsService;

    @Autowired private DeviceEventListener deviceEventListener;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName(
            "Per-customer phone alias should isolate Customer A from Customer B editing a shared phone")
    void shouldIsolatePhoneAliasPerCustomer() {
        String sharedNumber = "11988887777";

        CustomerOutputDto customerA =
                customerService.saveCustomer(
                        new InputCustomerDtoRecord(
                                "Cliente A",
                                "31781477051",
                                "M",
                                "a@example.com",
                                List.of(new InputPhoneDto(null, "Casa A", sharedNumber, true))));

        CustomerOutputDto customerB =
                customerService.saveCustomer(
                        new InputCustomerDtoRecord(
                                "Cliente B",
                                "75386544020",
                                "F",
                                "b@example.com",
                                List.of(
                                        new InputPhoneDto(null, "Principal B", "11988886666", true),
                                        new InputPhoneDto(
                                                null, "Trabalho B", sharedNumber, false))));

        // Customer B updates their alias for the shared phone
        customerService.updateCustomer(
                customerB.getCustomerId(),
                new InputCustomerDtoRecord(
                        "Cliente B",
                        "75386544020",
                        "F",
                        "b@example.com",
                        List.of(
                                new InputPhoneDto(null, "Principal B", "11988886666", true),
                                new InputPhoneDto(null, "Celular B", sharedNumber, false))));

        CustomerOutputDto reloadedA = customerService.findById(customerA.getCustomerId());
        CustomerOutputDto reloadedB = customerService.findById(customerB.getCustomerId());
        CustomerOutputDto reloadedAJpa =
                customerService.findByIdUsingJpa(customerA.getCustomerId());

        assertThat(reloadedA.getPhones()).extracting("name").containsExactly("Casa A");
        assertThat(reloadedAJpa.getPhones()).extracting("name").containsExactly("Casa A");
        assertThat(reloadedB.getPhones())
                .extracting("name")
                .containsExactlyInAnyOrder("Principal B", "Celular B");
    }

    @Test
    @DisplayName(
            "DeviceEventListener should update last_viewed_at without modifying last_update or version")
    void shouldUpdateLastViewedAtWithoutTouchingLastUpdateOrVersion() {
        Customer customer =
                customerRepository.saveAndFlush(
                        Customer.builder()
                                .name("Viewer Customer")
                                .cpf("46203912042")
                                .gender("M")
                                .email("viewer@example.com")
                                .build());

        BrandsModelsTypes bmt =
                typesBrandsModelsService.createOrReturnExistentBrandModelType(
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

        Device afterView = null;
        Instant deadline = Instant.now().plusSeconds(3);
        while (Instant.now().isBefore(deadline)) {
            entityManager.clear();
            afterView = deviceRepository.findById(saved.getId()).orElseThrow();
            if (afterView.getLastViewedAt() != null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertThat(afterView).isNotNull();
        assertThat(afterView.getLastViewedAt()).isNotNull();
        assertThat(afterView.getLastUpdate()).isEqualTo(originalLastUpdate);
        assertThat(afterView.getVersion()).isEqualTo(originalVersion);
    }

    @Test
    @DisplayName(
            "Optimistic locking @Version should reject stale updates on Device, Customer, and Payment")
    void shouldRejectStaleVersionUpdatesWithOptimisticLocking() {
        Customer customer =
                customerRepository.saveAndFlush(
                        Customer.builder()
                                .name("Versioned Customer")
                                .cpf("45931651055")
                                .gender("F")
                                .email("version@example.com")
                                .build());

        Customer staleCustomer =
                Customer.builder()
                        .idCustomer(customer.getIdCustomer())
                        .name("Stale Name")
                        .cpf(customer.getCpf())
                        .gender(customer.getGender())
                        .email(customer.getEmail())
                        .createdAt(customer.getCreatedAt())
                        .version(customer.getVersion())
                        .build();

        customer.setName("Updated Name");
        Customer updatedCustomer = customerRepository.saveAndFlush(customer);

        assertThatThrownBy(() -> customerRepository.saveAndFlush(staleCustomer))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        BrandsModelsTypes bmt =
                typesBrandsModelsService.createOrReturnExistentBrandModelType(
                        new TypeBrandModelInputDtoRecord("Notebook", "Dell", "Inspiron"));

        Device device = new Device();
        device.setCustomer(updatedCustomer);
        device.setBrandsModelsTypes(bmt);
        device.setColorIds(List.of());
        device.setProblem("Teclado");
        device.setBudgetFee(BigDecimal.ZERO);
        device.setDeviceStatus(DeviceStatusEnum.NOVO);
        device.setEntryDate(Timestamp.from(Instant.now()));
        Device savedDevice = deviceRepository.saveAndFlush(device);

        Device staleDevice = new Device();
        staleDevice.setId(savedDevice.getId());
        staleDevice.setCustomer(updatedCustomer);
        staleDevice.setBrandsModelsTypes(bmt);
        staleDevice.setColorIds(List.of());
        staleDevice.setProblem("Stale Problem");
        staleDevice.setBudgetFee(BigDecimal.ZERO);
        staleDevice.setDeviceStatus(DeviceStatusEnum.NOVO);
        staleDevice.setEntryDate(savedDevice.getEntryDate());
        staleDevice.setVersion(savedDevice.getVersion());

        savedDevice.setProblem("Updated Problem");
        Device updatedDevice = deviceRepository.saveAndFlush(savedDevice);

        assertThatThrownBy(() -> deviceRepository.saveAndFlush(staleDevice))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Device currentDevice = deviceRepository.findById(updatedDevice.getId()).orElseThrow();

        Payment payment = new Payment();
        payment.setDevice(currentDevice);
        payment.setPaymentDate(Timestamp.from(Instant.now()));
        payment.setPaymentType(PaymentMethodEnum.PIX);
        payment.setPaymentValue(new BigDecimal("100.00"));
        payment.setCategory(PaymentCategoryEnum.SERVICOS);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        Payment stalePayment = new Payment();
        stalePayment.setId(savedPayment.getId());
        stalePayment.setDevice(currentDevice);
        stalePayment.setVersion(savedPayment.getVersion());

        savedPayment.setDevice(currentDevice);
        savedPayment.setUpdatedAt(Timestamp.from(Instant.now()));
        paymentRepository.saveAndFlush(savedPayment);

        // If updatedAt changed, version bumped to 1 while stalePayment still has version 0
        assertThat(paymentRepository.findById(savedPayment.getId()).orElseThrow().getVersion())
                .isGreaterThan(stalePayment.getVersion());
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(stalePayment))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("V15 should create foreign key, history timeline, and pg_trgm GIN indexes")
    void shouldCreateV15ForeignKeyAndTrigramIndexes() {
        List<String> existingIndexes =
                jdbcTemplate.queryForList(
                        "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
                        String.class);

        assertThat(existingIndexes)
                .contains(
                        "ix_devices_id_customer",
                        "ix_devices_id_brand_model_type",
                        "ix_devices_id_technician",
                        "ix_devices_last_viewed_at",
                        "ix_device_history_id_device",
                        "ix_device_history_history_date",
                        "ix_payments_id_device",
                        "ix_customer_contact_id_device_last_contact",
                        "ix_customer_phones_id_phone",
                        "ix_customers_name_trgm",
                        "ix_customers_email_trgm",
                        "ix_customers_cpf_trgm",
                        "ix_phones_number_trgm",
                        "ix_brands_brand_trgm",
                        "ix_models_model_trgm",
                        "ix_types_type_trgm");

        String normalized =
                jdbcTemplate.queryForObject(
                        "SELECT public.immutable_unaccent('Conceição JoãO')", String.class);
        assertThat(normalized).isEqualTo("Conceicao JoaO");
    }

    @Test
    @DisplayName("V16 should enforce payment and device CHECK constraints and NOT NULL columns")
    void shouldEnforceV16PaymentAndDeviceConstraints() {
        List<String> constraints =
                jdbcTemplate.queryForList(
                        "SELECT conname FROM pg_constraint WHERE contype = 'c'", String.class);

        assertThat(constraints)
                .contains(
                        "chk_payments_payment_value_positive",
                        "chk_payments_payment_type_valid",
                        "chk_payments_category_valid",
                        "chk_devices_budget_fee_non_negative",
                        "chk_devices_service_value_non_negative",
                        "chk_devices_status_valid",
                        "chk_devices_departure_after_entry");

        String budgetFeeNullable =
                jdbcTemplate.queryForObject(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_name = 'devices' AND column_name = 'budget_fee'",
                        String.class);
        String budgetFeeCollectedNullable =
                jdbcTemplate.queryForObject(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_name = 'devices' AND column_name = 'budget_fee_collected'",
                        String.class);

        assertThat(budgetFeeNullable).isEqualTo("NO");
        assertThat(budgetFeeCollectedNullable).isEqualTo("NO");
    }
}
