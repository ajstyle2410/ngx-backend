package com.npx.npx.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_logs")
public class TimeLog {
public TimeLog() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private WorkTask task;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "developer_id", nullable = false)
	private AppUser developer;
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal hours;
	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;
	@Column(length = 255)
	private String note;
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public WorkTask getTask() {
		return task;
	}
	public void setTask(WorkTask task) {
		this.task = task;
	}
	public AppUser getDeveloper() {
		return developer;
	}
	public void setDeveloper(AppUser developer) {
		this.developer = developer;
	}
	public BigDecimal getHours() {
		return hours;
	}
	public void setHours(BigDecimal hours) {
		this.hours = hours;
	}
	public LocalDate getWorkDate() {
		return workDate;
	}
	public void setWorkDate(LocalDate workDate) {
		this.workDate = workDate;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	
}
