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
        assertThat(ObservabilityEventCatalog.all()).hasSize(11);
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
    void shouldCreateImmutableValidatedEvent() {
        Map<String, String> attributes = new HashMap<>(Map.of(
                "client.platform", "desktop",
                "actor.role", "admin",
                "result", "success"));

        ObservabilityEvent event = ObservabilityEvent.of(
                "workshop.service_order.created",
                Instant.parse("2026-09-06T12:00:00Z"),
                attributes);
        attributes.put("result", "failed");

        assertThat(event.name()).isEqualTo("workshop.service_order.created");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.severity()).isEqualTo(EventSeverity.INFO);
        assertThat(event.attributes()).containsEntry("result", "success");
        assertThatThrownBy(() -> event.attributes().put("result", "failed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowBoundedEnumValuesWithUnderscores() {
        ObservabilityEvent event = ObservabilityEvent.of(
                "workshop.service_order.status_changed",
                Instant.now(),
                Map.of(
                        "status.previous", "EM_ANDAMENTO",
                        "status.new", "NAO_APROVADO",
                        "actor.role", "ROLE_ADMIN"));

        assertThat(event.attributes()).containsEntry("status.new", "NAO_APROVADO");
    }

    @Test
    void shouldRejectMissingUnknownAndSensitiveAttributes() {
        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.service_order.created",
                Instant.now(),
                Map.of("client.platform", "desktop", "actor.role", "admin")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.service_order.created",
                Instant.now(),
                Map.of(
                        "client.platform", "desktop",
                        "actor.role", "admin",
                        "result", "success",
                        "email", "customer@example.com")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEventValidator.validate(
                ObservabilityEventCatalog.SERVICE_ORDER_CREATED,
                Map.of(
                        "client.platform", "desktop",
                        "actor.role", "admin",
                        "result", "success",
                        "service_order_id", "123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectFreeTextAndUnboundedAttributeValues() {
        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.auth.login.failed",
                Instant.now(),
                Map.of("reason.code", "password was sent in the request", "client.platform", "desktop")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.auth.login.failed",
                Instant.now(),
                Map.of("reason.code", "x".repeat(65), "client.platform", "desktop")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.service_order.created",
                Instant.now(),
                Map.of(
                        "client.platform", "desktop",
                        "actor.role", "admin",
                        "result", "customer-specific-result")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectUnknownEventName() {
        assertThatThrownBy(() -> ObservabilityEvent.of(
                "workshop.customer.created",
                Instant.now(),
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
