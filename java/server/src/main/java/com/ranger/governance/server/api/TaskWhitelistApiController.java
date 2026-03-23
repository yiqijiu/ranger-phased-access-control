package com.ranger.governance.server.api;

import com.ranger.governance.server.api.dto.TaskWhitelistCreateRequest;
import com.ranger.governance.server.api.dto.TaskWhitelistUpdateRequest;
import com.ranger.governance.server.whitelist.TaskWhitelistItem;
import com.ranger.governance.server.whitelist.TaskWhitelistService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/task-whitelist")
public class TaskWhitelistApiController {
    private final TaskWhitelistService service;

    public TaskWhitelistApiController(TaskWhitelistService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskWhitelistItem> list() {
        return service.list();
    }

    @PostMapping
    public TaskWhitelistItem create(@Valid @RequestBody TaskWhitelistCreateRequest request) {
        return service.saveOrEnable(request.getTaskName(), request.getDescription());
    }

    @PutMapping("/{id}")
    public TaskWhitelistItem update(@PathVariable("id") Long id, @Valid @RequestBody TaskWhitelistUpdateRequest request) {
        return service.update(id, request.getDescription(), request.getEnabled());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
