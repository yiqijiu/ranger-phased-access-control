package com.youcash.governance.common.protocol;

import com.youcash.governance.common.model.DecisionRequest;
import com.youcash.governance.common.model.DecisionResponse;

public interface GovernanceClient {
    DecisionResponse decide(DecisionRequest request);

    void msgFail(DecisionRequest request, String rangerErrorMessage);
}
