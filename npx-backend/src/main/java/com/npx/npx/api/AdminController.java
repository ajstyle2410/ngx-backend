package com.npx.npx.api;

import com.npx.npx.api.ApiDtos.ActiveRequest;
import com.npx.npx.api.ApiDtos.AuditResponse;
import com.npx.npx.api.ApiDtos.DashboardResponse;
import com.npx.npx.api.ApiDtos.ParticipantRequest;
import com.npx.npx.api.ApiDtos.ProjectRequest;
import com.npx.npx.api.ApiDtos.ProjectResponse;
import com.npx.npx.api.ApiDtos.ProjectScheduleRequest;
import com.npx.npx.api.ApiDtos.StatusRequest;
import com.npx.npx.api.ApiDtos.TaskRequest;
import com.npx.npx.api.ApiDtos.TaskResponse;
import com.npx.npx.api.ApiDtos.UserRequest;
import com.npx.npx.api.ApiDtos.UserResponse;
import com.npx.npx.api.ApiDtos.UserUpdateRequest;
import com.npx.npx.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
	private final WorkspaceService workspace;
	private final PasswordEncoder passwordEncoder;

	public AdminController(WorkspaceService workspace, PasswordEncoder passwordEncoder) {
		this.workspace = workspace;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/users")
	public List<UserResponse> users() {
		return workspace.listUsers();
	}

	@PostMapping("/users")
	public UserResponse createUser(@Valid @RequestBody UserRequest request, Authentication auth) {
		return workspace.createUser(request, auth.getName(), passwordEncoder);
	}

	@PutMapping("/users/{id}")
	public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request, Authentication auth) {
		return workspace.updateUser(id, request, auth.getName());
	}

	@PatchMapping("/users/{id}/active")
	public UserResponse setUserActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request, Authentication auth) {
		return workspace.setUserActive(id, request.active(), auth.getName());
	}

	@GetMapping("/projects")
	public List<ProjectResponse> projects(Authentication auth) {
		return workspace.listProjects(auth.getName());
	}

	@PostMapping("/projects")
	public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request, Authentication auth) {
		return workspace.createProject(request, auth.getName());
	}

	@PutMapping("/projects/{id}")
	public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request, Authentication auth) {
		return workspace.updateProject(id, request, auth.getName());
	}

	@PatchMapping("/projects/{id}/schedule")
	public ProjectResponse updateProjectSchedule(@PathVariable Long id, @Valid @RequestBody ProjectScheduleRequest request, Authentication auth) {
		return workspace.updateProjectSchedule(id, request.startDate(), request.endDate(), auth.getName());
	}

	@PostMapping("/projects/{id}/participants")
	public ProjectResponse addParticipant(@PathVariable Long id, @Valid @RequestBody ParticipantRequest request, Authentication auth) {
		return workspace.addParticipant(id, request.userId(), auth.getName());
	}

	@GetMapping("/tasks")
	public List<TaskResponse> tasks(Authentication auth) {
		return workspace.listTasks(auth.getName(), false);
	}

	@PostMapping("/tasks")
	public TaskResponse createTask(@Valid @RequestBody TaskRequest request, Authentication auth) {
		return workspace.createTask(request, auth.getName());
	}

	@PostMapping("/projects/{projectId}/tasks")
	public TaskResponse createProjectTask(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request, Authentication auth) {
		if (!projectId.equals(request.projectId())) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "Task project ID does not match the request path.");
		}
		return workspace.createTask(request, auth.getName());
	}

	@PutMapping("/tasks/{id}")
	public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request, Authentication auth) {
		return workspace.updateTask(id, request, auth.getName());
	}

	@PatchMapping("/tasks/{id}/status")
	public TaskResponse updateTaskStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
		return workspace.updateStatus(id, request.status(), auth.getName(), true);
	}

	@GetMapping("/dashboard/summary")
	public DashboardResponse summary() {
		return workspace.adminSummary();
	}

	@GetMapping(value = "/reports/projects/{id}", produces = "text/csv")
	public ResponseEntity<ByteArrayResource> report(@PathVariable Long id, Authentication auth) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"project-" + id + "-report.csv\"")
				.body(new ByteArrayResource(workspace.csvReport(id, auth.getName())));
	}

	@GetMapping("/audit-logs")
	public List<AuditResponse> auditLogs() {
		return workspace.auditLogs();
	}
}
