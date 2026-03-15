package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;

public interface GovernanceClient {
    DecisionResponse decide(DecisionRequest request);

    void msgFail(DecisionRequest request, String rangerErrorMessage);
}
