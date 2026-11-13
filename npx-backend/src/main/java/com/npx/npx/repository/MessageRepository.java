package com.npx.npx.repository;

import com.npx.npx.domain.WorkspaceMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<WorkspaceMessage, Long> {
	List<WorkspaceMessage> findBySender_IdOrRecipient_IdOrderByCreatedAtAsc(Long senderId, Long recipientId);
	Optional<WorkspaceMessage> findByIdAndRecipient_Id(Long id, Long recipientId);
}
