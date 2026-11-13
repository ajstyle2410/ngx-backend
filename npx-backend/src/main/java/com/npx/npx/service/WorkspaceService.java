package com.npx.npx.service;

import com.npx.npx.api.ApiDtos.*;
import com.npx.npx.domain.*;
import com.npx.npx.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class WorkspaceService {
	private final UserRepository users;
	private final ProjectRepository projects;
	private final TaskRepository tasks;
	private final TimeLogRepository timeLogs;
	private final MessageRepository messages;
	private final NotificationRepository notifications;
	private final AuditLogRepository auditLogs;

	public WorkspaceService(UserRepository users, ProjectRepository projects, TaskRepository tasks,
			TimeLogRepository timeLogs, MessageRepository messages, NotificationRepository notifications,
			AuditLogRepository auditLogs) {
		this.users = users;
		this.projects = projects;
		this.tasks = tasks;
		this.timeLogs = timeLogs;
		this.messages = messages;
		this.notifications = notifications;
		this.auditLogs = auditLogs;
	}

	public TimeTotalsResponse projectTimeTotals(Long projectId, String email) {
		participantProject(projectId, email);
		return new TimeTotalsResponse(projectId, timeLogs.totalHoursForProject(projectId));
	}

	public void markMessageRead(Long id, String email) {
		AppUser actor = currentUser(email);
		WorkspaceMessage message = messages.findByIdAndRecipient_Id(id, actor.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found."));
		message.setReadAt(LocalDateTime.now());
	}

	public AppUser currentUser(String email) {
		return users.findByEmailIgnoreCase(email)
				.filter(AppUser::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active."));
	}

	public List<UserResponse> listUsers() {
		return users.findAllByOrderByNameAsc().stream().map(this::userDto).toList();
	}

	public AppUser registerUser(RegisterRequest request, org.springframework.security.crypto.password.PasswordEncoder encoder) {
		if (users.existsByEmailIgnoreCase(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists.");
		}
		AppUser user = new AppUser(request.name().trim(), request.email(), encoder.encode(request.password()), "USER");
		AppUser saved = users.save(user);
		writeAudit(saved, "USER_REGISTERED", "USER", saved.getId(), "{\"email\":\"" + safeJson(saved.getEmail()) + "\"}");
		return saved;
	}

	public UserResponse createUser(UserRequest request, String actorEmail, org.springframework.security.crypto.password.PasswordEncoder encoder) {
		if (users.existsByEmailIgnoreCase(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists.");
		}
		AppUser user = new AppUser(request.name().trim(), request.email(), encoder.encode(request.password()), request.role());
		AppUser saved = users.save(user);
		writeAudit(currentUser(actorEmail), "USER_CREATED", "USER", saved.getId(), "{\"email\":\"" + safeJson(saved.getEmail()) + "\"}");
		return userDto(saved);
	}

	public UserResponse updateUser(Long id, UserUpdateRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		AppUser user = activeUser(id);
		if (!actor.getId().equals(id) && users.existsByEmailIgnoreCase(request.email())
				&& !user.getEmail().equalsIgnoreCase(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists.");
		}
		if (actor.getId().equals(id) && !actor.getRole().equals(request.role())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own administrator role.");
		}
		user.setName(request.name().trim());
		user.setEmail(request.email().trim().toLowerCase());
		user.setRole(request.role());
		AppUser saved = users.save(user);
		writeAudit(actor, "USER_UPDATED", "USER", saved.getId(), "{\"email\":\"" + safeJson(saved.getEmail()) + "\"}");
		return userDto(saved);
	}

	public UserResponse setUserActive(Long id, boolean active, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
		if (actor.getId().equals(id) && !active) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account.");
		}
		user.setActive(active);
		AppUser saved = users.save(user);
		writeAudit(actor, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id, "{}");
		return userDto(saved);
	}

	public List<ProjectResponse> listProjects(String email) {
		AppUser actor = currentUser(email);
		List<Project> result = actor.getRole().equals("ADMIN")
				? projects.findAllByOrderByCreatedAtDesc()
				: projects.findDistinctByParticipants_IdOrderByCreatedAtDesc(actor.getId());
		return result.stream().map(this::projectDto).toList();
	}

	public ProjectResponse participantProject(Long projectId, String email) {
		AppUser actor = currentUser(email);
		Project project = project(projectId);
		if (!actor.getRole().equals("ADMIN") && project.getParticipants().stream()
				.noneMatch(participant -> participant.getId().equals(actor.getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this project.");
		}
		return projectDto(project);
	}

	public List<TaskResponse> listProjectTasks(Long projectId) {
		return tasks.findAllByProject_IdOrderByDueDateAsc(projectId).stream().map(this::taskDto).toList();
	}

	public ProjectResponse createProject(ProjectRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		Project project = new Project();
		project.setName(request.name().trim());
		project.setDescription(request.description());
		project.setStartDate(request.startDate());
		project.setEndDate(request.endDate());
		project.setCreatedBy(actor);
		addParticipant(project, actor);
		if (request.participantIds() != null) {
			for (Long participantId : request.participantIds()) {
				addParticipant(project, activeUser(participantId));
			}
		}
		if (actor.getRole().equals("USER")) {
			List<AppUser> adminList = users.findAll().stream().filter(u -> u.getRole().equals("ADMIN") && u.isActive()).toList();
			for (AppUser admin : adminList) {
				addParticipant(project, admin);
			}
		}
		Project saved = projects.save(project);
		if (actor.getRole().equals("USER")) {
			List<AppUser> adminList = users.findAll().stream().filter(u -> u.getRole().equals("ADMIN") && u.isActive()).toList();
			for (AppUser admin : adminList) {
				notifyUser(admin, "PROJECT_SUBMITTED", actor.getName() + " submitted project requirements: " + saved.getName());
			}
		}
		writeAudit(actor, "PROJECT_CREATED", "PROJECT", saved.getId(), "{\"name\":\"" + safeJson(saved.getName()) + "\"}");
		return projectDto(saved);
	}

	public ProjectResponse updateProject(Long id, ProjectRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		Project project = project(id);
		project.setName(request.name().trim());
		project.setDescription(request.description());
		project.setStartDate(request.startDate());
		project.setEndDate(request.endDate());
		Project saved = projects.save(project);
		writeAudit(actor, "PROJECT_UPDATED", "PROJECT", id, "{\"name\":\"" + safeJson(saved.getName()) + "\"}");
		return projectDto(saved);
	}

	public ProjectResponse updateProjectSchedule(Long id, java.time.LocalDate startDate, java.time.LocalDate endDate, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		Project project = project(id);
		project.setStartDate(startDate);
		project.setEndDate(endDate);
		Project saved = projects.save(project);
		String scheduleInfo = "Administrator decided schedule for project '" + saved.getName() + "': Start "
				+ (startDate != null ? startDate : "TBD") + " → End " + (endDate != null ? endDate : "TBD");
		for (AppUser participant : saved.getParticipants()) {
			if (!participant.getId().equals(actor.getId())) {
				notifyUser(participant, "SCHEDULE_UPDATED", scheduleInfo);
			}
		}
		writeAudit(actor, "PROJECT_SCHEDULE_UPDATED", "PROJECT", id,
				"{\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}");
		return projectDto(saved);
	}

	public ProjectResponse addParticipant(Long projectId, Long userId, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		Project project = project(projectId);
		AppUser participant = activeUser(userId);
		addParticipant(project, participant);
		Project saved = projects.save(project);
		writeAudit(actor, "PARTICIPANT_ADDED", "PROJECT", projectId,
				"{\"userId\":" + participant.getId() + "}");
		return projectDto(saved);
	}

	public List<TaskResponse> listTasks(String email, boolean developerOnly) {
		AppUser actor = currentUser(email);
		List<WorkTask> selected;
		if (developerOnly || actor.getRole().equals("DEVELOPER")) {
			selected = tasks.findAllByAssignedTo_IdOrderByDueDateAsc(actor.getId());
		} else if (actor.getRole().equals("USER")) {
			selected = projects.findDistinctByParticipants_IdOrderByCreatedAtDesc(actor.getId()).stream()
					.flatMap(project -> tasks.findAllByProject_IdOrderByDueDateAsc(project.getId()).stream()).toList();
		} else {
			selected = tasks.findAllByOrderByDueDateAsc();
		}
		return selected.stream().map(this::taskDto).toList();
	}

	public List<TaskResponse> listUserTasks(String email) {
		AppUser actor = currentUser(email);
		List<WorkTask> selected = projects.findDistinctByParticipants_IdOrderByCreatedAtDesc(actor.getId()).stream()
				.flatMap(project -> tasks.findAllByProject_IdOrderByDueDateAsc(project.getId()).stream()).toList();
		return selected.stream().map(this::taskDto).toList();
	}

	public TaskResponse createTask(TaskRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		Project project = project(request.projectId());
		if (!project.getParticipants().stream().anyMatch(user -> user.getId().equals(actor.getId()))) {
			addParticipant(project, actor);
		}
		WorkTask task = new WorkTask();
		task.setProject(project);
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		task.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
		task.setDueDate(request.dueDate());
		task.setCreatedBy(actor);
		if (request.assignedTo() != null) {
			AppUser developer = activeUser(request.assignedTo());
			if (!developer.getRole().equals("DEVELOPER")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tasks can only be assigned to developers.");
			}
			task.setAssignedTo(developer);
			addParticipant(project, developer);
		}
		WorkTask saved = tasks.save(task);
		if (saved.getAssignedTo() != null) {
			notifyUser(saved.getAssignedTo(), "TASK_ASSIGNED", "You were assigned: " + saved.getTitle());
		}
		writeAudit(actor, "TASK_CREATED", "TASK", saved.getId(), "{\"title\":\"" + safeJson(saved.getTitle()) + "\"}");
		return taskDto(saved);
	}

	public TaskResponse updateTask(Long id, TaskRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		WorkTask task = task(id);
		Project project = project(request.projectId());
		task.setProject(project);
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		task.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
		task.setDueDate(request.dueDate());
		if (request.assignedTo() == null) {
			task.setAssignedTo(null);
		} else {
			AppUser developer = activeUser(request.assignedTo());
			if (!developer.getRole().equals("DEVELOPER")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tasks can only be assigned to developers.");
			}
			task.setAssignedTo(developer);
			addParticipant(project, developer);
			notifyUser(developer, "TASK_ASSIGNED", "You were assigned: " + task.getTitle());
		}
		WorkTask saved = tasks.save(task);
		writeAudit(actor, "TASK_UPDATED", "TASK", id, "{\"title\":\"" + safeJson(saved.getTitle()) + "\"}");
		return taskDto(saved);
	}

	public TaskResponse updateStatus(Long taskId, String status, String actorEmail, boolean administrator) {
		AppUser actor = currentUser(actorEmail);
		WorkTask task = task(taskId);
		if (!administrator && (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(actor.getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update tasks assigned to you.");
		}
		String oldStatus = task.getStatus();
		if (oldStatus.equals("DONE") && !status.equals("DONE") && !administrator) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can reopen a completed task.");
		}
		task.setStatus(status);
		task.setCompletedAt(status.equals("DONE") ? LocalDateTime.now() : null);
		WorkTask saved = tasks.save(task);
		writeAudit(actor, "TASK_STATUS_CHANGED", "TASK", taskId,
				"{\"from\":\"" + oldStatus + "\",\"to\":\"" + status + "\"}");
		for (AppUser participant : task.getProject().getParticipants()) {
			if (participant.getRole().equals("USER") || participant.getRole().equals("ADMIN")) {
				notifyUser(participant, "TASK_STATUS", task.getTitle() + " is now " + status.replace('_', ' ').toLowerCase() + ".");
			}
		}
		return taskDto(saved);
	}

	public TimeLogResponse addTimeLog(Long taskId, TimeLogRequest request, String actorEmail) {
		AppUser actor = currentUser(actorEmail);
		WorkTask task = task(taskId);
		if (!actor.getRole().equals("DEVELOPER") || task.getAssignedTo() == null
				|| !task.getAssignedTo().getId().equals(actor.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only log time on tasks assigned to you.");
		}
		if (request.hours().compareTo(BigDecimal.ZERO) <= 0 || request.hours().compareTo(new BigDecimal("24")) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hours must be greater than 0 and no more than 24.");
		}
		TimeLog log = new TimeLog();
		log.setTask(task);
		log.setDeveloper(actor);
		log.setHours(request.hours());
		log.setWorkDate(request.workDate());
		log.setNote(request.note());
		TimeLog saved = timeLogs.save(log);
		return new TimeLogResponse(saved.getId(), taskId, task.getTitle(), saved.getHours(), saved.getWorkDate(), saved.getNote());
	}

	public DashboardResponse adminSummary() {
		BigDecimal totalHours = timeLogs.findAllRecent().stream()
				.map(TimeLog::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
		long activeProjects = projects.findAllByOrderByCreatedAtDesc().stream()
				.filter(project -> project.getStatus().equals("ACTIVE")).count();
		return new DashboardResponse(activeProjects, tasks.countByStatusNot("DONE"),
				tasks.countByStatusNotAndDueDateBefore("DONE", LocalDate.now()), totalHours, tasks.countByStatus("DONE"));
	}

	public DashboardResponse developerSummary(String email) {
		AppUser actor = currentUser(email);
		List<WorkTask> assigned = tasks.findAllByAssignedTo_IdOrderByDueDateAsc(actor.getId());
		BigDecimal hours = timeLogs.findRecentForDeveloper(actor.getId()).stream()
				.map(TimeLog::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
		long completed = assigned.stream().filter(task -> task.getStatus().equals("DONE")).count();
		long active = assigned.stream().filter(task -> !task.getStatus().equals("DONE")).count();
		long overdue = assigned.stream().filter(task -> !task.getStatus().equals("DONE") && task.getDueDate() != null
				&& task.getDueDate().isBefore(LocalDate.now())).count();
		return new DashboardResponse(0, active, overdue, hours, completed);
	}

	public List<MessageResponse> listMessages(String email) {
		AppUser actor = currentUser(email);
		return messages.findBySender_IdOrRecipient_IdOrderByCreatedAtAsc(actor.getId(), actor.getId())
				.stream().map(this::messageDto).toList();
	}

	public MessageResponse sendMessage(MessageRequest request, String email) {
		AppUser sender = currentUser(email);
		Project project = project(request.projectId());
		AppUser recipient = activeUser(request.recipientId());
		Set<AppUser> participants = project.getParticipants();
		boolean senderIsParticipant = participants.stream().anyMatch(user -> user.getId().equals(sender.getId()));
		boolean recipientIsParticipant = participants.stream().anyMatch(user -> user.getId().equals(recipient.getId()));
		boolean permittedPair = !sender.getId().equals(recipient.getId())
				&& (sender.getRole().equals("ADMIN") ^ recipient.getRole().equals("ADMIN"));
		if (!senderIsParticipant || !recipientIsParticipant || !permittedPair) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Messages must be exchanged with an administrator within a shared project.");
		}
		WorkspaceMessage message = new WorkspaceMessage();
		message.setProject(project);
		message.setSender(sender);
		message.setRecipient(recipient);
		message.setBody(request.body().trim());
		WorkspaceMessage saved = messages.save(message);
		notifyUser(recipient, "MESSAGE", "New message in " + project.getName());
		return messageDto(saved);
	}

	public List<NotificationResponse> listNotifications(String email) {
		AppUser actor = currentUser(email);
		return notifications.findAllByUser_IdOrderByCreatedAtDesc(actor.getId()).stream()
				.map(item -> new NotificationResponse(item.getId(), item.getType(), item.getContent(), item.isRead(), item.getCreatedAt()))
				.toList();
	}

	public void markNotificationRead(Long id, String email) {
		AppUser actor = currentUser(email);
		WorkspaceNotification notification = notifications.findByIdAndUser_Id(id, actor.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."));
		notification.setRead(true);
	}

	public List<AuditResponse> auditLogs() {
		return auditLogs.findTop100ByOrderByCreatedAtDesc().stream()
				.map(item -> new AuditResponse(item.getId(), item.getActor().getId(), item.getActor().getName(),
						item.getAction(), item.getEntityType(), item.getEntityId(), item.getDetails(), item.getCreatedAt()))
				.toList();
	}

	public byte[] csvReport(Long projectId, String email) {
		AppUser actor = currentUser(email);
		Project project = project(projectId);
		if (!actor.getRole().equals("ADMIN")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can export reports.");
		}
		List<WorkTask> projectTasks = tasks.findAllByProject_IdOrderByDueDateAsc(projectId);
		StringBuilder csv = new StringBuilder("Project,Task,Status,Priority,Developer,Due date,Hours\r\n");
		for (WorkTask task : projectTasks) {
			csv.append(csvCell(project.getName())).append(',')
					.append(csvCell(task.getTitle())).append(',')
					.append(csvCell(task.getStatus())).append(',')
					.append(csvCell(task.getPriority())).append(',')
					.append(csvCell(task.getAssignedTo() == null ? "" : task.getAssignedTo().getName())).append(',')
					.append(csvCell(task.getDueDate() == null ? "" : task.getDueDate().toString())).append(',')
					.append(timeLogs.totalHoursForTask(task.getId())).append("\r\n");
		}
		return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	private void addParticipant(Project project, AppUser participant) {
		project.getParticipants().stream()
				.filter(existing -> existing.getId() != null && existing.getId().equals(participant.getId()))
				.findFirst()
				.orElseGet(() -> {
					project.getParticipants().add(participant);
					return participant;
				});
	}

	private ProjectResponse projectDto(Project project) {
		long total = tasks.countByProject_Id(project.getId());
		long done = tasks.countByProject_IdAndStatus(project.getId(), "DONE");
		int progress = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
		Long createdById = project.getCreatedBy() != null ? project.getCreatedBy().getId() : null;
		String createdByName = project.getCreatedBy() != null ? project.getCreatedBy().getName() : null;
		return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getStatus(),
				project.getStartDate(), project.getEndDate(), total, done, progress,
				timeLogs.totalHoursForProject(project.getId()), project.getParticipants().stream().map(this::userDto).toList(),
				createdById, createdByName);
	}

	private TaskResponse taskDto(WorkTask task) {
		return new TaskResponse(task.getId(), task.getProject().getId(), task.getProject().getName(), task.getTitle(),
				task.getDescription(), task.getStatus(), task.getPriority(), task.getDueDate(),
				task.getAssignedTo() == null ? null : task.getAssignedTo().getId(),
				task.getAssignedTo() == null ? null : task.getAssignedTo().getName(),
				timeLogs.totalHoursForTask(task.getId()), task.getCreatedAt());
	}

	private UserResponse userDto(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
	}

	private MessageResponse messageDto(WorkspaceMessage message) {
		return new MessageResponse(message.getId(), message.getProject().getId(), message.getSender().getId(),
				message.getSender().getName(), message.getRecipient().getId(), message.getRecipient().getName(),
				message.getBody(), message.getCreatedAt(), message.getReadAt());
	}

	private AppUser activeUser(Long id) {
		return users.findById(id).filter(AppUser::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active user not found."));
	}

	private Project project(Long id) {
		return projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
	}

	private WorkTask task(Long id) {
		return tasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
	}

	private void notifyUser(AppUser user, String type, String content) {
		WorkspaceNotification notification = new WorkspaceNotification();
		notification.setUser(user);
		notification.setType(type);
		notification.setContent(content);
		notifications.save(notification);
	}

	private void writeAudit(AppUser actor, String action, String entityType, Long entityId, String details) {
		AuditLog item = new AuditLog();
		item.setActor(actor);
		item.setAction(action);
		item.setEntityType(entityType);
		item.setEntityId(entityId);
		item.setDetails(details);
		auditLogs.save(item);
	}

	private String safeJson(String text) {
		return text.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String csvCell(String text) {
		return "\"" + (text == null ? "" : text.replace("\"", "\"\"")) + "\"";
	}
}
