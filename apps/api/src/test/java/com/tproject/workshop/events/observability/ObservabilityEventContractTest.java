package com.tproject.workshop.events.observability;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservabilityEventContractTest {

    @Test
    void shouldExposeStableVersionOneCatalog() {
        assertThat(ObservabilityEventCatalog.all()).hasSize(19);
        assertThat(ObservabilityEventCatalog.all())
                .allSatisfy(contract -> {
                    assertThat(contract.version()).isEqualTo(ObservabilityEventCatalog.CATALOG_VERSION);
                    assertThat(contract.name()).startsWith("workshop.");
                    assertThat(contract.allowedAttributes()).containsAll(contract.requiredAttributes());
                });
        assertThat(ObservabilityEventCatalog.require("workshop.payment.created"))
                .isSameAs(ObservabilityEventCatalog.PAYMENT_CREATED);
    }

    @Test
    void shouldCloseTheVocabularyOfEveryCatalogAttribute() {
        assertThat(ObservabilityEventCatalog.all())
                .flatMap(ObservabilityEventContract::allowedAttributes)
                .allSatisfy(name -> assertThat(ObservabilityEventValidator.hasClosedVocabulary(name))
                        .as("closed vocabulary for %s", name)
                        .isTrue());
    }

    @Test
    void shouldKeepApplicationEventsAtWarnOrBelow() {
        assertThat(ObservabilityEventCatalog.all())
                .filteredOn(contract -> contract.severity() == EventSeverity.ERROR)
                .containsExactly(ObservabilityEventCatalog.BACKUP_FAILED);
    }

    @Test
    void shouldNotCarryResultOnPostCommitDomainFacts() {
        assertThat(ObservabilityEventCatalog.all())
                .filteredOn(contract -> !contract.name().startsWith("workshop.deployment."))
                .allSatisfy(contract -> assertThat(contract.allowedAttributes()).doesNotContain("result"));
    }

    @Test
    void shouldCreateImmutableValidatedEvent() {
        Map<String, String> attributes = new HashMap<>(Map.of(
                "actor.role", "admin",
                "device.urgency", "normal"));

        ObservabilityEvent event = ObservabilityEvent.of(
                "workshop.device.created",
                Instant.parse("2026-09-06T12:00:00Z"),
                attributes);
        attributes.put("device.urgency", "urgent");

        assertThat(event.name()).isEqualTo("workshop.device.created");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.severity()).isEqualTo(EventSeverity.INFO);
        assertThat(event.attributes()).containsEntry("device.urgency", "normal");
        assertThatThrownBy(() -> event.attributes().put("device.urgency", "urgent"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAcceptOptionalClientPlatform() {
        ObservabilityEvent event = ObservabilityEvent.of(
                "workshop.device.created",
                Instant.now(),
                Map.of("actor.role", "service", "device.urgency", "urgent", "client.platform", "desktop"));

        assertThat(event.attributes()).containsEntry("client.platform", "desktop");
    }

    @Test
    void shouldAllowBoundedEnumValuesWithUnderscores() {
        ObservabilityEvent event = ObservabilityEvent.of(
                "workshop.device.status_changed",
                Instant.now(),
                Map.of(
                        "status.previous", "EM_ANDAMENTO",
                        "status.new", "NAO_APROVADO",
                        "change.origin", "status",
                        "actor.role", "ADMIN"));

        assertThat(event.attributes()).containsEntry("status.new", "NAO_APROVADO");
    }

    @Test
    void shouldAcceptOnlyReleaseVersionsOrCommitShas() {
        Map<String, String> attributes = new HashMap<>(Map.of(
                "deploy.service", "api",
                "deploy.environment", "production",
                "result", "ok"));

        attributes.put("deployment.version", "v1.23.0");
        assertThat(ObservabilityEvent.of("workshop.deployment.completed", Instant.now(), attributes)
                .attributes()).containsEntry("deployment.version", "v1.23.0");

        attributes.put("deployment.version", "7364274");
        assertThat(ObservabilityEvent.of("workshop.deployment.completed", Instant.now(), attributes)
                .attributes()).containsEntry("deployment.version", "7364274");

        attributes.put("deployment.version", "latest");
        assertThatThrownBy(() -> ObservabilityEvent.of("workshop.deployment.completed", Instant.now(), attributes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingUnknownAndSensitiveAttributes() {
        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.device.created",
                Instant.now(),
                Map.of("actor.role", "admin")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.device.created",
                Instant.now(),
                Map.of(
                        "actor.role", "admin",
                        "device.urgency", "normal",
                        "email", "customer@example.com")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEventValidator.validate(
                ObservabilityEventCatalog.DEVICE_CREATED,
                Map.of(
                        "actor.role", "admin",
                        "device.urgency", "normal",
                        "device_id", "123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectPersonalDataAndSecretsInClosedAttributes() {
        for (String value : new String[] {
                "12345678909",
                "11987654321",
                "sk_desktop_abcdef0123456789",
                "customer-name",
                "x".repeat(65)}) {
            assertThatThrownBy(() -> ObservabilityEvent.of(
                    "workshop.api_key.revoked",
                    Instant.now(),
                    Map.of("actor.role", "admin", "scope", value)))
                    .as("value %s", value)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.payment.created",
                Instant.now(),
                Map.of(
                        "payment.method", "pix",
                        "payment.category", "servicos",
                        "payment.value_band", "149.90",
                        "actor.role", "admin")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectRemovedContracts() {
        for (String name : new String[] {
                "workshop.service_order.created",
                "workshop.client.sync.failed",
                "workshop.auth.login.failed",
                "workshop.customer.created"}) {
            assertThatThrownBy(() -> ObservabilityEvent.of(name, Instant.now(), Map.of()))
                    .as("event %s", name)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
