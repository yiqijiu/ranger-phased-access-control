package com.ranger.governance.server;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.ranger.governance.server.service.GovernanceDecisionService;
import com.ranger.governance.server.service.MsgNotifier;
import com.ranger.governance.server.whitelist.TaskWhitelistLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class GovernanceDecisionServiceTest {
    private GovernanceDecisionService service;

    @BeforeEach
    void init() {
        TaskWhitelistLookup lookup = taskName -> "etl_finance_daily_01".equals(taskName);
        MsgNotifier notifier = (receiver, title, content) -> {
        };
        service = new GovernanceDecisionService(
                lookup,
                notifier,
                Pattern.compile("^[a-z][a-z0-9_\\-]{2,63}$"),
                blockedUsers("", "anonymous")
        );
    }

    @Test
    void shouldBlockWhenJobNameMissing() {
        DecisionResponse response = service.decide(new DecisionRequest("", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.BLOCK, response.getData().getActionType());
    }

    @Test
    void shouldCheckWhenWhitelisted() {
        DecisionResponse response = service.decide(new DecisionRequest("etl_finance_daily_01", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.CHECK, response.getData().getActionType());
    }

    @Test
    void shouldBypassForLegacyTask() {
        DecisionResponse response = service.decide(new DecisionRequest("etl_legacy_abc", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.BYPASS, response.getData().getActionType());
    }

    @Test
    void shouldBlockWhenUserInBlockedList() {
        DecisionResponse response = service.decide(new DecisionRequest("etl_legacy_abc", "anonymous", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1"));
        Assertions.assertEquals(ActionType.BLOCK, response.getData().getActionType());
    }

    @Test
    void shouldIgnoreNotifierError() {
        GovernanceDecisionService serviceWithBrokenNotifier = new GovernanceDecisionService(
                taskName -> false,
                (receiver, title, content) -> {
                    throw new RuntimeException("notify unavailable");
                },
                Pattern.compile("^[a-z][a-z0-9_\\-]{2,63}$"),
                blockedUsers("", "anonymous")
        );

        DecisionResponse response = serviceWithBrokenNotifier.decide(
                new DecisionRequest("", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", "q1")
        );
        Assertions.assertEquals(ActionType.BLOCK, response.getData().getActionType());
    }

    private Set<String> blockedUsers(String... users) {
        return new HashSet<String>(Arrays.asList(users));
    }
}
