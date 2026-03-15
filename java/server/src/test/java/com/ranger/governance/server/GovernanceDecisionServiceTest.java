package com.ranger.governance.server;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.ranger.governance.server.cache.GovernancePolicyCache;
import com.ranger.governance.server.service.GovernanceDecisionService;
import com.ranger.governance.server.service.NoopMsgNotifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

class GovernanceDecisionServiceTest {
    private GovernanceDecisionService service;

    @BeforeEach
    void init() {
        GovernancePolicyCache cache = new GovernancePolicyCache();
        cache.refresh(
                "^[a-z][a-z0-9_\\-]{2,63}$",
                new HashSet<String>(Arrays.asList("etl_finance_daily_01")),
                new HashSet<String>(Arrays.asList("", "anonymous")),
                new HashSet<String>(Arrays.asList("etl_warn_job"))
        );
        service = new GovernanceDecisionService(cache, new NoopMsgNotifier());
    }

    @Test
    void shouldBlockWhenJobNameMissing() {
        DecisionResponse response = service.decide(new DecisionRequest("", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.BLOCK, response.getData().getActionType());
    }

    @Test
    void shouldCheckWhenMigrated() {
        DecisionResponse response = service.decide(new DecisionRequest("etl_finance_daily_01", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.CHECK, response.getData().getActionType());
    }

    @Test
    void shouldBypassForLegacy() {
        DecisionResponse response = service.decide(new DecisionRequest("etl_legacy_abc", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.BYPASS, response.getData().getActionType());
    }
}
