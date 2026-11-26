package com.tproject.workshop.integration.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Cleans database tables before each integration test method and before {@code
 * SqlScriptsTestExecutionListener} (order 5000) executes {@code @Sql} setup scripts.
 */
public final class DatabaseCleanerTestExecutionListener extends AbstractTestExecutionListener {

    private static final int ORDER = 3000;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        JdbcTemplate jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
        new DatabaseCleaner(jdbcTemplate).clean();
    }
}
