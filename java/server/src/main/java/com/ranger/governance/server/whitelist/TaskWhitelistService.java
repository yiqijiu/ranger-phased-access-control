package com.ranger.governance.server.whitelist;

import com.ranger.governance.server.config.GovernancePolicyProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskWhitelistService implements TaskWhitelistLookup {
    private final TaskWhitelistRepository repository;
    private final Pattern jobNamePattern;

    public TaskWhitelistService(TaskWhitelistRepository repository, GovernancePolicyProperties properties) {
        this.repository = repository;
        this.jobNamePattern = Pattern.compile(properties.getJobNameRegex());
    }

    @Transactional(readOnly = true)
    public List<TaskWhitelistItem> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public TaskWhitelistItem saveOrEnable(String taskName, String description) {
        String normalizedTaskName = normalizeTaskName(taskName);
        String normalizedDescription = normalizeDescription(description);

        TaskWhitelistEntity entity = repository.findByTaskNameIgnoreCase(normalizedTaskName)
                .orElseGet(TaskWhitelistEntity::new);
        entity.setTaskName(normalizedTaskName);
        entity.setDescription(normalizedDescription);
        entity.setEnabled(true);
        return toItem(repository.save(entity));
    }

    public TaskWhitelistItem update(Long id, String description, boolean enabled) {
        TaskWhitelistEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Whitelist item does not exist: " + id));
        entity.setDescription(normalizeDescription(description));
        entity.setEnabled(enabled);
        return toItem(repository.save(entity));
    }

    public TaskWhitelistItem toggle(Long id) {
        TaskWhitelistEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Whitelist item does not exist: " + id));
        entity.setEnabled(!entity.isEnabled());
        return toItem(repository.save(entity));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Whitelist item does not exist: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWhitelisted(String taskName) {
        String normalizedTaskName = normalizeNullableTaskName(taskName);
        if (normalizedTaskName.isEmpty()) {
            return false;
        }
        return repository.existsByTaskNameIgnoreCaseAndEnabledTrue(normalizedTaskName);
    }

    private String normalizeTaskName(String taskName) {
        String normalized = normalizeNullableTaskName(taskName);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("taskName cannot be empty.");
        }
        if (!jobNamePattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException("taskName does not match policy regex.");
        }
        return normalized;
    }

    private String normalizeNullableTaskName(String taskName) {
        return taskName == null ? "" : taskName.trim().toLowerCase();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private TaskWhitelistItem toItem(TaskWhitelistEntity entity) {
        return new TaskWhitelistItem(
                entity.getId(),
                entity.getTaskName(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
