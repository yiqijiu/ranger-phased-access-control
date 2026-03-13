package com.youcash.governance.common.model;

public record DecisionData(
        ActionType actionType,
        String msg,
        boolean alertTriggered,
        String queryId
) {
}
