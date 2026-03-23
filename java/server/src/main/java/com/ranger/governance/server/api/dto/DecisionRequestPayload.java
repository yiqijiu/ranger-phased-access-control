package com.ranger.governance.server.api.dto;

import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.EngineType;

public class DecisionRequestPayload {
    private String jobName;
    private String user;
    private EngineType engine;
    private String sql;
    private String clientIp;
    private String queryId;

    public DecisionRequest toModel() {
        EngineType resolvedEngine = engine == null ? EngineType.HIVE_TEZ : engine;
        return new DecisionRequest(jobName, user, resolvedEngine, sql, clientIp, queryId);
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public EngineType getEngine() {
        return engine;
    }

    public void setEngine(EngineType engine) {
        this.engine = engine;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
}
