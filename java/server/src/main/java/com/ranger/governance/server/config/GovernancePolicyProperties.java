package com.ranger.governance.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "governance.policy")
public class GovernancePolicyProperties {
    private String jobNameRegex = "^[a-z][a-z0-9_\\-]{2,63}$";
    private Set<String> blockedUsers = defaultBlockedUsers();

    public String getJobNameRegex() {
        return jobNameRegex;
    }

    public void setJobNameRegex(String jobNameRegex) {
        this.jobNameRegex = jobNameRegex;
    }

    public Set<String> getBlockedUsers() {
        return blockedUsers;
    }

    public void setBlockedUsers(Set<String> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    private Set<String> defaultBlockedUsers() {
        Set<String> defaults = new LinkedHashSet<String>();
        defaults.add("");
        defaults.add("anonymous");
        return defaults;
    }
}
