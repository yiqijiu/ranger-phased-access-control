package com.ranger.governance.common.model;

public record DecisionRequest(
        String jobName,
        String user,
        EngineType engine,
        String sql,
        String clientIp,
        String queryId
) {
}
