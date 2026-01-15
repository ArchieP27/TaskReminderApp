package com.taskreminder.app.controller;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.service.TaskService;
import enums.TaskStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public String openDashboard(HttpSession session, Model model){
        Integer userId = (Integer) session.getAttribute("userId");
        if(userId==null)
            return "redirect:/auth/login";
        model.addAttribute("activePage","dashboard");
        return "dashboard";
    }

    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getStats(HttpSession session){
        Integer userId = (Integer) session.getAttribute("userId");

        List<Task> allTasks = taskService.getAllTasksByUser(userId);

        Map<String,Object> stats = new HashMap<>();
        stats.put("total",allTasks);
        stats.put("completed",allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).toList());
        stats.put("pending", allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).toList());
        stats.put("overdue",taskService.getOverdueTasks(userId));



        return stats;
    }
}
