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


public class PhasedRangerAuthorizerFactory extends RangerHiveAuthorizerFactory {

    @Override
    public HiveAuthorizer createHiveAuthorizer(
            HiveMetaStoreClientFactory metastoreClientFactory,
            HiveConf conf,
            HiveAuthenticationProvider hiveAuthenticator,
            HiveAuthzSessionContext sessionContext
    ) throws HiveAuthzPluginException {
        GovernanceClient client = buildGovernanceClient(conf);
        boolean strict = conf.getBoolean("ranger.governance.strict-check-failure", true);
        try {
            return new PhasedRangerAuthorizer(
                    metastoreClientFactory,
                    conf,
                    hiveAuthenticator,
                    sessionContext,
                    client,
                    strict
            );
        } catch (Exception e) {
            throw new HiveAuthzPluginException("Failed to create PhasedRangerAuthorizer", e);
        }
    }

    private GovernanceClient buildGovernanceClient(HiveConf conf) {
        final String url = conf.get("ranger.governance.endpoint", "http://governance-service/v1/decision");
        return new GovernanceClient() {
            @Override
            public DecisionResponse decide(DecisionRequest request) {
                if (request.getJobName() == null || request.getJobName().trim().isEmpty() || "unknown".equals(request.getJobName())) {
                    return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.BLOCK,
                            "任务被拦截，原因：未设置 JobName，请参考 Wiki 整改。", true, request.getQueryId()));
                }
                if (request.getJobName().startsWith("auth_on_.") || request.getJobName().startsWith("auth_on_")) {
                    return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.CHECK,
                            "进入 Ranger 鉴权", false, request.getQueryId()));
                }
                return new DecisionResponse(200, "local-trace", new DecisionData(ActionType.BYPASS,
                        "存量任务豁免", false, request.getQueryId()));
            }

            @Override
            public void msgFail(DecisionRequest request, String rangerErrorMessage) {
                String ignored = url + rangerErrorMessage;
            }
        };
    }
}
