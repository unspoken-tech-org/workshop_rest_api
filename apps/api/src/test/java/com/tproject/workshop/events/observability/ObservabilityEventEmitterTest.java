package com.tproject.workshop.events.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(ObservabilityEventEmitterTest.Config.class)
class ObservabilityEventEmitterTest {

    @Configuration
    @EnableTransactionManagement
    @Import({ObservabilityEventRecorder.class, ObservabilityEventEmitter.class})
    static class Config {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }
    }

    /** Transaction manager without a resource: enough to drive commit and rollback callbacks. */
    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Autowired
    private ObservabilityEventRecorder recorder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger eventsLogger = (Logger) LoggerFactory.getLogger(ObservabilityEventEmitter.EVENTS_LOGGER);

    @BeforeEach
    void attachAppender() {
        appender.start();
        eventsLogger.addAppender(appender);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "subject", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void detachAppender() {
        eventsLogger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldEmitStructuredEventAfterCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            recorder.record(ObservabilityEventCatalog.PAYMENT_CREATED, Map.of(
                    "payment.method", "pix",
                    "payment.category", "servicos",
                    "payment.value_band", ValueBand.of(new BigDecimal("150.00"))));
            assertThat(appender.list).as("nothing is written before the commit").isEmpty();
        });

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.getFirst();
        assertThat(logged.getLevel()).isEqualTo(Level.INFO);
        assertThat(logged.getArgumentArray()).singleElement().isInstanceOf(StructuredArgument.class);
        assertThat(logged.getFormattedMessage())
                .contains("event.name=workshop.payment.created")
                .contains("event.version=1")
                .contains("actor.role=admin")
                .contains("payment.value_band=medium");
    }

    @Test
    void shouldNotEmitWhenTheTransactionRollsBack() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            recorder.record(ObservabilityEventCatalog.DEVICE_CREATED, Map.of("device.urgency", "normal"));
            status.setRollbackOnly();
        });

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldDropInvalidEventsWithoutFailingTheTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                recorder.record(ObservabilityEventCatalog.DEVICE_CREATED, Map.of("device.urgency", "12345678909")));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldNotEmitOutsideATransaction() {
        recorder.record(ObservabilityEventCatalog.DEVICE_CREATED, Map.of("device.urgency", "normal"));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void shouldResolveActorRoleFromAuthorities() {
        assertThat(ObservabilityEventRecorder.currentActorRole()).isEqualTo("admin");

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "subject", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
        assertThat(ObservabilityEventRecorder.currentActorRole()).isEqualTo("service");

        SecurityContextHolder.clearContext();
        assertThat(ObservabilityEventRecorder.currentActorRole()).isEqualTo("anonymous");
    }

    @Test
    void shouldBandValuesWithoutExposingThem() {
        assertThat(ValueBand.of(null)).isEqualTo("zero");
        assertThat(ValueBand.of(BigDecimal.ZERO)).isEqualTo("zero");
        assertThat(ValueBand.of(new BigDecimal("99.99"))).isEqualTo("low");
        assertThat(ValueBand.of(new BigDecimal("100"))).isEqualTo("medium");
        assertThat(ValueBand.of(new BigDecimal("500"))).isEqualTo("high");
    }

    @Test
    void shouldMapTurnaroundToBands() {
        Instant entry = Instant.parse("2026-11-01T12:00:00Z");

        assertThat(TurnaroundBand.of(null, entry)).isEqualTo("lt_1d");
        assertThat(TurnaroundBand.of(entry, entry.plus(Duration.ofHours(23)))).isEqualTo("lt_1d");
        assertThat(TurnaroundBand.of(entry, entry.plus(Duration.ofHours(24)))).isEqualTo("1d_3d");
        assertThat(TurnaroundBand.of(entry, entry.plus(Duration.ofDays(3)))).isEqualTo("3d_7d");
        assertThat(TurnaroundBand.of(entry, entry.plus(Duration.ofDays(7)))).isEqualTo("gt_7d");
    }
}
