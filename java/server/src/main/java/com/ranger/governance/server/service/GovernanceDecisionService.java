package com.ranger.governance.server.service;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.server.config.GovernancePolicyProperties;
import com.ranger.governance.server.whitelist.TaskWhitelistLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GovernanceDecisionService {
    private static final String DEFAULT_JOB_NAME_REGEX = "^[a-z][a-z0-9_\\-]{2,63}$";
    private static final Logger LOG = LoggerFactory.getLogger(GovernanceDecisionService.class);

    private final TaskWhitelistLookup whitelistLookup;
    private final MsgNotifier notifier;
    private final Pattern jobNamePattern;
    private final Set<String> blockedUsers;

    @Autowired
    public GovernanceDecisionService(
            GovernancePolicyProperties policyProperties,
            TaskWhitelistLookup whitelistLookup,
            MsgNotifier notifier
    ) {
        this(
                whitelistLookup,
                notifier,
                Pattern.compile(policyProperties != null ? policyProperties.getJobNameRegex() : DEFAULT_JOB_NAME_REGEX),
                policyProperties != null ? policyProperties.getBlockedUsers() : Collections.<String>emptySet()
        );
    }

    public GovernanceDecisionService(
            TaskWhitelistLookup whitelistLookup,
            MsgNotifier notifier,
            Pattern jobNamePattern,
            Set<String> blockedUsers
    ) {
        this.whitelistLookup = whitelistLookup;
        this.notifier = notifier;
        this.jobNamePattern = jobNamePattern;
        this.blockedUsers = normalizeBlockedUsers(blockedUsers);
    }

    public DecisionResponse decide(DecisionRequest request) {
        String traceId = "req-" + UUID.randomUUID();
        String user = normalizeText(request.getUser());
        String jobName = normalizeText(request.getJobName());

        if (user.isEmpty() || blockedUsers.contains(user)) {
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "Blocked: user is missing or in blocked list.", true, request.getQueryId()));
        }

        if (jobName.isEmpty()) {
            notifyAsync(user, "Missing JobName", "Blocked because JobName is empty.");
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "Blocked: JobName is required.", true, request.getQueryId()));
        }

        if (!jobNamePattern.matcher(jobName).matches()) {
            notifyAsync(user, "Invalid JobName", "Blocked because JobName format is invalid.");
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "Blocked: JobName does not match policy format.", true, request.getQueryId()));
        }

        if (whitelistLookup.isWhitelisted(jobName)) {
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.CHECK, "Whitelisted task, continue with Ranger check.", false, request.getQueryId()));
        }

        return new DecisionResponse(200, traceId,
                new DecisionData(ActionType.BYPASS, "Legacy task not in whitelist, bypass Ranger check.", false, request.getQueryId()));
    }

    public void onRangerCheckFailed(DecisionRequest request, String rangerError) {
        notifyAsync(request.getUser(), "Ranger Check Failed", "Ranger authorization failed: " + rangerError);
    }

    private void notifyAsync(String receiver, String title, String content) {
        try {
            notifier.send(receiver, title, content);
        } catch (Exception ex) {
            LOG.error("notify failed but ignored, receiver={}, title={}", receiver, title, ex);
        }
    }

    private Set<String> normalizeBlockedUsers(Set<String> users) {
        Set<String> normalized = new HashSet<String>();
        normalized.add("");
        normalized.add("anonymous");
        if (users != null) {
            for (String user : users) {
                normalized.add(normalizeText(user));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
