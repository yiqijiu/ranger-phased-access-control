package com.youcash.governance.doris;

import com.youcash.governance.common.model.ActionType;
import com.youcash.governance.common.model.DecisionData;
import com.youcash.governance.common.model.DecisionRequest;
import com.youcash.governance.common.model.DecisionResponse;
import com.youcash.governance.common.protocol.GovernanceClient;

/**
 * Doris plugin router using unified governance semantics.
 */
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
            return; // fail-open
        }

        if (response == null || !response.success() || response.data() == null || response.data().actionType() == null) {
            return;
        }

        DecisionData data = response.data();
        switch (data.actionType()) {
            case BLOCK -> throw new RuntimeException(data.msg());
            case BYPASS, WARN -> {
                return;
            }
            case CHECK -> {
                try {
                    rangerChecker.check(request);
                } catch (RuntimeException rangerEx) {
                    try {
                        governanceClient.msgFail(request, rangerEx.getMessage());
                    } catch (Exception ignored) {
                    }
                    if (strictCheckFailure) {
                        throw rangerEx;
                    }
                }
            }
            default -> {
                return;
            }
        }
    }
}
