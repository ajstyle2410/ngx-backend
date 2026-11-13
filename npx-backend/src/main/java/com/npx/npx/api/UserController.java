package com.npx.npx.api;

import com.npx.npx.api.ApiDtos.ProjectResponse;
import com.npx.npx.api.ApiDtos.TaskResponse;
import com.npx.npx.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')")
public class UserController {
	private final WorkspaceService workspace;

	public UserController(WorkspaceService workspace) {
		this.workspace = workspace;
	}

	@GetMapping("/projects")
	public List<ProjectResponse> projects(Authentication auth) {
		return workspace.listProjects(auth.getName());
	}

	@PostMapping("/projects")
	@ResponseStatus(org.springframework.http.HttpStatus.CREATED)
	public ProjectResponse createProject(@jakarta.validation.Valid @RequestBody com.npx.npx.api.ApiDtos.ProjectRequest request, Authentication auth) {
		return workspace.createProject(request, auth.getName());
	}

	@GetMapping("/tasks")
	public List<TaskResponse> tasks(Authentication auth) {
		return workspace.listUserTasks(auth.getName());
	}

	@GetMapping("/projects/{id}")
	public ProjectResponse project(@PathVariable Long id, Authentication auth) {
		return workspace.participantProject(id, auth.getName());
	}

	@GetMapping("/projects/{id}/progress")
	public ProjectResponse progress(@PathVariable Long id, Authentication auth) {
		return workspace.participantProject(id, auth.getName());
	}

	@GetMapping("/projects/{id}/tasks")
	public List<TaskResponse> tasks(@PathVariable Long id, Authentication auth) {
		workspace.participantProject(id, auth.getName());
		return workspace.listProjectTasks(id);
	}

	@GetMapping("/projects/{id}/time-totals")
	public com.npx.npx.api.ApiDtos.TimeTotalsResponse timeTotals(@PathVariable Long id, Authentication auth) {
		return workspace.projectTimeTotals(id, auth.getName());
	}
}
