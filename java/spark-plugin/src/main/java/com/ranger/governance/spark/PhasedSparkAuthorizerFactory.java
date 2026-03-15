package com.ranger.governance.spark;

import com.ranger.governance.common.protocol.GovernanceClient;

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
