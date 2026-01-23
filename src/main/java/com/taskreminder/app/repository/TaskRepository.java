package com.taskreminder.app.repository;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.enums.TaskPriority;
import com.taskreminder.app.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    List<Task> findByUser_Id(Integer userId);
    Page<Task> findByUser_IdAndStatus(Integer userId, TaskStatus status, Pageable pageable);
    List<Task> findByUser_IdAndStatus(Integer userId, TaskStatus status);

    Page<Task> findByUser_IdAndPriority(Integer userId, TaskPriority priority, Pageable pageable);

    Page<Task> findByUser_IdAndStatusAndPriority(
            Integer userId,
            TaskStatus status,
            TaskPriority priority,
            Pageable pageable
    );

    List<Task> findByUser_IdAndDueDateAndStatusNot(Integer userId, LocalDate date, TaskStatus status);

    List<Task> findByUser_IdAndDueDateBetweenAndStatusNot(
            Integer userId,
            LocalDate start,
            LocalDate end,
            TaskStatus status
    );

    List<Task> findByUser_IdAndDueDateBeforeAndStatusNot(
            Integer userId,
            LocalDate date,
            TaskStatus status
    );

    @Query("SELECT t FROM Task t WHERE t.reminderSent = false AND t.reminderTime IS NOT NULL AND t.reminderTime <= :now")
    List<Task> findTasksForReminder(@Param("now") LocalDateTime now);

    List<Task> findByUser_IdAndDeletedTrue(Integer userId);
    List<Task> findByUser_IdAndDeletedFalse(Integer userId);

    Page<Task> findByUser_IdAndDeletedFalse(Integer userId, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndStatus(Integer userId, TaskStatus status, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndPriority(Integer userId, TaskPriority priority, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndTitleContainingIgnoreCase(Integer userId, String keyword, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndStatusAndPriority(Integer userId, TaskStatus status, TaskPriority priority, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndStatusAndTitleContainingIgnoreCase(Integer userId, TaskStatus status, String keyword, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndPriorityAndTitleContainingIgnoreCase(Integer userId, TaskPriority priority, String keyword, Pageable pageable);

    Page<Task> findByUser_IdAndDeletedFalseAndStatusAndPriorityAndTitleContainingIgnoreCase(Integer userId, TaskStatus status, TaskPriority priority, String keyword, Pageable pageable);

    List<Task> findByUser_IdAndDeletedFalseAndDueDateAndStatusNot(Integer userId, LocalDate dueDate, TaskStatus status);

    List<Task> findByUser_IdAndDeletedFalseAndDueDateBetweenAndStatusNot(Integer userId, LocalDate start, LocalDate end, TaskStatus status);

    List<Task> findByUser_IdAndDeletedFalseAndDueDateBeforeAndStatusNot(Integer userId, LocalDate date, TaskStatus status);

    List<Task> findByUser_IdAndDeletedFalseAndStatus(Integer userId, TaskStatus status);

    Optional<Task> findByIdAndUserIdAndDeletedTrue(Integer id, Integer userId);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.deleted = false AND t.reminderSent = false AND t.reminderTime BETWEEN :now AND :future")
    List<Task> findUpcomingRemindersForUser(@Param("userId") Integer userId,
                                            @Param("now") LocalDateTime now,
                                            @Param("future") LocalDateTime future);

    @Query("SELECT t FROM Task t WHERE t.id = :taskId AND t.user.id = :userId")
    Optional<Task> findByIdAndUserIdIncludingDeleted(@Param("taskId") Integer taskId, @Param("userId") Integer userId);

}
