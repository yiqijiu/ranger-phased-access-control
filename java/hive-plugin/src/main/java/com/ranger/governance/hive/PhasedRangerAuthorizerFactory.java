package com.ranger.governance.hive;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.protocol.GovernanceClient;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClientFactory;
import org.apache.hadoop.hive.ql.metadata.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory;

/**
 * Factory to replace RangerHiveAuthorizerFactory with phased governance proxy.
 */
public class PhasedRangerAuthorizerFactory extends RangerHiveAuthorizerFactory {

    @Override
    public HiveAuthorizer createHiveAuthorizer(
            HiveMetaStoreClientFactory metastoreClientFactory,
            HiveConf conf,
            HiveAuthenticationProvider hiveAuthenticator,
            HiveAuthzSessionContext sessionContext
    ) throws HiveAuthzPluginException {
        try {
            return new PhasedRangerAuthorizer(
                    metastoreClientFactory,
                    conf,
                    hiveAuthenticator,
                    sessionContext,
                    buildGovernanceClient(conf),
                    conf.getBoolean("ranger.governance.strict-check-failure", true)
            );
        } catch (Exception e) {
            throw new HiveAuthzPluginException("Failed to create PhasedRangerAuthorizer", e);
        }
    }

    private GovernanceClient buildGovernanceClient(HiveConf conf) {
        String url = conf.get("ranger.governance.endpoint", "http://governance-service/v1/decision");
        return new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                // TODO: replace with real HTTP/RPC client. This default keeps plugin behavior deterministic.
                if (request.jobName() == null || request.jobName().isBlank() || "unknown".equals(request.jobName())) {
                    return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.BLOCK,
                            "任务被拦截，原因：未设置 JobName，请参考 Wiki 整改。", true, request.queryId()));
                }
                if (request.jobName().startsWith("auth_on_.") || request.jobName().startsWith("auth_on_")) {
                    return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.CHECK,
                            "进入 Ranger 鉴权", false, request.queryId()));
                }
                return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.BYPASS,
                        "存量任务豁免", false, request.queryId()));
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
                // TODO: callback to governance platform /v1/msgFail
                String ignored = url + rangerErrorMessage;
            }
        };
    }
}
