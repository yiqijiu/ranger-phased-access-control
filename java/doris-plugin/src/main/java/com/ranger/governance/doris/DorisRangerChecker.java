package com.ranger.governance.doris;

import com.ranger.governance.common.model.DecisionRequest;

@FunctionalInterface
public interface DorisRangerChecker {
    void check(DecisionRequest request);
}
