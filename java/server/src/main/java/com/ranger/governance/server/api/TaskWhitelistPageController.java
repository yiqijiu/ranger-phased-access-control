package com.ranger.governance.server.api;

import com.ranger.governance.server.whitelist.TaskWhitelistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/task-whitelist")
public class TaskWhitelistPageController {
    private final TaskWhitelistService service;

    public TaskWhitelistPageController(TaskWhitelistService service) {
        this.service = service;
    }

    @GetMapping
    public String page(@RequestParam(value = "error", required = false) String error, Model model) {
        model.addAttribute("entries", service.list());
        model.addAttribute("error", error);
        return "task-whitelist";
    }

    @PostMapping
    public String create(
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        return handleRedirect(() -> service.saveOrEnable(taskName, description), redirectAttributes);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        return handleRedirect(() -> service.toggle(id), redirectAttributes);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        return handleRedirect(() -> service.delete(id), redirectAttributes);
    }

    private String handleRedirect(RunnableWithException operation, RedirectAttributes redirectAttributes) {
        try {
            operation.run();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/task-whitelist";
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run();
    }
}
