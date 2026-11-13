package com.npx.npx.config;

import com.npx.npx.domain.AppUser;
import com.npx.npx.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
	@Bean
	CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
			@Value("${app.admin.email}") String email,
			@Value("${app.admin.password}") String password,
			@Value("${app.admin.name}") String name) {
		return args -> {
			if (email.isBlank() || password.isBlank()) {
				return;
			}
			if (!users.existsByEmailIgnoreCase(email)) {
				users.save(new AppUser(name, email, encoder.encode(password), "ADMIN"));
			}
		};
	}
}
