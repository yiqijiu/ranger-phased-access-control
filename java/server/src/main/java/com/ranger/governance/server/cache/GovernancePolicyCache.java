package com.ranger.governance.server.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class GovernancePolicyCache {
    private volatile Pattern jobNamePattern = Pattern.compile("^[a-z][a-z0-9_\\-]{2,63}$");
    private volatile Set<String> migratedJobNames = Collections.emptySet();
    private volatile Set<String> blockedUsers = new HashSet<String>();
    private volatile Set<String> warnOnlyJobNames = Collections.emptySet();
    private volatile Instant lastRefreshAt = Instant.now();

    public GovernancePolicyCache() {
        blockedUsers.add("");
        blockedUsers.add("anonymous");
        blockedUsers = Collections.unmodifiableSet(blockedUsers);
    }

    public boolean isExpired(Duration maxAge) {
        return Instant.now().isAfter(lastRefreshAt.plus(maxAge));
    }

    public void refresh(String jobNameRegex, Set<String> migrated, Set<String> blocked, Set<String> warnOnly) {
        this.jobNamePattern = Pattern.compile(jobNameRegex);
        this.migratedJobNames = Collections.unmodifiableSet(new HashSet<String>(migrated));
        this.blockedUsers = Collections.unmodifiableSet(new HashSet<String>(blocked));
        this.warnOnlyJobNames = Collections.unmodifiableSet(new HashSet<String>(warnOnly));
        this.lastRefreshAt = Instant.now();
    }

    public Pattern getJobNamePattern() {
        return jobNamePattern;
    }

    public Set<String> getMigratedJobNames() {
        return migratedJobNames;
    }

    public Set<String> getBlockedUsers() {
        return blockedUsers;
    }

    public Set<String> getWarnOnlyJobNames() {
        return warnOnlyJobNames;
    }
}
