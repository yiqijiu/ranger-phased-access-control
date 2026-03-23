package com.ranger.governance.server.api.dto;

import javax.validation.constraints.NotNull;

public class TaskWhitelistUpdateRequest {
    private String description;

    @NotNull
    private Boolean enabled;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
