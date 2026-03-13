package com.ranger.governance.hive;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.ranger.governance.common.protocol.GovernanceClient;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClientFactory;
import org.apache.hadoop.hive.ql.metadata.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ranger proxy authorizer for phased governance rollout.
 *
 * <p>Key point: this class extends {@link RangerHiveAuthorizer} so CHECK can inherit
 * native Ranger behavior via super.checkPrivileges(...).</p>
 */
public class PhasedRangerAuthorizer extends RangerHiveAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(PhasedRangerAuthorizer.class);

    private final GovernanceClient governanceClient;
    private final boolean strictCheckFailure;

    public PhasedRangerAuthorizer(
            HiveMetaStoreClientFactory metastoreClientFactory,
            HiveConf hiveConf,
            HiveAuthenticationProvider hiveAuthenticator,
            HiveAuthzSessionContext sessionContext,
            GovernanceClient governanceClient,
            boolean strictCheckFailure
    ) throws SemanticException {
        super(metastoreClientFactory, hiveConf, hiveAuthenticator, sessionContext);
        this.governanceClient = governanceClient;
        this.strictCheckFailure = strictCheckFailure;
    }

    @Override
    public void checkPrivileges(
            HiveOperationType hiveOpType,
            List<HivePrivilegeObject> inputHObjs,
            List<HivePrivilegeObject> outputHObjs,
            HiveAuthzContext context
    ) throws HiveAuthzPluginException, HiveAccessControlException {

        DecisionRequest request = buildDecisionRequest(context);
        DecisionResponse response;
        try {
            response = governanceClient.decide(request);
        } catch (Exception ex) {
            LOG.error("Governance platform error, fail-open BYPASS", ex);
            return;
        }

        if (response == null || !response.success() || response.data() == null || response.data().actionType() == null) {
            LOG.warn("Governance response invalid/non-200, fail-open BYPASS. response={}", response);
            return;
        }

        DecisionData data = response.data();
        switch (data.actionType()) {
            case BLOCK -> throw new HiveAccessControlException(nonEmpty(data.msg(), "任务被拦截：请补充 JobName 或按规范整改。"));
            case BYPASS -> {
                return;
            }
            case WARN -> {
                LOG.warn("WARN action for queryId={}, msg={}", request.queryId(), data.msg());
                return;
            }
            case CHECK -> {
                try {
                    super.checkPrivileges(hiveOpType, inputHObjs, outputHObjs, context);
                } catch (HiveAccessControlException rangerEx) {
                    try {
                        governanceClient.msgFail(request, rangerEx.getMessage());
                    } catch (Exception callbackEx) {
                        LOG.error("msgFail callback failed", callbackEx);
                    }
                    if (strictCheckFailure) {
                        throw rangerEx;
                    }
                    LOG.warn("Observe mode enabled, swallow Ranger denial. queryId={}, err={}", request.queryId(), rangerEx.getMessage());
                }
            }
            default -> {
                return;
            }
        }
    }

    private DecisionRequest buildDecisionRequest(HiveAuthzContext context) {
        SessionState session = SessionState.get();
        String user = session != null ? session.getUserName() : "unknown";
        String jobName = "unknown";
        String queryId = "";
        if (session != null && session.getConf() != null) {
            jobName = session.getConf().get("tez.job.name", "unknown");
            queryId = session.getConf().get("hive.query.id", "");
        }
        String sql = context != null ? context.getCommandString() : "";
        String ip = context != null ? context.getIpAddress() : "";

        return new DecisionRequest(jobName, user, EngineType.HIVE_TEZ, sql, ip, queryId);
    }

    private String nonEmpty(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }
}
