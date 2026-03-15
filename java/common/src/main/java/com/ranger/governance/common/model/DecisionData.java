package com.ranger.governance.common.model;

public class DecisionData {
    private final ActionType actionType;
    private final String msg;
    private final boolean alertTriggered;
    private final String queryId;

    public DecisionData(ActionType actionType, String msg, boolean alertTriggered, String queryId) {
        this.actionType = actionType;
        this.msg = msg;
        this.alertTriggered = alertTriggered;
        this.queryId = queryId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public String getQueryId() {
        return queryId;
    }
}
