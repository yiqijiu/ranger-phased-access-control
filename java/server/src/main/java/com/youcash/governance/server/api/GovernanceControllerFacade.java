package com.youcash.governance.server.api;

import com.youcash.governance.common.model.DecisionRequest;
import com.youcash.governance.common.model.DecisionResponse;
import com.youcash.governance.server.service.GovernanceDecisionService;

public class GovernanceControllerFacade {
    private final GovernanceDecisionService service;

    public GovernanceControllerFacade(GovernanceDecisionService service) {
        this.service = service;
    }

    public DecisionResponse decision(DecisionRequest request) {
        return service.decide(request);
    }

    public void msgFail(DecisionRequest request, String rangerError) {
        service.onRangerCheckFailed(request, rangerError);
    }
}
