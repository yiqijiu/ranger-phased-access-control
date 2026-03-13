package com.youcash.governance.hive;

import com.youcash.governance.common.model.ActionType;
import com.youcash.governance.common.model.DecisionData;
import com.youcash.governance.common.model.DecisionRequest;
import com.youcash.governance.common.model.DecisionResponse;
import com.youcash.governance.common.protocol.AccessControlException;
import com.youcash.governance.common.protocol.GovernanceClient;
import com.youcash.governance.common.protocol.RangerDelegate;

/**
 * Hive plugin side "dumb terminal" router.
 */
public class HiveGovernanceAuthorizer {
    private final GovernanceClient governanceClient;
    private final RangerDelegate rangerDelegate;
    private final boolean strictCheckFailure;

    public HiveGovernanceAuthorizer(GovernanceClient governanceClient, RangerDelegate rangerDelegate) {
        this(governanceClient, rangerDelegate, true);
    }

    public HiveGovernanceAuthorizer(
            GovernanceClient governanceClient,
            RangerDelegate rangerDelegate,
            boolean strictCheckFailure
    ) {
        this.governanceClient = governanceClient;
        this.rangerDelegate = rangerDelegate;
        this.strictCheckFailure = strictCheckFailure;
    }

    public void checkPrivileges(DecisionRequest request) {
        DecisionResponse response;
        try {
            response = governanceClient.decide(request);
        } catch (Exception ex) {
            return; // fail-open: BYPASS
        }

        if (!response.success() || response.data() == null) {
            return; // fail-open on non-200 or malformed payload
        }

        DecisionData data = response.data();
        ActionType action = data.actionType();
        if (action == null) {
            return;
        }

        switch (action) {
            case BLOCK -> throw new AccessControlException(data.msg());
            case BYPASS, WARN -> {
                return;
            }
            case CHECK -> {
                try {
                    rangerDelegate.checkPrivileges(request);
                } catch (RuntimeException rangerEx) {
                    try {
                        governanceClient.msgFail(request, rangerEx.getMessage());
                    } catch (Exception ignored) {
                        // swallow callback failure
                    }
                    if (strictCheckFailure) {
                        throw rangerEx;
                    }
                }
            }
        }
    }
}
