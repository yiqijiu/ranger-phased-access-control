package com.ranger.governance.doris;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.protocol.GovernanceClient;

public class PhasedDorisAuthorizer {
    private final GovernanceClient governanceClient;
    private final DorisRangerChecker rangerChecker;
    private final boolean strictCheckFailure;

    public PhasedDorisAuthorizer(
            GovernanceClient governanceClient,
            DorisRangerChecker rangerChecker,
            boolean strictCheckFailure
    ) {
        this.governanceClient = governanceClient;
        this.rangerChecker = rangerChecker;
        this.strictCheckFailure = strictCheckFailure;
    }

    public void checkPrivileges(DecisionRequest request) {
        DecisionResponse response;
        try {
            response = governanceClient.decide(request);
        } catch (Exception ex) {
            return;
        }

        if (response == null || !response.success() || response.getData() == null || response.getData().getActionType() == null) {
            return;
        }

        DecisionData data = response.getData();
        ActionType action = data.getActionType();
        if (action == ActionType.BLOCK) {
            throw new RuntimeException(data.getMsg());
        }
        if (action == ActionType.BYPASS || action == ActionType.WARN) {
            return;
        }
        if (action == ActionType.CHECK) {
            try {
                rangerChecker.check(request);
            } catch (Exception rangerEx) {
                try {
                    governanceClient.msgFail(request, rangerEx.getMessage());
                } catch (Exception ignored) {
                    // ignore callback error
                }
                if (strictCheckFailure) {
                    throw wrapAsRuntime(rangerEx);
                }
            }
        }
    }

    private RuntimeException wrapAsRuntime(Exception ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new RuntimeException(ex);
    }
}
