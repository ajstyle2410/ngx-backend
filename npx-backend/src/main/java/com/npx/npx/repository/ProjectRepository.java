package com.npx.npx.repository;

import com.npx.npx.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	List<Project> findDistinctByParticipants_IdOrderByCreatedAtDesc(Long userId);
	List<Project> findAllByOrderByCreatedAtDesc();
}
