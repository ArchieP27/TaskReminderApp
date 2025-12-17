package com.taskreminder.app.Controller;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Service.TaskService;
import enums.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public String listTasks(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sort,
        Model model) {

        List<Task> tasks = taskService.getFilteredTasks(status, priority, keyword, sort);

        model.addAttribute("tasks", tasks);
        return "tasks";
    }


    @GetMapping("/add")
    public String showAddForm(Model model){
        model.addAttribute("task",new Task());
        return "add-task";
    }
    @PostMapping("/add")
    public String saveTask(@ModelAttribute Task task, Model model, RedirectAttributes ra){
        if(task.getTitle()==null || task.getTitle().trim().isEmpty()){
            model.addAttribute("errorMessage","Title is Required!");
            model.addAttribute("task",task);
            return "add-task";
        }
        if(task.getDescription()==null || task.getDescription().trim().isEmpty()){
            model.addAttribute("errorMessage","Description is Required!");
            model.addAttribute("task",task);
            return "add-task";
        }
        if(task.getDueDate()==null){
            model.addAttribute("errorMessage","Due Date is Required!");
            model.addAttribute("task",task);
            return "add-task";
        }
        taskService.addTask(task);
        ra.addFlashAttribute("successMessage","Task added successfully!");
        return "redirect:/api/tasks";
    }

    @GetMapping("/update/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Task task = taskService.findById(id).orElse(null);
        if (task == null) {
            return "redirect:/api/tasks";
        }
        if (task.getStatus()== TaskStatus.COMPLETED) {
            ra.addFlashAttribute("errorMessage", "Completed tasks cannot be updated.");
            return "redirect:/api/tasks";
        }
        model.addAttribute("task", task);
        return "update-task";
    }


    @PostMapping("/update/{id}")
    public String updateTask(@PathVariable Integer id, @ModelAttribute Task task, Model model, RedirectAttributes ra){
        if(task.getTitle()==null || task.getTitle().trim().isEmpty()){
            model.addAttribute("errorMessage","Title is Required!");
            model.addAttribute("task",task);
            return "update-task";
        }
        if(task.getDueDate()==null){
            model.addAttribute("errorMessage","Due Date is Required!");
            model.addAttribute("task",task);
            return "update-task";
        }
        task.setId(id);
        Task existing = taskService.findById(id).orElse(null);
        if(existing!=null)
            task.setCreatedAt(existing.getCreatedAt());
        else
            task.setCreatedAt(LocalDate.now());
        taskService.updateTask(task);
        ra.addFlashAttribute(
                "successMessage",
                "Task updated successfully!"
        );
        return "redirect:/api/tasks";
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable Integer id){
        taskService.deleteTask(id);
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
