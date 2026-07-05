package com.example.TaskManagement.repository;

import com.example.TaskManagement.model.Task;
import com.example.TaskManagement.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByAssignedToId(Long assignedToId, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByAssignedToIdAndStatus(Long assignedToId, TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Task> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByKeywordAndAssignedTo(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") TaskStatus status,
            Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByKeywordStatusAndAssignedTo(
            @Param("keyword") String keyword,
            @Param("status") TaskStatus status,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT t FROM Task t JOIN FETCH t.assignedTo JOIN FETCH t.createdBy " +
           "WHERE t.status = :status AND t.deadline IS NOT NULL " +
           "AND t.reminderSent = false AND t.deadline <= :threshold")
    List<Task> findTasksNeedingReminder(
            @Param("status") TaskStatus status,
            @Param("threshold") LocalDateTime threshold);

    boolean existsByCategoryId(Long categoryId);
}
