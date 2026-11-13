package com.npx.npx.repository;

import com.npx.npx.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
	@Query("select coalesce(sum(t.hours), 0) from TimeLog t where t.task.project.id = :projectId")
	BigDecimal totalHoursForProject(@Param("projectId") Long projectId);

	@Query("select coalesce(sum(t.hours), 0) from TimeLog t where t.task.id = :taskId")
	BigDecimal totalHoursForTask(@Param("taskId") Long taskId);

	@Query("select t from TimeLog t where t.developer.id = :userId order by t.workDate desc, t.createdAt desc")
	List<TimeLog> findRecentForDeveloper(@Param("userId") Long userId);

	@Query("select t from TimeLog t order by t.workDate desc, t.createdAt desc")
	List<TimeLog> findAllRecent();
}
