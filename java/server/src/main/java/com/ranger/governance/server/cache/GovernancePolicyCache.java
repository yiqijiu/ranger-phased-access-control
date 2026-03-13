package com.ranger.governance.server.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class GovernancePolicyCache {
    private volatile Pattern jobNamePattern = Pattern.compile("^[a-z][a-z0-9_\\-]{2,63}$");
    private volatile Set<String> migratedJobNames = Set.of();
    private volatile Set<String> blockedUsers = Set.of("", "anonymous");
    private volatile Set<String> warnOnlyJobNames = Set.of();
    private volatile Instant lastRefreshAt = Instant.now();

    public boolean isExpired(Duration maxAge) {
        return Instant.now().isAfter(lastRefreshAt.plus(maxAge));
    }

    public void refresh(String jobNameRegex, Set<String> migrated, Set<String> blocked, Set<String> warnOnly) {
        this.jobNamePattern = Pattern.compile(jobNameRegex);
        this.migratedJobNames = Collections.unmodifiableSet(new HashSet<>(migrated));
        this.blockedUsers = Collections.unmodifiableSet(new HashSet<>(blocked));
        this.warnOnlyJobNames = Collections.unmodifiableSet(new HashSet<>(warnOnly));
        this.lastRefreshAt = Instant.now();
    }

    public Pattern jobNamePattern() { return jobNamePattern; }
    public Set<String> migratedJobNames() { return migratedJobNames; }
    public Set<String> blockedUsers() { return blockedUsers; }
    public Set<String> warnOnlyJobNames() { return warnOnlyJobNames; }
}
