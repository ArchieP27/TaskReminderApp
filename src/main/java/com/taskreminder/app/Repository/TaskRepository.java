package com.taskreminder.app.Repository;

import com.taskreminder.app.Entity.Task;
import enums.TaskPriority;
import enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByPriority(TaskPriority priority, Pageable pageable);

    Page<Task> findByStatusAndPriority(
            TaskStatus status,
            TaskPriority priority,
            Pageable pageable
    );

    Page<Task> findByStatusAndTitleContainingIgnoreCase(
            TaskStatus status,
            String keyword,
            Pageable pageable
    );

    Page<Task> findByPriorityAndTitleContainingIgnoreCase(
            TaskPriority priority,
            String keyword,
            Pageable pageable
    );

    Page<Task> findByStatusAndPriorityAndTitleContainingIgnoreCase(
            TaskStatus status,
            TaskPriority priority,
            String keyword,
            Pageable pageable
    );

    Page<Task> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    List<Task> findByDueDateAndStatusNot(LocalDate date, TaskStatus status);
    List<Task> findByDueDateBetweenAndStatusNot(LocalDate start, LocalDate end, TaskStatus status);
    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);
}
