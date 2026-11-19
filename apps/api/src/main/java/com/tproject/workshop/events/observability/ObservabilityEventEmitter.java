package com.tproject.workshop.events.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static net.logstash.logback.argument.StructuredArguments.entries;

/**
 * Writes committed events as structured JSON logs; Alloy ships them to Loki
 * ({@code otel.logs.exporter=none}). ERROR is written as WARN because ERROR
 * logs from the application count in the deploy canary.
 */
@Component
public class ObservabilityEventEmitter {

    public static final String EVENTS_LOGGER = "workshop.events";

    private static final Logger EVENTS = LoggerFactory.getLogger(EVENTS_LOGGER);
    private static final Logger log = LoggerFactory.getLogger(ObservabilityEventEmitter.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(PendingObservabilityEvent pending) {
        try {
            emit(new ObservabilityEvent(pending.contract(), pending.occurredAt(), pending.attributes()));
        } catch (RuntimeException e) {
            log.warn("Observability event dropped: {}", pending.contract().name());
        }
    }

    void emit(ObservabilityEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event.name", event.name());
        fields.put("event.version", event.version());
        fields.putAll(new TreeMap<>(event.attributes()));

        if (event.severity() == EventSeverity.INFO) {
            EVENTS.info("observability event {}", entries(fields));
        } else {
            EVENTS.warn("observability event {}", entries(fields));
        }
    }
}
