package com.tproject.workshop.integration;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.model.Technician;
import com.tproject.workshop.repository.ColorRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.PhoneRepository;
import com.tproject.workshop.service.BrandService;
import com.tproject.workshop.service.ColorService;
import com.tproject.workshop.service.CustomerService;
import com.tproject.workshop.service.ModelService;
import com.tproject.workshop.service.TechnicianService;
import com.tproject.workshop.service.TypeService;
import com.tproject.workshop.service.TypesBrandsModelsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Sql({"/test-scripts/cleanTestData.sql"})
public class RaceConditionIT extends AbstractIntegrationLiveTest {

    private static final List<String> VALID_CPFS = List.of(
            "31781477051",
            "75386544020",
            "46203912042",
            "45931651055",
            "52998224725"
    );

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TypesBrandsModelsService typesBrandsModelsService;

    @Autowired
    private ColorService colorService;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private TypeService typeService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PhoneRepository phoneRepository;

    @Test
    @DisplayName("Should handle concurrent creation of technicians without duplicates")
    public void shouldHandleConcurrentTechnicianCreation() throws InterruptedException {
        int threads = 10;
        List<Future<Technician>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Callable<Technician>> tasks = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    latch.await();
                    Technician t = new Technician();
                    t.setName("Concurrent Technician");
                    t.setNumber("123456789");
                    return technicianService.save(t);
                });
            }

            latch.countDown();
            futures = executorService.invokeAll(tasks);
            executorService.shutdown();
        }

        for (Future<Technician> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // Ignore expected transient failures if any, but our retry should handle it
            }
        }

        long count = technicianService.findAll().stream()
                .filter(t -> t.name().equalsIgnoreCase("concurrent technician"))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle concurrent creation of Type-Brand-Model without duplicates")
    public void shouldHandleConcurrentTypeBrandModelCreation() throws InterruptedException, ExecutionException {
        int threads = 10;
        List<Future<Object>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(1);

            TypeBrandModelInputDtoRecord input = new TypeBrandModelInputDtoRecord("Smartphone", "Apple", "iPhone 15");

            List<Callable<Object>> tasks = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    latch.await();
                    return typesBrandsModelsService.createOrReturnExistentBrandModelType(input);
                });
            }

            latch.countDown();
            futures = executorService.invokeAll(tasks);
            executorService.shutdown();
        }

        for (Future<Object> future : futures) {
            assertThat(future.get()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle concurrent creation of catalog entries (color, brand, type, model) without aborting transaction")
    public void shouldHandleConcurrentCatalogCreation() throws InterruptedException, ExecutionException {
        int threads = 10;
        List<Future<Color>> colorFutures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Callable<Color>> tasks = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                tasks.add(() -> {
                    latch.await();
                    brandService.createOrReturnExistentBrand("  Samsung   Mobile  ");
                    typeService.createOrReturnExistentType("  Smart   TV  ");
                    modelService.createOrReturnExistentModel("  Galaxy   S25  ");
                    return colorService.createOrReturnExistentColor(idx % 2 == 0 ? "  Azul   Marinho  " : "azul marinho");
                });
            }

            latch.countDown();
            colorFutures = executorService.invokeAll(tasks);
            executorService.shutdown();
        }

        Integer expectedColorId = null;
        for (Future<Color> future : colorFutures) {
            Color color = future.get();
            assertThat(color.getColor()).isEqualTo("azul marinho");
            if (expectedColorId == null) {
                expectedColorId = color.getIdColor();
            } else {
                assertThat(color.getIdColor()).isEqualTo(expectedColorId);
            }
        }

        assertThat(colorRepository.findAll().stream()
                .filter(c -> c.getColor().equals("azul marinho"))
                .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle concurrent customer creation sharing the same phone number without transaction abort")
    public void shouldHandleConcurrentSharedPhoneCreation() throws InterruptedException, ExecutionException {
        int threads = VALID_CPFS.size();
        String sharedNumber = "11999998888";
        List<Future<CustomerOutputDto>> futures;

        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Callable<CustomerOutputDto>> tasks = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                tasks.add(() -> {
                    latch.await();
                    InputCustomerDtoRecord dto = new InputCustomerDtoRecord(
                            "Cliente " + idx,
                            VALID_CPFS.get(idx),
                            "M",
                            "cliente" + idx + "@example.com",
                            List.of(new InputPhoneDto(null, sharedNumber, "Alias " + idx, true))
                    );
                    return customerService.saveCustomer(dto);
                });
            }

            latch.countDown();
            futures = executorService.invokeAll(tasks);
            executorService.shutdown();
        }

        for (int i = 0; i < threads; i++) {
            CustomerOutputDto customer = futures.get(i).get();
            assertThat(customer.getPhones()).hasSize(1);
            assertThat(customer.getPhones().get(0).getNumber()).isEqualTo(sharedNumber);
            assertThat(customer.getPhones().get(0).getName()).isEqualTo("Alias " + i);
        }

        assertThat(phoneRepository.findByNumber(sharedNumber)).isPresent();
    }

    @Test
    @DisplayName("Should reject concurrent customer creation with duplicate CPF without creating duplicate rows")
    public void shouldRejectConcurrentDuplicateCpf() throws InterruptedException {
        int threads = 5;
        String duplicateCpf = "31781477051";
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<Void>> futures;

        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                tasks.add(() -> {
                    latch.await();
                    InputCustomerDtoRecord dto = new InputCustomerDtoRecord(
                            "Duplicate " + idx,
                            duplicateCpf,
                            "M",
                            "dup" + idx + "@example.com",
                            List.of(new InputPhoneDto(null, "1197777000" + idx, "Principal", true))
                    );
                    customerService.saveCustomer(dto);
                    successes.incrementAndGet();
                    return null;
                });
            }

            latch.countDown();
            futures = executorService.invokeAll(tasks);
            executorService.shutdown();
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof EntityAlreadyExistsException
                        || e.getCause() instanceof DataIntegrityViolationException) {
                    conflicts.incrementAndGet();
                } else {
                    throw new AssertionError("Unexpected exception during CPF race", e.getCause());
                }
            }
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
        assertThat(customerRepository.findAll().stream()
                .filter(c -> duplicateCpf.equals(c.getCpf()))
                .count()).isEqualTo(1);
    }
}
