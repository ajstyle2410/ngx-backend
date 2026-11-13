package com.npx.npx.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
	private ApiDtos() {}

	public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
	public record LoginResponse(Long id, String token, String role, String name, String email) {}
	public record RegisterRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email,
			@NotBlank @Size(min = 8, max = 72) String password) {}
	public record UserRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email,
			@NotBlank @Size(min = 8, max = 72) String password, @NotNull @Pattern(regexp = "USER|ADMIN|DEVELOPER") String role) {}
	public record UserUpdateRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email,
			@NotNull @Pattern(regexp = "USER|ADMIN|DEVELOPER") String role) {}
	public record ActiveRequest(boolean active) {}
	public record UserResponse(Long id, String name, String email, String role, boolean active) {}
	public record ProjectRequest(@NotBlank @Size(max = 200) String name, String description,
			LocalDate startDate, LocalDate endDate, List<Long> participantIds) {}
	public record ProjectScheduleRequest(LocalDate startDate, LocalDate endDate) {}
	public record ParticipantRequest(@NotNull Long userId) {}
	public record ProjectResponse(Long id, String name, String description, String status, LocalDate startDate,
			LocalDate endDate, long totalTasks, long doneTasks, int progress, BigDecimal totalHours,
			List<UserResponse> participants, Long createdById, String createdByName) {}
	public record TimeTotalsResponse(Long projectId, BigDecimal totalHours) {}
	public record TaskRequest(@NotNull Long projectId, @NotBlank @Size(max = 200) String title,
			String description, @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority, LocalDate dueDate,
			Long assignedTo) {}
	public record TaskResponse(Long id, Long projectId, String projectName, String title, String description,
			String status, String priority, LocalDate dueDate, Long assignedToId, String assignedTo,
			BigDecimal hours, LocalDateTime createdAt) {}
	public record StatusRequest(@NotBlank @Pattern(regexp = "TODO|IN_PROGRESS|DONE") String status) {}
	public record TimeLogRequest(@NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "24.00") BigDecimal hours,
			@NotNull LocalDate workDate, @Size(max = 255) String note) {}
	public record TimeLogResponse(Long id, Long taskId, String taskTitle, BigDecimal hours,
			LocalDate workDate, String note) {}
	public record DashboardResponse(long activeProjects, long openTasks, long overdueTasks,
			BigDecimal totalHours, long completedTasks) {}
	public record MessageRequest(@NotNull Long projectId, @NotNull Long recipientId,
			@NotBlank @Size(max = 5000) String body) {}
	public record MessageResponse(Long id, Long projectId, Long senderId, String senderName,
			Long recipientId, String recipientName, String body, LocalDateTime createdAt, LocalDateTime readAt) {}
	public record NotificationResponse(Long id, String type, String content, boolean read, LocalDateTime createdAt) {}
	public record AuditResponse(Long id, Long actorId, String actorName, String action, String entityType,
			Long entityId, String details, LocalDateTime createdAt) {}
	public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
}
