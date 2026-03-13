package com.youcash.governance.doris;

import com.youcash.governance.common.model.DecisionRequest;

@FunctionalInterface
public interface DorisRangerChecker {
    void check(DecisionRequest request);
}
