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
import java.util.*;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getAllTasks(){
        return taskRepository.findAll();
    }

    public Task addTask(Task  task){
        return taskRepository.save(task);
    }

    public Task updateTask(Task  task){
        return taskRepository.save(task);
    }

    public void deleteTask(Integer id){
        taskRepository.deleteById(id);
    }

    public Optional<Task> findById(Integer id){
        return taskRepository.findById(id);
    }

    public void markTask(Integer id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus().equals(TaskStatus.COMPLETED)) {
            throw new IllegalStateException("Task is already completed and cannot be marked again.");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDate.now());
        taskRepository.save(task);
    }

    public Page<Task> getPagedTasks(
            Pageable pageable,
            TaskStatus status,
            TaskPriority priority,
            String keyword
    ) {

        boolean hasStatus = status != null;
        boolean hasPriority = priority != null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasStatus && hasPriority && hasKeyword) {
            return taskRepository
                    .findByStatusAndPriorityAndTitleContainingIgnoreCase(
                            status, priority, keyword, pageable);
        }

        if (hasStatus && hasPriority) {
            return taskRepository
                    .findByStatusAndPriority(status, priority, pageable);
        }

        if (hasStatus && hasKeyword) {
            return taskRepository
                    .findByStatusAndTitleContainingIgnoreCase(
                            status, keyword, pageable);
        }

        if (hasPriority && hasKeyword) {
            return taskRepository
                    .findByPriorityAndTitleContainingIgnoreCase(
                            priority, keyword, pageable);
        }

        if (hasStatus) {
            return taskRepository.findByStatus(status, pageable);
        }

        if (hasPriority) {
            return taskRepository.findByPriority(priority, pageable);
        }

        if (hasKeyword) {
            return taskRepository
                    .findByTitleContainingIgnoreCase(keyword, pageable);
        }

        return taskRepository.findAll(pageable);
    }

    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    public List<Task> getTasksDueToday() {
        LocalDate today = LocalDate.now();
        return taskRepository.findByDueDateAndStatusNot(today,TaskStatus.COMPLETED);
    }

    public List<Task> getUpcomingTasks(int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return taskRepository.findByDueDateBetweenAndStatusNot(today,end,TaskStatus.COMPLETED);
    }

    public List<Task> getOverdueTasks() {
        LocalDate today = LocalDate.now();
        return taskRepository.findByDueDateBeforeAndStatusNot(today,TaskStatus.COMPLETED);
    }
}
