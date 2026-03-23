package com.ranger.governance.server.whitelist;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class TaskWhitelistServiceIntegrationTest {
    @Autowired
    private TaskWhitelistService service;

    @Autowired
    private TaskWhitelistRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateAndResolveWhitelist() {
        service.saveOrEnable("etl_finance_daily_01", "daily job");
        Assertions.assertTrue(service.isWhitelisted("etl_finance_daily_01"));
        Assertions.assertEquals(1, service.list().size());
    }

    @Test
    void shouldDisableWhitelistEntry() {
        TaskWhitelistItem item = service.saveOrEnable("etl_finance_daily_01", "daily job");
        service.update(item.getId(), "disabled", false);
        Assertions.assertFalse(service.isWhitelisted("etl_finance_daily_01"));
    }

    @Test
    void shouldRejectInvalidTaskName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.saveOrEnable("1invalid_job", ""));
    }
}
