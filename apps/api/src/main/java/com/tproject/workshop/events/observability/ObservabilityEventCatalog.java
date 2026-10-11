package com.tproject.workshop.events.observability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stable event names and their version-one attribute contracts.
 *
 * <p>Values that identify a customer, device, order or trace are intentionally
 * absent from these schemas. If a future event needs another shape, it must
 * receive a new version instead of silently changing an existing contract.</p>
 */
public final class ObservabilityEventCatalog {

    public static final int CATALOG_VERSION = 1;

    public static final ObservabilityEventContract SERVICE_ORDER_CREATED = contract(
            "workshop.service_order.created",
            EventSeverity.INFO,
            Set.of("client.platform", "actor.role", "result"),
            Set.of());

    public static final ObservabilityEventContract SERVICE_ORDER_STATUS_CHANGED = contract(
            "workshop.service_order.status_changed",
            EventSeverity.INFO,
            Set.of("status.previous", "status.new", "actor.role"),
            Set.of("result"));

    public static final ObservabilityEventContract SERVICE_ORDER_BUDGET_CHANGED = contract(
            "workshop.service_order.budget_changed",
            EventSeverity.INFO,
            Set.of("budget.change_type", "budget.value_band", "result"),
            Set.of("actor.role"));

    public static final ObservabilityEventContract PAYMENT_CREATED = contract(
            "workshop.payment.created",
            EventSeverity.INFO,
            Set.of("payment.method", "payment.category", "result"),
            Set.of("actor.role"));

    public static final ObservabilityEventContract AUTH_LOGIN_FAILED = contract(
            "workshop.auth.login.failed",
            EventSeverity.WARN,
            Set.of("reason.code", "client.platform"),
            Set.of());

    public static final ObservabilityEventContract API_KEY_REVOKED = contract(
            "workshop.api_key.revoked",
            EventSeverity.INFO,
            Set.of("actor.role", "reason.code"),
            Set.of());

    public static final ObservabilityEventContract BACKUP_COMPLETED = contract(
            "workshop.backup.completed",
            EventSeverity.INFO,
            Set.of("backup.type", "duration.band", "result"),
            Set.of());

    public static final ObservabilityEventContract BACKUP_FAILED = contract(
            "workshop.backup.failed",
            EventSeverity.ERROR,
            Set.of("backup.type", "duration.band", "result"),
            Set.of("error.code"));

    public static final ObservabilityEventContract DEPLOYMENT_COMPLETED = contract(
            "workshop.deployment.completed",
            EventSeverity.INFO,
            Set.of("deployment.service", "deployment.version", "deployment.environment", "result"),
            Set.of());

    public static final ObservabilityEventContract DEPLOYMENT_ROLLED_BACK = contract(
            "workshop.deployment.rolled_back",
            EventSeverity.WARN,
            Set.of("deployment.service", "deployment.version", "deployment.environment", "result"),
            Set.of("reason.code"));

    public static final ObservabilityEventContract CLIENT_SYNC_FAILED = contract(
            "workshop.client.sync.failed",
            EventSeverity.ERROR,
            Set.of("client.platform", "error.class", "retry.policy"),
            Set.of("result"));

    private static final List<ObservabilityEventContract> CONTRACTS = List.of(
            SERVICE_ORDER_CREATED,
            SERVICE_ORDER_STATUS_CHANGED,
            SERVICE_ORDER_BUDGET_CHANGED,
            PAYMENT_CREATED,
            AUTH_LOGIN_FAILED,
            API_KEY_REVOKED,
            BACKUP_COMPLETED,
            BACKUP_FAILED,
            DEPLOYMENT_COMPLETED,
            DEPLOYMENT_ROLLED_BACK,
            CLIENT_SYNC_FAILED);

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
