package com.taskreminder.app.Controller;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Service.TaskService;
import enums.TaskPriority;
import enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public String listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "table") String view,
            Model model
    ) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            switch (sort) {
                case "dueDate" -> sortObj = Sort.by("dueDate");
                case "priority" -> sortObj = Sort.by("priority");
                case "createdAt" -> sortObj = Sort.by("createdAt");
                case "title" -> sortObj = Sort.by("title");
            }
        }

        int pageSize;
        if ("card".equals(view)) {
            pageSize = 6;
        } else if (size != null && size >= 9999) {
            pageSize = Integer.MAX_VALUE;
            page = 0;
        } else if (size == null) {
            pageSize = 5;
        } else {
            pageSize = size;
        }

        if ("calendar".equals(view)) {
            List<Task> tasks = taskService.getAllTasks();
            model.addAttribute("tasks", tasks);

            model.addAttribute("view", view);
            model.addAttribute("size", pageSize);
            return "tasks";
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortObj);
        Page<Task> taskPage =
                taskService.getPagedTasks(pageable, status, priority, keyword);

        model.addAttribute("tasks", taskPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", taskPage.getTotalPages());
        model.addAttribute("size", pageSize);
        model.addAttribute("view", view);
        model.addAttribute("view", view);

        return "tasks";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("task", new Task());
        return "add-task";
    }

    @PostMapping("/add")
    public String saveTask(@ModelAttribute Task task, Model model, RedirectAttributes ra) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            model.addAttribute("errorMessage", "Title is Required!");
            model.addAttribute("task", task);
            return "add-task";
        }
        if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
            model.addAttribute("errorMessage", "Description is Required!");
            model.addAttribute("task", task);
            return "add-task";
        }
        if (task.getDueDate() == null) {
            model.addAttribute("errorMessage", "Due Date is Required!");
            model.addAttribute("task", task);
            return "add-task";
        }
        taskService.addTask(task);
        ra.addFlashAttribute("successMessage", "Task added successfully!");
        return "redirect:/api/tasks";
    }

    @GetMapping("/update/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Task task = taskService.findById(id).orElse(null);
        if (task == null) {
            return "redirect:/api/tasks";
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            ra.addFlashAttribute("errorMessage", "Completed tasks cannot be updated.");
            return "redirect:/api/tasks";
        }
        model.addAttribute("task", task);
        return "update-task";
    }


    @PostMapping("/update/{id}")
    public String updateTask(@PathVariable Integer id, @ModelAttribute Task task, Model model, RedirectAttributes ra) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            model.addAttribute("errorMessage", "Title is Required!");
            model.addAttribute("task", task);
            return "update-task";
        }
        if (task.getDueDate() == null) {
            model.addAttribute("errorMessage", "Due Date is Required!");
            model.addAttribute("task", task);
            return "update-task";
        }
        task.setId(id);
        Task existing = taskService.findById(id).orElse(null);
        if (existing != null)
            task.setCreatedAt(existing.getCreatedAt());
        else
            task.setCreatedAt(LocalDate.now());
        if(task.getStatus() == TaskStatus.COMPLETED)
            task.setCompletedAt(LocalDate.now());
        taskService.updateTask(task);
        ra.addFlashAttribute(
                "successMessage",
                "Task updated successfully!"
        );
        return "redirect:/api/tasks";
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        if (taskService.findById(id).isEmpty()) {
            model.addAttribute("errorMessage", "Task not found!");
            return "redirect:/api/tasks";
        }
        taskService.deleteTask(id);
        ra.addFlashAttribute(
                "successMessage",
                "Task deleted successfully!"
        );
        return "redirect:/api/tasks";
    }


    @GetMapping("/markAsDone/{id}")
    public String markAsDone(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            taskService.markTask(id);
            ra.addFlashAttribute(
                    "successMessage", "Task marked as completed!"
            );
        } catch (IllegalStateException e) {
            ra.addFlashAttribute(
                    "errorMessage", "⚠ " + e.getMessage()
            );
        }
        return "redirect:/api/tasks";
    }
}