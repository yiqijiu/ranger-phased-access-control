package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;

public class GovernanceClientHttpImpl implements GovernanceClient {
  private static final GovernanceClient INSTANCE = new GovernanceClientHttpImpl();

  private GovernanceClientHttpImpl() {

  }

  public static GovernanceClient getInstance() {
    return INSTANCE;
  }

  @Deprecated
  public static GovernanceClient GetInstance() {
    return getInstance();
  }

  @Override
  public DecisionResponse decide(DecisionRequest request) {
    return null;
  }

  @Override
  public void msgFail(DecisionRequest request, String rangerErrorMessage) {

  }
}
