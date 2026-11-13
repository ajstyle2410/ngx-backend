package com.npx.npx.api;

import com.npx.npx.api.ApiDtos.LoginRequest;
import com.npx.npx.api.ApiDtos.LoginResponse;
import com.npx.npx.api.ApiDtos.RegisterRequest;
import com.npx.npx.domain.AppUser;
import com.npx.npx.repository.UserRepository;
import com.npx.npx.security.JwtService;
import com.npx.npx.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final WorkspaceService workspace;

	public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			WorkspaceService workspace) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.workspace = workspace;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		AppUser user = users.findByEmailIgnoreCase(request.email())
				.filter(AppUser::isActive)
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
		return new LoginResponse(user.getId(), jwtService.createToken(user), user.getRole(), user.getName(), user.getEmail());
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
		AppUser user = workspace.registerUser(request, passwordEncoder);
		return new LoginResponse(user.getId(), jwtService.createToken(user), user.getRole(), user.getName(), user.getEmail());
	}
}
