package com.ranger.governance.server.api.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class MsgFailPayload {
    @Valid
    @NotNull
    private DecisionRequestPayload request;

    @NotBlank
    private String rangerError;

    public DecisionRequestPayload getRequest() {
        return request;
    }

    public void setRequest(DecisionRequestPayload request) {
        this.request = request;
    }

    public String getRangerError() {
        return rangerError;
    }

    public void setRangerError(String rangerError) {
        this.rangerError = rangerError;
    }
}
