package com.ranger.governance.server.whitelist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskWhitelistRepository extends JpaRepository<TaskWhitelistEntity, Long> {
    Optional<TaskWhitelistEntity> findByTaskNameIgnoreCase(String taskName);

    boolean existsByTaskNameIgnoreCaseAndEnabledTrue(String taskName);

    List<TaskWhitelistEntity> findAllByOrderByUpdatedAtDesc();
}
