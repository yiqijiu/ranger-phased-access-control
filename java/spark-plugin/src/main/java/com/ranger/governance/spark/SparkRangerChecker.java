package com.ranger.governance.spark;

import com.ranger.governance.common.model.DecisionRequest;

@FunctionalInterface
public interface SparkRangerChecker {
    void check(DecisionRequest request);
}
