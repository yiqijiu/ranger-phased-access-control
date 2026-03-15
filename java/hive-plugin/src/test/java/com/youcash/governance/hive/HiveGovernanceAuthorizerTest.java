package com.youcash.governance.hive;

import com.youcash.governance.common.model.ActionType;
import com.youcash.governance.common.model.DecisionData;
import com.youcash.governance.common.model.DecisionRequest;
import com.youcash.governance.common.model.DecisionResponse;
import com.youcash.governance.common.model.EngineType;
import com.youcash.governance.common.protocol.AccessControlException;
import com.youcash.governance.common.protocol.GovernanceClient;
import com.youcash.governance.common.protocol.RangerDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HiveGovernanceAuthorizerTest {

    @Test
    void shouldFailOpenWhenGovernanceThrows() {
        GovernanceClient client = new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                throw new RuntimeException("timeout");
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
            }
        };
        RangerDelegate ranger = request -> {
            throw new AssertionError("should not be called");
        };

        HiveGovernanceAuthorizer authorizer = new HiveGovernanceAuthorizer(client, ranger);
        authorizer.checkPrivileges(sampleRequest());
    }

    @Test
    void shouldBlockOnBlockAction() {
        GovernanceClient client = new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                return new DecisionResponse(200, "t",
                        new DecisionData(ActionType.BLOCK, "任务被拦截", true, "q"));
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
            }
        };

        HiveGovernanceAuthorizer authorizer = new HiveGovernanceAuthorizer(client, request -> {
        });
        Assertions.assertThrows(AccessControlException.class, () -> authorizer.checkPrivileges(sampleRequest()));
    }

    @Test
    void shouldCallRangerOnCheck() {
        GovernanceClient client = new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                return new DecisionResponse(200, "t",
                        new DecisionData(ActionType.CHECK, "", false, "q"));
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
            }
        };

        final boolean[] called = {false};
        RangerDelegate ranger = request -> called[0] = true;

        HiveGovernanceAuthorizer authorizer = new HiveGovernanceAuthorizer(client, ranger);
        authorizer.checkPrivileges(sampleRequest());
        Assertions.assertTrue(called[0]);
    }

    @Test
    void shouldNotBlockInObserveModeWhenRangerFails() {
        GovernanceClient client = new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                return new DecisionResponse(200, "t",
                        new DecisionData(ActionType.CHECK, "", false, "q"));
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
            }
        };

        RangerDelegate ranger = request -> {
            throw new RuntimeException("denied");
        };

        HiveGovernanceAuthorizer authorizer = new HiveGovernanceAuthorizer(client, ranger, false);
        authorizer.checkPrivileges(sampleRequest());
    }

    private DecisionRequest sampleRequest() {
        return new DecisionRequest("etl_finance_daily_01", "zhangsan", EngineType.HIVE_TEZ,
                "select * from finance.orders", "10.1.100.5", "q-1");
    }
}
