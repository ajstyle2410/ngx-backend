package com.npx.npx.api;

import com.npx.npx.api.ApiDtos.DashboardResponse;
import com.npx.npx.api.ApiDtos.ProjectResponse;
import com.npx.npx.api.ApiDtos.StatusRequest;
import com.npx.npx.api.ApiDtos.TaskResponse;
import com.npx.npx.api.ApiDtos.TimeLogRequest;
import com.npx.npx.api.ApiDtos.TimeLogResponse;
import com.npx.npx.service.WorkspaceService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/developer")
@PreAuthorize("hasRole('DEVELOPER')")
public class DeveloperController {
	
	private final WorkspaceService workspace;

	public DeveloperController(WorkspaceService workspace) {
		this.workspace = workspace;
	}

	@GetMapping("/projects")
	public List<ProjectResponse> projects(Authentication auth) {
		return workspace.listProjects(auth.getName());
	}

	@GetMapping("/tasks")
	public List<TaskResponse> tasks(Authentication auth) {
		return workspace.listTasks(auth.getName(), true);
	}

	@PatchMapping("/tasks/{id}/status")
	public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
		return workspace.updateStatus(id, request.status(), auth.getName(), false);
	}

	@PostMapping("/tasks/{id}/time-logs")
	public TimeLogResponse logTime(@PathVariable Long id, @Valid @RequestBody TimeLogRequest request, Authentication auth) {
		return workspace.addTimeLog(id, request, auth.getName());
	}

	@GetMapping("/dashboard/summary")
	public DashboardResponse summary(Authentication auth) {
		return workspace.developerSummary(auth.getName());
	}
}
