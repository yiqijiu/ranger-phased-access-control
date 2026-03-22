package com.ranger.governance.doris;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.ranger.governance.common.protocol.GovernanceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PhasedDorisAuthorizerTest {
    private static final DecisionRequest REQUEST = new DecisionRequest(
            "etl_finance_daily_01",
            "zhangsan",
            EngineType.DORIS,
            "select 1",
            "127.0.0.1",
            "q1"
    );

    @Test
    void shouldFailOpenWhenGovernanceThrows() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        governanceClient.decideException = new RuntimeException("governance unavailable");
        int[] rangerCalls = new int[]{0};
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(governanceClient, request -> rangerCalls[0]++, true);

        Assertions.assertDoesNotThrow(() -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals(0, rangerCalls[0]);
        Assertions.assertEquals(0, governanceClient.msgFailCalls);
    }

    @Test
    void shouldFailOpenWhenGovernanceResponseInvalid() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        governanceClient.decideResponse = new DecisionResponse(500, "trace-1", null);
        int[] rangerCalls = new int[]{0};
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(governanceClient, request -> rangerCalls[0]++, true);

        Assertions.assertDoesNotThrow(() -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals(0, rangerCalls[0]);
    }

    @Test
    void shouldThrowWhenBlockAction() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        governanceClient.decideResponse = response(ActionType.BLOCK, "blocked");
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(governanceClient, request -> {
        }, true);

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals("blocked", ex.getMessage());
    }

    @Test
    void shouldBypassWhenWarnOrBypassAction() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        int[] rangerCalls = new int[]{0};
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(governanceClient, request -> rangerCalls[0]++, true);

        governanceClient.decideResponse = response(ActionType.BYPASS, "");
        Assertions.assertDoesNotThrow(() -> authorizer.checkPrivileges(REQUEST));
        governanceClient.decideResponse = response(ActionType.WARN, "warn");
        Assertions.assertDoesNotThrow(() -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals(0, rangerCalls[0]);
    }

    @Test
    void shouldRethrowAndCallbackWhenStrictCheckFailure() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        governanceClient.decideResponse = response(ActionType.CHECK, "");
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(
                governanceClient,
                request -> {
                    throw new RuntimeException("ranger deny");
                },
                true
        );

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals("ranger deny", ex.getMessage());
        Assertions.assertEquals(1, governanceClient.msgFailCalls);
        Assertions.assertEquals("ranger deny", governanceClient.lastRangerError);
    }

    @Test
    void shouldSwallowAndCallbackWhenObserveCheckFailure() {
        StubGovernanceClient governanceClient = new StubGovernanceClient();
        governanceClient.decideResponse = response(ActionType.CHECK, "");
        PhasedDorisAuthorizer authorizer = new PhasedDorisAuthorizer(
                governanceClient,
                request -> {
                    throw new RuntimeException("ranger deny");
                },
                false
        );

        Assertions.assertDoesNotThrow(() -> authorizer.checkPrivileges(REQUEST));
        Assertions.assertEquals(1, governanceClient.msgFailCalls);
        Assertions.assertEquals("ranger deny", governanceClient.lastRangerError);
    }

    private static DecisionResponse response(ActionType actionType, String msg) {
        return new DecisionResponse(200, "trace-1", new DecisionData(actionType, msg, false, REQUEST.getQueryId()));
    }

    private static class StubGovernanceClient implements GovernanceClient {
        private DecisionResponse decideResponse;
        private RuntimeException decideException;
        private int msgFailCalls;
        private String lastRangerError;

        @Override
        public DecisionResponse decide(DecisionRequest request) {
            if (decideException != null) {
                throw decideException;
            }
            return decideResponse;
        }

        @Override
        public void msgFail(DecisionRequest request, String rangerErrorMessage) {
            msgFailCalls++;
            lastRangerError = rangerErrorMessage;
        }
    }
}
