package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Repository.TaskRepository;
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

    public List<Task> getFilteredTasks(String status, String priority, String keyword, String sort) {

        List<Task> tasks = taskRepository.findAll();

        if (status != null && !status.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        if (priority != null && !priority.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> t.getPriority().name().equalsIgnoreCase(priority))
                    .toList();
        }
        if (keyword != null && !keyword.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }

        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "dueDate" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getDueDate))
                        .toList();
                case "priority" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getPriority))
                        .toList();
                case "createdAt" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getCreatedAt))
                        .toList();
                case "title" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getTitle))
                        .toList();
            }
        }

        return tasks;
    }

    public List<Task> findByStatus(String status){
        return taskRepository.findByStatus(status);
    }

    public List<Task> findByPriority(String priority){
        return taskRepository.findByPriority(priority);
    }

    public List<Task> findByDueDate(String dueDate){
        return taskRepository.findByDueDate(dueDate);
    }

    public List<Task> searchByTitle(String keyword){
        return taskRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }
}
