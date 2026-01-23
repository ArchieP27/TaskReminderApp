package com.taskreminder.app.controller;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.service.EmailService;
import com.taskreminder.app.service.TaskService;
import com.taskreminder.app.enums.TaskPriority;
import com.taskreminder.app.enums.TaskStatus;
import com.taskreminder.app.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "table") String view,
            Model model,
            HttpSession session
    ) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/auth/login";

        if (page < 0) page = 0;
        if (size != null && size <= 0) size = 5;

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

        List<Task> allTasks = taskService.getAllTasksByUser(userId);

        if ("calendar".equals(view)) {

            if (allTasks.isEmpty()) {
                model.addAttribute("errorMessage", "No tasks available to display on the calendar.");
                model.addAttribute("successMessage", null);
            }

            model.addAttribute("tasks", allTasks);
            model.addAttribute("overdueTasks", taskService.getOverdueTasks(userId));
            model.addAttribute("todayTasks", taskService.getTasksDueToday(userId));
            model.addAttribute("upcomingTasks", taskService.getUpcomingTasks(userId, 7));
            model.addAttribute("completedTasks",
                    allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).toList());
            model.addAttribute("pendingTasks",
                    allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).toList());
            model.addAttribute("allTasks", allTasks);

            model.addAttribute("view", view);
            model.addAttribute("size", pageSize);
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "tasks");
            model.addAttribute("upcomingReminders", taskService.getUpcomingReminders(userId));

            return "tasks";
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortObj);

        Page<Task> taskPage;
        try {
            taskPage = taskService.getPagedTasks(userId, pageable, status, priority, keyword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid filter value provided.");
            model.addAttribute("tasks", List.of());
            model.addAttribute("successMessage", null);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("size", pageSize);
            model.addAttribute("view", view);
            model.addAttribute("status", null);
            model.addAttribute("priority", null);
            model.addAttribute("keyword", keyword);
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "tasks");
            return "tasks";
        }

        model.addAttribute("tasks", taskPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", taskPage.getTotalPages());
        model.addAttribute("size", pageSize);
        model.addAttribute("view", view);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("keyword", keyword);
        model.addAttribute("overdueTasks", taskService.getOverdueTasks(userId));
        model.addAttribute("todayTasks", taskService.getTasksDueToday(userId));
        model.addAttribute("upcomingTasks", taskService.getUpcomingTasks(userId, 7));
        model.addAttribute("completedTasks",
                allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).toList());
        model.addAttribute("pendingTasks",
                allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).toList());
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("userName", session.getAttribute("name"));
        model.addAttribute("activePage", "tasks");
        model.addAttribute("upcomingReminders", taskService.getUpcomingReminders(userId));

        boolean noTasksAtAll = allTasks.isEmpty();
        boolean noResultsAfterFilter = taskPage.isEmpty() && !noTasksAtAll;
        boolean pageOutOfRange = page >= taskPage.getTotalPages() && taskPage.getTotalPages() > 0;

        if (noTasksAtAll) {
            model.addAttribute("errorMessage", "You don’t have any tasks yet.");
            model.addAttribute("successMessage", null);
        } else if (pageOutOfRange) {
            model.addAttribute("errorMessage", "The page you requested does not exist.");
            model.addAttribute("successMessage", null);
        } else if (noResultsAfterFilter) {
            model.addAttribute("errorMessage", "No tasks match your current filters.");
            model.addAttribute("successMessage", null);
        }

        return "tasks";
    }

    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/auth/login";
            }

            if (!model.containsAttribute("task")) {
                model.addAttribute("task", new Task());
            }

            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "add");
            model.addAttribute("errorMessage", null);

            return "add-task";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Unable to load the Add Task form. Please try again.");
            model.addAttribute("successMessage", null);
            model.addAttribute("task", new Task());
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "add");
            return "add-task";
        }
    }


    @PostMapping("/add")
    public String saveTask(
            @ModelAttribute Task task,
            Model model,
            RedirectAttributes ra,
            HttpSession session
    ) {
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
            }
            if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Title is required!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "add");
                return "add-task";
            }

            if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Description is required!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "add");
                return "add-task";
            }

            if (task.getDueDate() == null) {
                model.addAttribute("errorMessage", "Due Date is required!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "add");
                return "add-task";
            }

            User user = new User();
            user.setId(userId);
            task.setUser(user);

            LocalDate today = LocalDate.now();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (task.getDueDate().isBefore(today)) {
                model.addAttribute("errorMessage", "Due Date cannot be in the past!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "add");
                return "add-task";
            }
            if (Boolean.TRUE.equals(task.getReminderSent())) {
                if (task.getReminderTime() == null) {
                    model.addAttribute("errorMessage", "Please select a reminder time.");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));
                    model.addAttribute("activePage", "add");
                    return "add-task";
                }
                if (task.getReminderTime().isBefore(now)) {
                    model.addAttribute("errorMessage", "Reminder time cannot be in the past!");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));
                    model.addAttribute("activePage", "add");
                    return "add-task";
                }
                if (task.getReminderTime().toLocalDate().isAfter(task.getDueDate())) {
                    model.addAttribute("errorMessage", "Reminder cannot be set for a date after the Due Date!");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));
                    model.addAttribute("activePage", "add");
                    return "add-task";
                }
                task.setReminderSent(false);
                task.setReminderTime(task.getReminderTime().withSecond(0).withNano(0));
            } else {
                task.setReminderSent(false);
                task.setReminderTime(null);
            }

            taskService.addTask(task);


            ra.addFlashAttribute("successMessage", "Task added successfully!");
            return "redirect:/api/tasks";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to add task. Please try again.");
            model.addAttribute("successMessage", null);
            model.addAttribute("task", task != null ? task : new Task());
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "add");
            return "add-task";
        }
    }


    @GetMapping("/update/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes ra,
            HttpSession session
    ) {
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/auth/login";
            }

            Task task = taskService.findByIdAndUserId(id, userId);
            if (task == null) {
                ra.addFlashAttribute("errorMessage", "Task not found.");
                return "redirect:/api/tasks";
            }

            if (task.getStatus() == TaskStatus.COMPLETED) {
                ra.addFlashAttribute("errorMessage", "Completed tasks cannot be updated.");
                return "redirect:/api/tasks";
            }

            model.addAttribute("task", task);
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "edit");

            model.addAttribute("successMessage", null);

            return "update-task";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Unable to load the task for editing. Please try again.");
            return "redirect:/api/tasks";
        }
    }

    @PostMapping("/update/{id}")
    public String updateTask(
            @PathVariable Integer id,
            @ModelAttribute Task task,
            BindingResult result,
            Model model,
            RedirectAttributes ra,
            HttpSession session
    ) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Invalid input. Please check all fields.");
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "edit");
            return "update-task";
        }

        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/auth/login";
            }

            if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Title is required!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "edit");
                return "update-task";
            }

            if (task.getDueDate() == null) {
                model.addAttribute("errorMessage", "Due Date is required!");
                model.addAttribute("task", task);
                model.addAttribute("userName", session.getAttribute("name"));
                model.addAttribute("activePage", "edit");
                return "update-task";
            }

            Task existingTask = taskService.findByIdAndUserId(id, userId);
            if (existingTask == null) {
                ra.addFlashAttribute("errorMessage", "Task not found or you do not have permission to edit it.");
                return "redirect:/api/tasks";
            }

            if (existingTask.getStatus() == TaskStatus.COMPLETED) {
                ra.addFlashAttribute("errorMessage", "Completed tasks cannot be updated.");
                return "redirect:/api/tasks";
            }

            task.setId(id);
            task.setUser(existingTask.getUser());
            task.setCreatedAt(existingTask.getCreatedAt());
            if (task.getStatus() == TaskStatus.COMPLETED && existingTask.getStatus() != TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDate.now());
            } else {
                task.setCompletedAt(existingTask.getCompletedAt());
            }

            LocalDate today = LocalDate.now();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            if (Boolean.TRUE.equals(task.getReminderSent())) {
                if (task.getReminderTime() == null) {
                    model.addAttribute("errorMessage", "Please select a reminder time.");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));
                    model.addAttribute("activePage", "edit");
                    return "update-task";
                }

                if (task.getReminderTime().isBefore(now)) {
                    model.addAttribute("errorMessage", "Reminder time cannot be in the past!");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));
                    model.addAttribute("activePage", "edit");
                    return "update-task";
                }

                if (task.getReminderTime().toLocalDate().isAfter(task.getDueDate())) {
                    model.addAttribute("errorMessage", "Reminder cannot be set for a date after the Due Date!");
                    model.addAttribute("task", task);
                    model.addAttribute("userName", session.getAttribute("name"));

                    model.addAttribute("activePage", "edit");
                    return "update-task";
                }
                task.setReminderSent(false);
                task.setReminderTime(task.getReminderTime().withSecond(0).withNano(0));
            } else {
                task.setReminderSent(false);

                task.setReminderTime(null);
            }
            taskService.updateTask(task, userId);


            ra.addFlashAttribute("successMessage", "Task updated successfully!");
            return "redirect:/api/tasks";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to update the task. Please try again.");
            model.addAttribute("successMessage", null);
            model.addAttribute("task", task);
            model.addAttribute("userName", session.getAttribute("name"));
            model.addAttribute("activePage", "edit");
            return "update-task";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(
            @PathVariable Integer id,
            RedirectAttributes ra,
            HttpSession session
    ) {
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/auth/login";
            }

            Task task = taskService.findByIdAndUserId(id, userId);
            if (task == null) {
                ra.addFlashAttribute("errorMessage", "Task not found or you do not have permission to delete it.");
                return "redirect:/api/tasks";
            }


            taskService.moveToTrash(id, userId);
            ra.addFlashAttribute("successMessage", "Task moved to Trash!");
            return "redirect:/api/tasks";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete the task. Please try again.");
            return "redirect:/api/tasks";
        }
    }

    @GetMapping("/trash")
    public String viewTrash(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/auth/login";

        List<Task> trashedTasks = taskService.getTrashedTasks(userId);

        model.addAttribute("tasks", trashedTasks);
        model.addAttribute("userName", session.getAttribute("name"));
        model.addAttribute("activePage", "trash");

        if (trashedTasks.isEmpty()) {
            model.addAttribute("errorMessage", "Trash is empty.");
        }

        return "trash";
    }

    @GetMapping("/restore/{id}")

    public String restoreTask(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/auth/login";

        taskService.restoreTask(id, userId);
        ra.addFlashAttribute("successMessage","Task Restored Successfully!");
        return "redirect:/api/tasks/trash";
    }

    @GetMapping("/permanent-delete/{id}")
    public String permanentDelete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/auth/login";

        taskService.permanentDelete(id, userId);
        ra.addFlashAttribute("successMessage","Task Deleted Successfully!");
        return "redirect:/api/tasks/trash";
    }

    @GetMapping("/empty-trash")
    public String emptyTrash(HttpSession session, RedirectAttributes ra) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            taskService.emptyTrash(userId);
            ra.addFlashAttribute("successMessage", "Trash emptied successfully!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/api/tasks/trash";
    }


    @GetMapping("/markAsDone/{id}")
    public String markAsDone(
            @PathVariable Integer id,
            RedirectAttributes ra,
            HttpSession session
    ) {
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/auth/login";
            }

            Task task = taskService.findByIdAndUserId(id, userId);
            if (task == null) {
                ra.addFlashAttribute("errorMessage", "Task not found or you do not have permission to modify it.");
                return "redirect:/api/tasks";
            }

            if (task.getStatus() == TaskStatus.COMPLETED) {
                ra.addFlashAttribute("errorMessage", "Task is already completed.");
                return "redirect:/api/tasks";
            }

            taskService.markTask(id, userId);
            ra.addFlashAttribute("successMessage", "Task marked as completed!");

            return "redirect:/api/tasks";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to mark task as completed. Please try again.");
            return "redirect:/api/tasks";
        }
    }

    @PostMapping("/export")
    public String exportTasks(
            @RequestParam("action") String action,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (action == null || action.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an action to export: Download or Email.");
            return "redirect:/api/tasks";
        }

        List<Task> tasks = taskService.getAllTasksByUser(userId);

        if (tasks.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don’t have any tasks to export yet.");
            return "redirect:/api/tasks";
        }

        if ("download".equals(action)) {
            exportCsvDownload(response, tasks);
            redirectAttributes.addFlashAttribute("successMessage", "File downloaded successfully!");
            return null;
        } else if ("email".equals(action)) {
            exportCsvEmail(tasks, userId);
            redirectAttributes.addFlashAttribute("successMessage", "File emailed successfully!");
            return "redirect:/api/tasks";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid export action selected.");
            return "redirect:/api/tasks";
        }
    }

    private void exportCsvDownload(HttpServletResponse response, List<Task> tasks) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.csv");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Title,Description,Status,Priority,Due Date,Created At,Completed At");

        for (Task task : tasks) {
            writer.println(
                    task.getId() + "," +
                            csvEscape(task.getTitle()) + "," +
                            csvEscape(task.getDescription()) + "," +
                            task.getStatus() + "," +
                            task.getPriority() + "," +
                            task.getDueDate() + "," +
                            task.getCreatedAt() + "," +
                            task.getCompletedAt()
            );
        }
        writer.flush();
    }

    private void exportCsvEmail(List<Task> tasks, Integer userId) throws IOException {

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Title,Description,Status,Priority,Due Date,Created At,Completed At\n");

        for (Task task : tasks) {
            csv.append(
                            task.getId()).append(",")
                    .append(csvEscape(task.getTitle())).append(",")
                    .append(csvEscape(task.getDescription())).append(",")
                    .append(task.getStatus()).append(",")
                    .append(task.getPriority()).append(",")
                    .append(task.getDueDate()).append(",")
                    .append(task.getCreatedAt()).append(",")
                    .append(task.getCompletedAt()).append("\n");
        }

        String email = userService.getEmailByUserId(userId);

        Optional<User> optionalUser = userService.getUserById(userId);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();
        String body = buildExportCsvHtml(user.getName())
                .replace("${TASK_COUNT}", String.valueOf(tasks.size()))
                .replace("${CURRENT_DATE}", LocalDate.now().toString());

        emailService.sendCsvAttachmentEmail(
                email,
                "Your Tasks Export - TimeIt",
                body,
                csv.toString()
        );
    }

    private String buildExportCsvHtml(String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>" +
                "  <div style='max-width:600px; margin:auto; background:#ffffff; padding:30px; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>" +
                "    <h2 style='color:#16a34a; margin-bottom:10px;'>Your Task Export 📄</h2>" +
                "    <p>Hi <strong>" + userName + "</strong>,</p>" +
                "    <p>We've prepared your task list in CSV format. You can find it attached to this email.</p>" +
                "    <ul style='background:#e6f4ea; padding:15px; border-radius:6px;'>" +
                "      <li><strong>Export Type:</strong> CSV File</li>" +
                "      <li><strong>Total Tasks:</strong> ${TASK_COUNT}</li>" +
                "      <li><strong>Date:</strong> ${CURRENT_DATE}</li>" +
                "    </ul>" +
                "    <p style='margin-top:15px;'>Feel free to open it in Excel or any spreadsheet tool to manage your tasks.</p>" +
                "    <p style='margin-top:20px; font-size:14px; color:#555;'>– The <strong>TimeIt</strong> Team</p>" +
                "    <hr style='border:none; border-top:1px solid #eee; margin:20px 0;' />" +
                "    <p style='font-size:12px; color:#777;'>This is an automated email, please do not reply.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        boolean hasSpecialChar =
                value.contains(",") ||
                        value.contains("\"") ||
                        value.contains("\n") ||
                        value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        return hasSpecialChar ? "\"" + escaped + "\"" : escaped;
    }

}
