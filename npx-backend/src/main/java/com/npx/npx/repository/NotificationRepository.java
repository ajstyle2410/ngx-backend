package com.npx.npx.repository;

import com.npx.npx.domain.WorkspaceNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<WorkspaceNotification, Long> {
	List<WorkspaceNotification> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
	Optional<WorkspaceNotification> findByIdAndUser_Id(Long id, Long userId);
}
