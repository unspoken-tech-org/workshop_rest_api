package com.tproject.workshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.repository.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

@Sql({"/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
public class ApiKeyTokenVersionIT extends AbstractIntegrationTest {

    private static final long API_KEY_ID = 1L;

    @Autowired private ApiKeyRepository apiKeyRepository;

    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("incrementAndGetTokenVersion returns 1 then 2 via native UPDATE ... RETURNING")
    public void shouldIncrementAndReturnNewTokenVersion() {
        assertThat(currentTokenVersion()).isZero();

        Long first =
                transactionTemplate.execute(
                        status -> apiKeyRepository.incrementAndGetTokenVersion(API_KEY_ID));
        Long second =
                transactionTemplate.execute(
                        status -> apiKeyRepository.incrementAndGetTokenVersion(API_KEY_ID));

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
        assertThat(currentTokenVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("incrementAndGetTokenVersion requires a caller transaction")
    public void shouldRequireTransaction() {
        assertThatThrownBy(() -> apiKeyRepository.incrementAndGetTokenVersion(API_KEY_ID))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(currentTokenVersion()).isZero();
    }

    @Test
    @DisplayName("Saving a stale ApiKey entity never rolls token_version back")
    public void shouldNotOverwriteTokenVersionFromStaleEntity() {
        ApiKey stale = apiKeyRepository.findById(API_KEY_ID).orElseThrow();
        assertThat(stale.getTokenVersion()).isZero();

        transactionTemplate.execute(
                status -> apiKeyRepository.incrementAndGetTokenVersion(API_KEY_ID));

        stale.setDescription("touched");
        apiKeyRepository.save(stale);

        assertThat(currentTokenVersion()).isEqualTo(1L);
    }

    private long currentTokenVersion() {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM api_keys WHERE id = ?", Long.class, API_KEY_ID);
    }
}
