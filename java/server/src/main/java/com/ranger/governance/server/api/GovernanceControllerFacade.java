package com.ranger.governance.server.api;

import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.server.api.dto.DecisionRequestPayload;
import com.ranger.governance.server.api.dto.MsgFailPayload;
import com.ranger.governance.server.service.GovernanceDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/governance")
public class GovernanceControllerFacade {
    private final GovernanceDecisionService service;

    public GovernanceControllerFacade(GovernanceDecisionService service) {
        this.service = service;
    }

    @PostMapping("/decision")
    public DecisionResponse decision(@Valid @RequestBody DecisionRequestPayload request) {
        return service.decide(request.toModel());
    }

    @PostMapping("/msg-fail")
    public ResponseEntity<Void> msgFail(@Valid @RequestBody MsgFailPayload payload) {
        service.onRangerCheckFailed(payload.getRequest().toModel(), payload.getRangerError());
        return ResponseEntity.ok().build();
    }
}
