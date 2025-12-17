package com.taskreminder.app.Controller;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskRestController {

    private final TaskService taskService;

    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    // GET all
    @GetMapping
    public ResponseEntity<Page<Task>> getAll(Pageable pageable) {
        return ResponseEntity.ok(taskService.findAll(pageable));
    }

    // GET by ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getById(@PathVariable Integer id) {
        return taskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Task> add(@RequestBody Task task) {
        task.setCreatedAt(LocalDate.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.addTask(task));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Integer id, @RequestBody Task task) {
        return taskService.findById(id)
                .map(existing -> {
                    task.setId(id);
                    task.setCreatedAt(existing.getCreatedAt());
                    return ResponseEntity.ok(taskService.updateTask(task));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (taskService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // FILTERS
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Task>> getByPriority(@PathVariable String priority) {
        return ResponseEntity.ok(taskService.findByPriority(priority));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Task>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(taskService.searchByTitle(keyword));
    }

    @GetMapping("/due")
    public ResponseEntity<List<Task>> getByDueDate(@RequestParam String date) {
        return ResponseEntity.ok(taskService.findByDueDate(date));
    }

    // MARK AS DONE
    @PatchMapping("/{id}/done")
    public ResponseEntity<Void> markAsDone(@PathVariable Integer id) {
        if (taskService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        taskService.markTask(id);
        return ResponseEntity.ok().build();
    }
}
