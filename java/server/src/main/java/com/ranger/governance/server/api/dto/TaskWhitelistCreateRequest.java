package com.ranger.governance.server.api.dto;

import javax.validation.constraints.NotBlank;

public class TaskWhitelistCreateRequest {
    @NotBlank
    private String taskName;

    private String description;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
