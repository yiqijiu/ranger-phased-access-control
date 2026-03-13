package com.ranger.governance.server.service;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.server.cache.GovernancePolicyCache;

import java.util.UUID;

public class GovernanceDecisionService {
    private final GovernancePolicyCache cache;
    private final FeiYouNotifier notifier;

    public GovernanceDecisionService(GovernancePolicyCache cache, FeiYouNotifier notifier) {
        this.cache = cache;
        this.notifier = notifier;
    }

    public DecisionResponse decide(DecisionRequest request) {
        String traceId = "req-" + UUID.randomUUID();

        if (request.user() == null || cache.blockedUsers().contains(request.user())) {
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "任务被拦截：无主/黑户任务，请补充任务归属信息。", true, request.queryId()));
        }

        if (request.jobName() == null || request.jobName().isBlank()) {
            notifyAsync(request.user(), "任务缺少 JobName", "任务被拦截，原因：未设置 JobName，请参考 Wiki 整改。");
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "任务被拦截，原因：未设置 JobName，请参考 Wiki 整改。", true, request.queryId()));
        }

        if (!cache.jobNamePattern().matcher(request.jobName()).matches()) {
            notifyAsync(request.user(), "任务命名不规范", "任务被拦截，原因：JobName 不符合命名规范。请按规范整改。");
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.BLOCK, "任务被拦截，原因：JobName 不符合命名规范。", true, request.queryId()));
        }

        if (cache.migratedJobNames().contains(request.jobName())) {
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.CHECK, "已迁移任务，进入 Ranger 鉴权。", false, request.queryId()));
        }

        if (cache.warnOnlyJobNames().contains(request.jobName())) {
            notifyAsync(request.user(), "任务迁移提醒", "您的任务暂未接入权限管控，请在 Q4 前完成迁移。" );
            return new DecisionResponse(200, traceId,
                    new DecisionData(ActionType.WARN, "任务处于过渡期，提醒放行。", true, request.queryId()));
        }

        return new DecisionResponse(200, traceId,
                new DecisionData(ActionType.BYPASS, "存量任务豁免放行。", false, request.queryId()));
    }

    public void onRangerCheckFailed(DecisionRequest request, String rangerError) {
        notifyAsync(request.user(), "权限缺失告警", "Ranger 鉴权失败: " + rangerError + "，请联系治理团队补齐权限。");
    }

    private void notifyAsync(String receiver, String title, String content) {
        notifier.send(receiver, title, content);
    }
}
