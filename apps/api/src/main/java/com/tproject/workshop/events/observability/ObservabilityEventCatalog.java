package com.tproject.workshop.events.observability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stable event names and their version-one attribute contracts.
 *
 * <p>Values that identify a customer, device, payment, user or trace are
 * intentionally absent from these schemas. Domain facts are emitted by the
 * backend only after the transaction commits, so they carry no {@code result}.
 * Application events stay at WARN or below because ERROR logs count in the
 * deploy canary; only host-side backup failures use ERROR. Once an event is
 * emitted, a new shape needs a new version instead of a silent change.</p>
 */
public final class ObservabilityEventCatalog {

    public static final int CATALOG_VERSION = 1;

    public static final ObservabilityEventContract DEVICE_CREATED = contract(
            "workshop.device.created",
            EventSeverity.INFO,
            Set.of("actor.role", "device.urgency"),
            Set.of("client.platform"));

    public static final ObservabilityEventContract DEVICE_STATUS_CHANGED = contract(
            "workshop.device.status_changed",
            EventSeverity.INFO,
            Set.of("status.previous", "status.new", "change.origin", "actor.role"),
            Set.of());

    public static final ObservabilityEventContract DEVICE_BUDGET_CHANGED = contract(
            "workshop.device.budget_changed",
            EventSeverity.INFO,
            Set.of("budget.field", "budget.change_type", "budget.value_band", "actor.role"),
            Set.of());

    public static final ObservabilityEventContract DEVICE_URGENCY_CHANGED = contract(
            "workshop.device.urgency_changed",
            EventSeverity.INFO,
            Set.of("device.urgency", "actor.role"),
            Set.of());

    public static final ObservabilityEventContract PAYMENT_CREATED = contract(
            "workshop.payment.created",
            EventSeverity.INFO,
            Set.of("payment.method", "payment.category", "payment.value_band", "actor.role"),
            Set.of());

    public static final ObservabilityEventContract CUSTOMER_CONTACT_REGISTERED = contract(
            "workshop.customer_contact.registered",
            EventSeverity.INFO,
            Set.of("contact.made", "status.new", "actor.role"),
            Set.of());

    public static final ObservabilityEventContract API_KEY_REVOKED = contract(
            "workshop.api_key.revoked",
            EventSeverity.INFO,
            Set.of("actor.role", "scope"),
            Set.of());

    public static final ObservabilityEventContract API_KEY_DEVICE_RESET = contract(
            "workshop.api_key.device_reset",
            EventSeverity.INFO,
            Set.of("actor.role"),
            Set.of());

    public static final ObservabilityEventContract BACKUP_COMPLETED = contract(
            "workshop.backup.completed",
            EventSeverity.INFO,
            Set.of("backup.type", "duration.band"),
            Set.of());

    public static final ObservabilityEventContract BACKUP_FAILED = contract(
            "workshop.backup.failed",
            EventSeverity.ERROR,
            Set.of("backup.type", "duration.band"),
            Set.of());

    public static final ObservabilityEventContract DEPLOYMENT_COMPLETED = contract(
            "workshop.deployment.completed",
            EventSeverity.INFO,
            Set.of("deploy.service", "deploy.environment", "deployment.version", "result"),
            Set.of("reason.code"));

    public static final ObservabilityEventContract DEPLOYMENT_ROLLED_BACK = contract(
            "workshop.deployment.rolled_back",
            EventSeverity.WARN,
            Set.of("deploy.service", "deploy.environment", "deployment.version", "result", "reason.code"),
            Set.of());

    public static final ObservabilityEventContract DEPLOYMENT_GATE_BLOCKED = contract(
            "workshop.deployment.gate_blocked",
            EventSeverity.WARN,
            Set.of("deploy.service", "deploy.environment", "reason.code"),
            Set.of());

    private static final List<ObservabilityEventContract> CONTRACTS = List.of(
            DEVICE_CREATED,
            DEVICE_STATUS_CHANGED,
            DEVICE_BUDGET_CHANGED,
            DEVICE_URGENCY_CHANGED,
            PAYMENT_CREATED,
            CUSTOMER_CONTACT_REGISTERED,
            API_KEY_REVOKED,
            API_KEY_DEVICE_RESET,
            BACKUP_COMPLETED,
            BACKUP_FAILED,
            DEPLOYMENT_COMPLETED,
            DEPLOYMENT_ROLLED_BACK,
            DEPLOYMENT_GATE_BLOCKED);

    private static final Map<String, ObservabilityEventContract> BY_NAME = index(CONTRACTS);

    private ObservabilityEventCatalog() {
    }

    public static List<ObservabilityEventContract> all() {
        return CONTRACTS;
    }

    public static ObservabilityEventContract require(String name) {
        ObservabilityEventContract contract = BY_NAME.get(name);
        if (contract == null) {
            throw new IllegalArgumentException("Unknown observability event contract");
        }
        return contract;
    }

    private static ObservabilityEventContract contract(
            String name,
            EventSeverity severity,
            Set<String> required,
            Set<String> optional) {
        return new ObservabilityEventContract(name, CATALOG_VERSION, severity, required, optional);
    }

    private static Map<String, ObservabilityEventContract> index(List<ObservabilityEventContract> contracts) {
        Map<String, ObservabilityEventContract> index = new LinkedHashMap<>();
        for (ObservabilityEventContract contract : contracts) {
            if (index.put(contract.name(), contract) != null) {
                throw new IllegalStateException("Duplicate observability event contract");
            }
        }
        return Collections.unmodifiableMap(index);
    }
}
