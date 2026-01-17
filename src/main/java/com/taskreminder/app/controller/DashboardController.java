package com.taskreminder.app.controller;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.service.TaskService;
import com.taskreminder.app.enums.TaskStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

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
    public Map<String, Object> getDashboardStats(HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");

        List<Task> allTasks = taskService.getAllTasksByUser(userId);

        long completedCount = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        long pendingCount = taskService.getPendingTasks(userId).size();
        long inProgressCount = taskService.getInProgressTasks(userId).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", allTasks.size());
        stats.put("completedCount", completedCount);
        stats.put("pendingCount", pendingCount);
        stats.put("inProgressCount", inProgressCount);
        stats.put("overdueCount", taskService.getOverdueTasks(userId).size());

        stats.put("recentTasks", taskService.getRecentTasks(userId, 5));
        stats.put("dueTodayTasks", taskService.getTasksDueToday(userId));
        stats.put("highPriorityTasks", taskService.getHighPriorityTasks(userId));
        stats.put("mediumPriorityTasks", taskService.getMediumPriorityTasks(userId));
        stats.put("lowPriorityTasks", taskService.getLowPriorityTasks(userId));

        double completionRate = allTasks.isEmpty()
                ? 0
                : (completedCount * 100.0) / allTasks.size();
        stats.put("completionRate", Math.round(completionRate));

        return stats;
    }

    @GetMapping("/tasks")
    @ResponseBody
    public List<Task> getTasksForModal(
            HttpSession session,
            @RequestParam String type) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");

        return switch (type.toLowerCase()) {
            case "completed" -> taskService.getCompletedTasks(userId);
            case "pending" -> taskService.getPendingTasks(userId);
            case "inprogress", "in_progress" -> taskService.getInProgressTasks(userId);
            case "overdue" -> taskService.getOverdueTasks(userId);
            case "today" -> taskService.getTasksDueToday(userId);
            case "high" -> taskService.getHighPriorityTasks(userId);
            case "medium" -> taskService.getMediumPriorityTasks(userId);
            case "low" -> taskService.getLowPriorityTasks(userId);
            case "recent" -> taskService.getRecentTasks(userId, 20);
            default -> throw new ResponseStatusException(
                    BAD_REQUEST, "Invalid task type"
            );
        };
    }

}
