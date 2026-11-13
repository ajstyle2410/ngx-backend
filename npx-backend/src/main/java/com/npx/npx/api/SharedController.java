package com.npx.npx.api;

import com.npx.npx.api.ApiDtos.MessageRequest;
import com.npx.npx.api.ApiDtos.MessageResponse;
import com.npx.npx.api.ApiDtos.NotificationResponse;
import com.npx.npx.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SharedController {
	private final WorkspaceService workspace;

	public SharedController(WorkspaceService workspace) {
		this.workspace = workspace;
	}

	@GetMapping("/api/messages")
	public List<MessageResponse> messages(Authentication auth) {
		return workspace.listMessages(auth.getName());
	}

	@PostMapping("/api/messages")
	public MessageResponse sendMessage(@Valid @RequestBody MessageRequest request, Authentication auth) {
		return workspace.sendMessage(request, auth.getName());
	}

	@PatchMapping("/api/messages/{id}/read")
	public void markMessageRead(@PathVariable Long id, Authentication auth) {
		workspace.markMessageRead(id, auth.getName());
	}

	@GetMapping("/api/notifications")
	public List<NotificationResponse> notifications(Authentication auth) {
		return workspace.listNotifications(auth.getName());
	}

	@PatchMapping("/api/notifications/{id}/read")
	public void markRead(@PathVariable Long id, Authentication auth) {
		workspace.markNotificationRead(id, auth.getName());
	}
}
