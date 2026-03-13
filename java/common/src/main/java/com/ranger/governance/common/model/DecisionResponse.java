package com.ranger.governance.common.model;

public record DecisionResponse(
        int code,
        String traceId,
        DecisionData data
) {
    public boolean success() {
        return code == 200;
    }
}
