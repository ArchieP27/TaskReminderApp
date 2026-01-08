package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Repository.TaskRepository;
import enums.TaskPriority;
import enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

        public Task addTask(Task task) {
            return taskRepository.save(task);
        }

        public Task findByIdAndUserId(Integer taskId, Integer userId) {
            return taskRepository.findById(taskId)
                    .filter(task -> task.getUser().getId().equals(userId))
                    .orElse(null);
        }

        public Task updateTask(Task task, Integer userId) {
            Task existing = findByIdAndUserId(task.getId(), userId);
            if (existing == null) {
                throw new RuntimeException("Task not found");
            }
            task.setUser(existing.getUser());
            task.setCreatedAt(existing.getCreatedAt());
            return taskRepository.save(task);
        }

        public void deleteTask(Integer id, Integer userId) {
            Task task = findByIdAndUserId(id, userId);
            if (task == null) {
                throw new RuntimeException("Task not found");
            }
            taskRepository.delete(task);
        }

        public void markTask(Integer id, Integer userId) {
            Task task = findByIdAndUserId(id, userId);
            if (task == null) {
                throw new RuntimeException("Task not found");
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
            return taskRepository.findByUser_IdAndStatusAndPriorityAndTitleContainingIgnoreCase(
                    userId, status, priority, keyword, pageable);
        }

        if (hasStatus && hasPriority) {
            return taskRepository.findByUser_IdAndStatusAndPriority(userId, status, priority, pageable);
        }

        if (hasStatus && hasKeyword) {
            return taskRepository.findByUser_IdAndStatusAndTitleContainingIgnoreCase(userId, status, keyword, pageable);
        }

        if (hasPriority && hasKeyword) {
            return taskRepository.findByUser_IdAndPriorityAndTitleContainingIgnoreCase(userId, priority, keyword, pageable);
        }

        if (hasStatus) {
            return taskRepository.findByUser_IdAndStatus(userId, status, pageable);
        }

        if (hasPriority) {
            return taskRepository.findByUser_IdAndPriority(userId, priority, pageable);
        }

        if (hasKeyword) {
            return taskRepository.findByUser_IdAndTitleContainingIgnoreCase(userId, keyword, pageable);
        }

        return taskRepository.findByUser_Id(userId, pageable);
    }

    public List<Task> getAllTasksByUser(Integer userId) {
        return taskRepository.findByUser_Id(userId);
    }

    public List<Task> getTasksDueToday(Integer userId) {
        LocalDate today = LocalDate.now();
        return taskRepository.findByUser_IdAndDueDateAndStatusNot(userId, today, TaskStatus.COMPLETED);
    }

    public List<Task> getUpcomingTasks(Integer userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return taskRepository.findByUser_IdAndDueDateBetweenAndStatusNot(userId, today, end, TaskStatus.COMPLETED);
    }

    public List<Task> getOverdueTasks(Integer userId) {
        LocalDate today = LocalDate.now();
        return taskRepository.findByUser_IdAndDueDateBeforeAndStatusNot(userId, today, TaskStatus.COMPLETED);
    }
}
