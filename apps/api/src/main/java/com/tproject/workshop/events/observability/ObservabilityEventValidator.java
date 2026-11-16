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
            "client_name",
            "cnpj",
            "cpf",
            "device_id",
            "email",
            "free_text",
            "message",
            "password",
            "payload",
            "payment_id",
            "phone",
            "query",
            "query_string",
            "secret",
            "span_id",
            "token",
            "trace_id",
            "user_id");

    /**
     * Every attribute has a closed vocabulary, so no request-supplied text
     * (CPF, phone, e-mail, API key, free text) can reach an event. A new
     * category is an explicit contract change.
     */
    private static final Map<String, Set<String>> CLOSED_VOCABULARIES = Map.ofEntries(
            Map.entry("actor.role", Set.of("admin", "service", "anonymous")),
            Map.entry("client.platform", Set.of("mobile", "web", "desktop", "server")),
            Map.entry("result", Set.of("ok", "failed", "skipped")),
            Map.entry("device.urgency", Set.of("urgent", "normal")),
            Map.entry("status.previous", statusValues()),
            Map.entry("status.new", statusValues()),
            Map.entry("change.origin", Set.of("status", "device_update", "urgency", "revision", "customer_contact")),
            Map.entry("budget.field", Set.of("budget_fee", "service_value")),
            Map.entry("budget.change_type", Set.of("set", "increase", "decrease", "cleared")),
            Map.entry("budget.value_band", valueBands()),
            Map.entry("payment.method", Set.of("credito", "debito", "dinheiro", "pix", "outro")),
            Map.entry("payment.category", Set.of("taxa_orcamento", "servicos")),
            Map.entry("payment.value_band", valueBands()),
            Map.entry("contact.made", Set.of("true", "false")),
            Map.entry("scope", Set.of("single", "all_by_client")),
            Map.entry("backup.type", Set.of("full", "diff")),
            Map.entry("duration.band", Set.of("lt_1m", "1m_10m", "10m_60m", "gt_60m")),
            Map.entry("deploy.service", Set.of("api", "gateway", "observability")),
            Map.entry("deploy.environment", Set.of("qa", "production")),
            Map.entry("reason.code", Set.of(
                    "canary_failed",
                    "telemetry_no_span",
                    "health_timeout",
                    "migration_approval_required",
                    "migration_ahead_of_image",
                    "backup_stale",
                    "backup_info_failed",
                    "wal_check_failed",
                    "flyway_history_failed",
                    "db_not_running",
                    "admin_request")));

    /** Attributes whose values are bounded by a strict format instead of a list. */
    private static final Map<String, Pattern> CLOSED_FORMATS = Map.of(
            "deployment.version", Pattern.compile("v\\d+\\.\\d+\\.\\d+(?:-[0-9a-z.]+)?|[0-9a-f]{7,40}"));

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
        String normalized = value.toLowerCase(Locale.ROOT);
        Set<String> allowedValues = CLOSED_VOCABULARIES.get(name);
        Pattern format = CLOSED_FORMATS.get(name);
        if (allowedValues == null && format == null) {
            throw new IllegalArgumentException("event attribute has no closed vocabulary");
        }
        if (allowedValues != null && !allowedValues.contains(normalized)) {
            throw new IllegalArgumentException("event attribute value is outside the closed vocabulary");
        }
        if (format != null && !format.matcher(normalized).matches()) {
            throw new IllegalArgumentException("event attribute value does not match its closed format");
        }
    }

    /** True when the attribute name has a closed vocabulary or a strict format. */
    public static boolean hasClosedVocabulary(String name) {
        return CLOSED_VOCABULARIES.containsKey(name) || CLOSED_FORMATS.containsKey(name);
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
                "pronto");
    }

    private static Set<String> valueBands() {
        return Set.of("zero", "low", "medium", "high");
    }
}
