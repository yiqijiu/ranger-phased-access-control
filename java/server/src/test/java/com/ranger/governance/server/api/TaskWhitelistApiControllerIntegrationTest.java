package com.ranger.governance.server.api;

import com.ranger.governance.server.whitelist.TaskWhitelistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskWhitelistApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskWhitelistRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateAndListByApi() throws Exception {
        mockMvc.perform(post("/api/v1/task-whitelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskName\":\"etl_finance_daily_01\",\"description\":\"daily\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"taskName\":\"etl_finance_daily_01\"")));

        mockMvc.perform(get("/api/v1/task-whitelist"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"taskName\":\"etl_finance_daily_01\"")));
    }

    @Test
    void shouldRenderAdminPage() throws Exception {
        mockMvc.perform(get("/admin/task-whitelist"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("任务名称白名单配置")));
    }
}
