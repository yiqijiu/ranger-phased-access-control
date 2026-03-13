package com.ranger.governance.doris;

import com.ranger.governance.common.protocol.GovernanceClient;

/**
 * Doris-side factory to build phased authorizer.
 */
public class PhasedDorisAuthorizerFactory {
    public PhasedDorisAuthorizer create(
            GovernanceClient governanceClient,
            DorisRangerChecker rangerChecker,
            boolean strictCheckFailure
    ) {
        return new PhasedDorisAuthorizer(governanceClient, rangerChecker, strictCheckFailure);
    }
}
