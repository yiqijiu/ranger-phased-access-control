package com.youcash.governance.spark;

import com.youcash.governance.common.model.DecisionRequest;

@FunctionalInterface
public interface SparkRangerChecker {
    void check(DecisionRequest request);
}
