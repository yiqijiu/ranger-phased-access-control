package com.ranger.governance.hive;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.ranger.governance.common.protocol.GovernanceClient;
import com.ranger.governance.common.protocol.GovernanceClientHttpImpl;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.*;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PhasedRangerAuthorizer extends RangerHiveAuthorizer {
  private static final Logger LOG = LoggerFactory.getLogger(PhasedRangerAuthorizer.class);

  private final GovernanceClient governanceClient;

  public PhasedRangerAuthorizer(HiveMetastoreClientFactory metastoreClientFactory, HiveConf conf,
      HiveAuthenticationProvider hiveAuthenticator, HiveAuthzSessionContext sessionContext) {
    super(metastoreClientFactory, conf, hiveAuthenticator, sessionContext);
    this.governanceClient = GovernanceClientHttpImpl.GetInstance();
  }

  @Override
  public void checkPrivileges(HiveOperationType hiveOpType, List<HivePrivilegeObject> inputHObjs,
      List<HivePrivilegeObject> outputHObjs, HiveAuthzContext context)
      throws HiveAuthzPluginException, HiveAccessControlException {

    DecisionRequest request = buildDecisionRequest(context);
    DecisionResponse response;
    try {
      response = governanceClient.decide(request);
    } catch (Exception ex) {
      LOG.error("Governance platform error, fail-open BYPASS", ex);
      return;
    }
    if (response == null || !response.success() || response.getData() == null || response.getData()
        .getActionType() == null) {
      LOG.warn("Governance response invalid/non-200, fail-open BYPASS. response={}", response);
      return;
    }

    DecisionData data = response.getData();
    ActionType actionType = data.getActionType();
    if (actionType == ActionType.BLOCK) {
      throw new HiveAccessControlException(data.getMsg());
    }
    if (actionType == ActionType.BYPASS) {
      return;
    }
    if (actionType == ActionType.WARN) {
      LOG.warn("WARN action for queryId={}, msg={}", request.getQueryId(), data.getMsg());
      return;
    }
    if (actionType == ActionType.CHECK) {
      try {
        super.checkPrivileges(hiveOpType, inputHObjs, outputHObjs, context);
      } catch (Exception rangerEx){
        try {
          governanceClient.msgFail(request, rangerEx.getMessage());
        } catch (Exception callbackEx) {
          LOG.error("msgFail callback failed", callbackEx);
        }
        LOG.warn("Observe mode enabled, swallow Ranger denial. queryId={}, err={}", request.getQueryId(),
            rangerEx.getMessage());
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
}
