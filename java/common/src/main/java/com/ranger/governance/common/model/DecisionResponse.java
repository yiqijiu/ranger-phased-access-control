package com.ranger.governance.common.model;

public class DecisionResponse {
    private final int code;
    private final String traceId;
    private final DecisionData data;

    public DecisionResponse(int code, String traceId, DecisionData data) {
        this.code = code;
        this.traceId = traceId;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public String getTraceId() {
        return traceId;
    }

    public DecisionData getData() {
        return data;
    }


    public boolean success() {
        return code == 200;
    }
}
