package com.npx.npx.repository;

import com.npx.npx.domain.WorkTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<WorkTask, Long> {
	List<WorkTask> findAllByAssignedTo_IdOrderByDueDateAsc(Long userId);
	List<WorkTask> findAllByProject_IdOrderByDueDateAsc(Long projectId);
	List<WorkTask> findAllByOrderByDueDateAsc();
	Optional<WorkTask> findByIdAndProject_Participants_Id(Long id, Long userId);
	long countByStatus(String status);
	long countByStatusNot(String status);
	long countByStatusNotAndDueDateBefore(String status, java.time.LocalDate date);
	long countByProject_IdAndStatus(Long projectId, String status);
	long countByProject_Id(Long projectId);
}
