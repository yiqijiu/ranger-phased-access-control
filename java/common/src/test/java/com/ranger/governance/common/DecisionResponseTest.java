package com.ranger.governance.common;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionData;
import com.ranger.governance.common.model.DecisionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DecisionResponseTest {

    @Test
    void shouldReportSuccessByCode() {
        DecisionResponse response = new DecisionResponse(200, "trace-1", new DecisionData(ActionType.BYPASS, "", false, "q"));
        Assertions.assertTrue(response.success());
    }
}
