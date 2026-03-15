package com.ranger.governance.common.model;

public class DecisionRequest {
    private final String jobName;
    private final String user;
    private final EngineType engine;
    private final String sql;
    private final String clientIp;
    private final String queryId;

    public DecisionRequest(String jobName, String user, EngineType engine, String sql, String clientIp, String queryId) {
        this.jobName = jobName;
        this.user = user;
        this.engine = engine;
        this.sql = sql;
        this.clientIp = clientIp;
        this.queryId = queryId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getUser() {
        return user;
    }

    public EngineType getEngine() {
        return engine;
    }

    public String getSql() {
        return sql;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getQueryId() {
        return queryId;
    }
}
