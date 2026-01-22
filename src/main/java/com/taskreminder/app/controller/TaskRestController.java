package com.taskreminder.app.controller;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.enums.TaskPriority;
import com.taskreminder.app.enums.TaskStatus;
import com.taskreminder.app.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskRestController {

    private final TaskService taskService;

    @Autowired
    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Task task,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Task savedTask = taskService.addTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable Integer id,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");

        Task task = taskService.findByIdAndUserId(id, userId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<Page<Task>> getPagedTasks(
            HttpSession session,
            Pageable pageable,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String keyword
    ) {
        Integer userId = (Integer) session.getAttribute("userId");

        Page<Task> page = taskService.getPagedTasks(
                userId, pageable, status, priority, keyword
        );

        return ResponseEntity.ok(page);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Task>> getAllTasksByUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getAllTasksByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Integer id,
            @RequestBody Task task,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        task.setId(id);

        Task updated = taskService.updateTask(task, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Integer id,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> markTaskCompleted(
            @PathVariable Integer id,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        taskService.markTask(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/due-today")
    public ResponseEntity<List<Task>> getTasksDueToday(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getTasksDueToday(userId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Task>> getUpcomingTasks(
            HttpSession session,
            @RequestParam(defaultValue = "3") int days
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getUpcomingTasks(userId, days));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getOverdueTasks(userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Task>> getRecentTasks(
            HttpSession session,
            @RequestParam(defaultValue = "5") int limit
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getRecentTasks(userId, limit));
    }

    @GetMapping("/priority/high")
    public ResponseEntity<List<Task>> getHighPriorityTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getHighPriorityTasks(userId));
    }

    @GetMapping("/priority/medium")
    public ResponseEntity<List<Task>> getMediumPriorityTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getMediumPriorityTasks(userId));
    }

    @GetMapping("/priority/low")
    public ResponseEntity<List<Task>> getLowPriorityTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getLowPriorityTasks(userId));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Task>> getCompletedTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getCompletedTasks(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Task>> getPendingTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getPendingTasks(userId));
    }

    @GetMapping("/in-progress")
    public ResponseEntity<List<Task>> getInProgressTasks(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        return ResponseEntity.ok(taskService.getInProgressTasks(userId));
    }
}
