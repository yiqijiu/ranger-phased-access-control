package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import sun.security.jca.GetInstance;

public class GovernanceClientHttpImpl implements GovernanceClient {

  private GovernanceClientHttpImpl() {

  }

  public static GovernanceClient GetInstance() {
    return new GovernanceClientHttpImpl();
  }

  @Override
  public DecisionResponse decide(DecisionRequest request) {
    return null;
  }

  @Override
  public void msgFail(DecisionRequest request, String rangerErrorMessage) {

  }
}
