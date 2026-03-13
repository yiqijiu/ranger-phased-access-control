package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.DecisionRequest;

public interface RangerDelegate {
    void checkPrivileges(DecisionRequest request);
}
