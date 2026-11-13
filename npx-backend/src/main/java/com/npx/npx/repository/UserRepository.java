package com.npx.npx.repository;

import com.npx.npx.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
	Optional<AppUser> findByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCase(String email);
	List<AppUser> findAllByOrderByNameAsc();
}
