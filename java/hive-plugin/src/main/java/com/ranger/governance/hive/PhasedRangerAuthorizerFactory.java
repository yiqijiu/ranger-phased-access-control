package com.ranger.governance.hive;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.protocol.GovernanceClient;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactory;
import org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizer;
import org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory;

/**
 * 自定义 Ranger 工厂：用于替换原生的 RangerHiveAuthorizerFactory
 */
public class PhasedRangerAuthorizerFactory extends RangerHiveAuthorizerFactory {
  @Override
  public HiveAuthorizer createHiveAuthorizer(HiveMetastoreClientFactory metastoreClientFactory, HiveConf conf,
      HiveAuthenticationProvider hiveAuthenticator, HiveAuthzSessionContext sessionContext)
      throws HiveAuthzPluginException {
    return new PhasedRangerAuthorizer(metastoreClientFactory, conf, hiveAuthenticator, sessionContext);
  }
}

