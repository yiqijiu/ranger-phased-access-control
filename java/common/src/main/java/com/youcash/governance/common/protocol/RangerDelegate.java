package com.youcash.governance.common.protocol;

import com.youcash.governance.common.model.DecisionRequest;

public interface RangerDelegate {
    void checkPrivileges(DecisionRequest request);
}
