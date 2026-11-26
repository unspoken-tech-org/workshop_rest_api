package com.tproject.workshop.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tproject.workshop.exception.IdempotencyKeyReusedException;
import com.tproject.workshop.exception.IdempotencyRequestInProgressException;
import com.tproject.workshop.integration.support.TestClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration IN_PROGRESS = Duration.ofSeconds(60);
    private static final Instant NOW = TestClock.FIXED.instant();

    @Mock private IdempotencyRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, TestClock.FIXED, TTL, IN_PROGRESS);
    }

    @Test
    void shouldProceed_whenKeyIsFree() {
        when(repository.claim("s", "k", "h", NOW, NOW.plus(IN_PROGRESS)))
                .thenReturn(Optional.of(7L));

        assertThat(service.begin("s", "k", "h")).isEqualTo(new IdempotencyService.Proceed(7L));
    }

    @Test
    void shouldReplay_whenCompletedWithSameHash() {
        var stored = new StoredResponse(201, "application/json", "{}");
        when(repository.claim(eq("s"), eq("k"), eq("h"), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.find("s", "k"))
                .thenReturn(Optional.of(new IdempotencyRepository.Row(1, "h", true, stored)));

        assertThat(service.begin("s", "k", "h")).isEqualTo(new IdempotencyService.Replay(stored));
    }

    @Test
    void shouldThrowReused_whenHashDiffers() {
        when(repository.claim(eq("s"), eq("k"), eq("h2"), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.find("s", "k"))
                .thenReturn(Optional.of(new IdempotencyRepository.Row(1, "h", true, null)));

        assertThatThrownBy(() -> service.begin("s", "k", "h2"))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void shouldThrowInProgress_whenFirstRequestIsRunning() {
        when(repository.claim(eq("s"), eq("k"), eq("h"), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.find("s", "k"))
                .thenReturn(Optional.of(new IdempotencyRepository.Row(1, "h", false, null)));

        assertThatThrownBy(() -> service.begin("s", "k", "h"))
                .isInstanceOf(IdempotencyRequestInProgressException.class);
    }

    @Test
    void shouldStoreResponseWithTtl_whenCompleted() {
        var stored = new StoredResponse(201, "application/json", "{}");

        service.complete(9L, stored);

        verify(repository).complete(9L, stored, NOW.plus(TTL));
    }
}
