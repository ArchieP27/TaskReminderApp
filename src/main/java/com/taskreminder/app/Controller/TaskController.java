package com.taskreminder.app.Controller;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

//    @GetMapping("/tasks")
    @GetMapping
    public String listTasks(Model model){
        model.addAttribute("tasks",taskService.getAllTasks());
        return "tasks";
    }

    @GetMapping("/add")
    public String showAddForm(Model model){
        model.addAttribute("task",new Task());
        return "add-task";
    }
    @PostMapping("/add")
    public String saveTask(@ModelAttribute Task task){
        task.setId(TaskService.nextId());
        task.setCreatedAt(LocalDateTime.now());
        taskService.addTask(task);
        return "redirect:/api/tasks";
    }
}
