package com.tproject.workshop.events.observability;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates event attributes before a future exporter is allowed to use them. */
public final class ObservabilityEventValidator {

    public static final int MAX_ATTRIBUTES = 16;
    public static final int MAX_ATTRIBUTE_KEY_LENGTH = 64;
    public static final int MAX_ATTRIBUTE_VALUE_LENGTH = 64;

    private static final Pattern EVENT_NAME = Pattern.compile(
            "workshop\\.[a-z0-9]+(?:[._-][a-z0-9]+)+");
    private static final Pattern ATTRIBUTE_NAME = Pattern.compile(
            "[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*");
    private static final Pattern NORMALIZED_VALUE = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:/+_\\-]{0," + (MAX_ATTRIBUTE_VALUE_LENGTH - 1) + "}");

    private static final Set<String> PROHIBITED_SEGMENTS = Set.of(
            "authorization",
            "access_token",
            "apikey",
            "api_key",
            "body",
            "cnpj",
            "cpf",
            "device_id",
            "email",
            "free_text",
            "message",
            "password",
            "payload",
            "phone",
            "query",
            "query_string",
            "secret",
            "service_order_id",
            "span_id",
            "token",
            "trace_id",
            "user_id");

    /**
     * Closed vocabularies keep categorical event attributes suitable for
     * aggregation. A new category is an explicit contract change rather than
     * an unbounded value supplied by a request.
     */
    private static final Map<String, Set<String>> LOW_CARDINALITY_VALUES = Map.ofEntries(
            Map.entry("client.platform", Set.of("mobile", "web", "desktop", "server", "unknown")),
            Map.entry("actor.role", Set.of("admin", "service", "system", "anonymous", "unknown",
                    "role_admin", "role_service")),
            Map.entry("result", Set.of("success", "ok", "failure", "failed", "error", "skipped", "unknown")),
            Map.entry("status.previous", statusValues()),
            Map.entry("status.new", statusValues()),
            Map.entry("budget.change_type", Set.of("increase", "decrease", "set", "waived", "unknown")),
            Map.entry("budget.value_band", Set.of("none", "zero", "low", "medium", "high", "unknown")),
            Map.entry("payment.method", Set.of("credito", "debito", "dinheiro", "pix", "outro", "unknown")),
            Map.entry("payment.category", Set.of("taxa_orcamento", "servicos", "unknown")),
            Map.entry("backup.type", Set.of("full", "diff", "incremental", "wal", "logical", "unknown")),
            Map.entry("duration.band", Set.of("lt_1s", "1s_10s", "10s_60s", "gt_60s", "unknown")),
            Map.entry("deployment.environment", Set.of("local", "qa", "production", "test", "staging", "unknown")),
            Map.entry("deployment.service", Set.of("api", "gateway", "frontend", "observability", "workshop-api", "unknown")),
            Map.entry("retry.policy", Set.of("none", "immediate", "backoff", "pending", "retrying", "exhausted", "unknown")));

    private ObservabilityEventValidator() {
    }

    public static void validate(ObservabilityEventContract contract, Map<String, String> attributes) {
        if (contract == null) {
            throw new IllegalArgumentException("event contract must not be null");
        }
        if (attributes == null) {
            throw new IllegalArgumentException("event attributes must not be null");
        }
        if (!EVENT_NAME.matcher(contract.name()).matches()) {
            throw new IllegalArgumentException("event name does not follow the stable naming contract");
        }
        if (attributes.size() > MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("event contains too many attributes");
        }

        for (String required : contract.requiredAttributes()) {
            if (!attributes.containsKey(required) || attributes.get(required) == null
                    || attributes.get(required).isBlank()) {
                throw new IllegalArgumentException("missing required event attribute");
            }
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            validateAttribute(contract, entry.getKey(), entry.getValue());
        }
    }

    private static void validateAttribute(
            ObservabilityEventContract contract,
            String name,
            String value) {
        if (name == null || name.length() > MAX_ATTRIBUTE_KEY_LENGTH
                || !ATTRIBUTE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("event attribute name is invalid");
        }
        if (!contract.allowedAttributes().contains(name)) {
            throw new IllegalArgumentException("event attribute is not in the contract");
        }
        if (containsProhibitedSegment(name)) {
            throw new IllegalArgumentException("event attribute is prohibited");
        }
        if (value == null || !NORMALIZED_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("event attribute value must be a bounded normalized token");
        }
        Set<String> allowedValues = LOW_CARDINALITY_VALUES.get(name);
        if (allowedValues != null && !allowedValues.contains(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("event attribute value is outside the low-cardinality vocabulary");
        }
    }

    private static boolean containsProhibitedSegment(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace('-', '_');
        for (String segment : normalized.split("\\.")) {
            if (PROHIBITED_SEGMENTS.contains(segment) || segment.equals("id") || segment.endsWith("_id")) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> statusValues() {
        return Set.of(
                "novo",
                "em_andamento",
                "aguardando",
                "entregue",
                "descartado",
                "aprovado",
                "nao_aprovado",
                "pronto",
                "none",
                "unknown");
    }
}
