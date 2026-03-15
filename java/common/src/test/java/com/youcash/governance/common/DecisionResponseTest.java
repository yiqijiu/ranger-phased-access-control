package com.youcash.governance.common;

import com.youcash.governance.common.model.ActionType;
import com.youcash.governance.common.model.DecisionData;
import com.youcash.governance.common.model.DecisionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DecisionResponseTest {

    @Test
    void shouldReportSuccessByCode() {
        DecisionResponse response = new DecisionResponse(200, "trace-1", new DecisionData(ActionType.BYPASS, "", false, "q"));
        Assertions.assertTrue(response.success());
    }
}
