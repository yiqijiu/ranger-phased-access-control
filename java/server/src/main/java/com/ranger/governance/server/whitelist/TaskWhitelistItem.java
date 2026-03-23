package com.ranger.governance.server.whitelist;

import java.time.Instant;

public class TaskWhitelistItem {
    private final Long id;
    private final String taskName;
    private final String description;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TaskWhitelistItem(
            Long id,
            String taskName,
            String description,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
