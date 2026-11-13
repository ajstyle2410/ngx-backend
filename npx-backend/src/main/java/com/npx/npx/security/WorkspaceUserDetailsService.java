package com.npx.npx.security;

import com.npx.npx.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceUserDetailsService implements UserDetailsService {
	private final UserRepository users;

	public WorkspaceUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return users.findByEmailIgnoreCase(email)
				.map(user -> User.withUsername(user.getEmail())
						.password(user.getPasswordHash())
						.roles(user.getRole())
						.disabled(!user.isActive())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
	}
}
