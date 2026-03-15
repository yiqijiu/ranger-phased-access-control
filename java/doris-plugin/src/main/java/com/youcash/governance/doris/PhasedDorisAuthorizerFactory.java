package com.youcash.governance.doris;

import com.youcash.governance.common.protocol.GovernanceClient;

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
