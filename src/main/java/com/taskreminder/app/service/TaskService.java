package com.taskreminder.app.service;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.repository.TaskRepository;
import com.taskreminder.app.enums.TaskPriority;
import com.taskreminder.app.enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserService userService;

    public Task addTask(Task task) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        task.setCreatedAt(LocalDate.now());
        return taskRepository.save(task);
    }


    public Task findByIdAndUserId(Integer taskId, Integer userId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getUser().getId().equals(userId) && Boolean.FALSE.equals(task.isDeleted()))
                .orElse(null);
    }

    public Task updateTask(Task task, Integer userId) {
        Task existing = findByIdAndUserId(task.getId(), userId);
        if (existing == null) {
            throw new RuntimeException("Task not found");
        }
        task.setUser(existing.getUser());
        task.setCreatedAt(existing.getCreatedAt());
        task.setDeleted(existing.isDeleted());

        if (task.getStatus() == TaskStatus.COMPLETED && existing.getStatus() != TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDate.now());
        } else {
            task.setCompletedAt(existing.getCompletedAt());
        }

        return taskRepository.save(task);
    }

    public void moveToTrash(Integer id, Integer userId) {
        Task task = taskRepository.findByIdAndUserIdIncludingDeleted(id, userId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found or you do not have permission"));

        if (Boolean.TRUE.equals(task.isDeleted())) {
            throw new IllegalStateException("Task is already in Trash");
        }

        task.setDeleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    public void restoreTask(Integer id, Integer userId) {
        Task task = taskRepository.findByIdAndUserIdIncludingDeleted(id, userId)
                .orElseThrow(() -> new RuntimeException("Task not found or you do not have permission"));

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setUser(user);
        task.setDeleted(false);
        task.setDeletedAt(null);
        taskRepository.save(task);
    }

    public void permanentDelete(Integer id, Integer userId) {
        Task task = taskRepository
                .findByIdAndUserIdAndDeletedTrue(id, userId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found or you do not have permission"));

        taskRepository.delete(task);
    }

    public List<Task> getTrashedTasks(Integer userId) {
        return taskRepository.findByUser_IdAndDeletedTrue(userId);
    }

    public void emptyTrash(Integer userId) {
        List<Task> trashedTasks = taskRepository.findByUser_IdAndDeletedTrue(userId);

        if (trashedTasks.isEmpty()) {
            throw new RuntimeException("Trash is already empty");
        }

        taskRepository.deleteAll(trashedTasks);
    }

    public void markTask(Integer id, Integer userId) {
        Task task = findByIdAndUserId(id, userId);
        if (task == null) {
            throw new RuntimeException("Task not found or you do not have permission");
        }

        if (Boolean.TRUE.equals(task.isDeleted())) {
            throw new IllegalStateException("Cannot complete a task in Trash");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task already completed");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDate.now());
        taskRepository.save(task);
    }

    public Page<Task> getPagedTasks(Integer userId, Pageable pageable, TaskStatus status, TaskPriority priority, String keyword) {
        boolean hasStatus = status != null;
        boolean hasPriority = priority != null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasStatus && hasPriority && hasKeyword) {
            return taskRepository.findByUser_IdAndDeletedFalseAndStatusAndPriorityAndTitleContainingIgnoreCase(
                    userId, status, priority, keyword, pageable);
        }

        if (hasStatus && hasPriority) {
            return taskRepository.findByUser_IdAndDeletedFalseAndStatusAndPriority(userId, status, priority, pageable);
        }

        if (hasStatus && hasKeyword) {
            return taskRepository.findByUser_IdAndDeletedFalseAndStatusAndTitleContainingIgnoreCase(userId, status, keyword, pageable);
        }

        if (hasPriority && hasKeyword) {
            return taskRepository.findByUser_IdAndDeletedFalseAndPriorityAndTitleContainingIgnoreCase(userId, priority, keyword, pageable);
        }

        if (hasStatus) {
            return taskRepository.findByUser_IdAndDeletedFalseAndStatus(userId, status, pageable);
        }

        if (hasPriority) {
            return taskRepository.findByUser_IdAndDeletedFalseAndPriority(userId, priority, pageable);
        }

        if (hasKeyword) {
            return taskRepository.findByUser_IdAndDeletedFalseAndTitleContainingIgnoreCase(userId, keyword, pageable);
        }

        return taskRepository.findByUser_IdAndDeletedFalse(userId, pageable);
    }

    public List<Task> getAllTasksByUser(Integer userId) {
        return taskRepository.findByUser_IdAndDeletedFalse(userId);
    }

    public List<Task> getTasksDueToday(Integer userId) {
        LocalDate today = LocalDate.now();
        return taskRepository.findByUser_IdAndDeletedFalseAndDueDateAndStatusNot(userId, today, TaskStatus.COMPLETED);
    }

    public List<Task> getUpcomingTasks(Integer userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return taskRepository.findByUser_IdAndDeletedFalseAndDueDateBetweenAndStatusNot(userId, today, end, TaskStatus.COMPLETED);
    }

    public List<Task> getOverdueTasks(Integer userId) {
        LocalDate today = LocalDate.now();
        return taskRepository.findByUser_IdAndDeletedFalseAndDueDateBeforeAndStatusNot(userId, today, TaskStatus.COMPLETED);
    }

    public List<Task> getUpcomingReminders(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(24);
        return taskRepository.findUpcomingRemindersForUser(userId, now, future)
                .stream()
                .filter(task -> Boolean.FALSE.equals(task.isDeleted()))
                .toList();
    }

    public List<Task> getRecentTasks(Integer userId, int limit) {
        return taskRepository.findByUser_IdAndDeletedFalse(userId)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    public List<Task> getHighPriorityTasks(Integer userId) {
        return taskRepository
                .findByUser_IdAndDeletedFalseAndPriority(userId, TaskPriority.HIGH, PageRequest.of(0, 10))
                .getContent();
    }

    public List<Task> getMediumPriorityTasks(Integer userId) {
        return taskRepository
                .findByUser_IdAndDeletedFalseAndPriority(userId, TaskPriority.MEDIUM, PageRequest.of(0, 10))
                .getContent();
    }

    public List<Task> getLowPriorityTasks(Integer userId) {
        return taskRepository
                .findByUser_IdAndDeletedFalseAndPriority(userId, TaskPriority.LOW, PageRequest.of(0, 10))
                .getContent();
    }

    public List<Task> getCompletedTasks(Integer userId) {
        return taskRepository
                .findByUser_IdAndDeletedFalseAndStatus(userId, TaskStatus.COMPLETED, PageRequest.of(0, 20))
                .getContent();
    }

    public List<Task> getPendingTasks(Integer userId) {
        return taskRepository.findByUser_IdAndDeletedFalseAndStatus(userId, TaskStatus.PENDING);
    }

    public List<Task> getInProgressTasks(Integer userId) {
        return taskRepository.findByUser_IdAndDeletedFalseAndStatus(userId, TaskStatus.IN_PROGRESS);
    }


}
