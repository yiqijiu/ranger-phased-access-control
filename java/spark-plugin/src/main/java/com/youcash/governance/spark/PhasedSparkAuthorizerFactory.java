package com.youcash.governance.spark;

import com.youcash.governance.common.protocol.GovernanceClient;

/**
 * Spark-side factory to build phased authorizer.
 */
public class PhasedSparkAuthorizerFactory {
    public PhasedSparkAuthorizer create(
            GovernanceClient governanceClient,
            SparkRangerChecker rangerChecker,
            boolean strictCheckFailure
    ) {
        return new PhasedSparkAuthorizer(governanceClient, rangerChecker, strictCheckFailure);
    }
}
