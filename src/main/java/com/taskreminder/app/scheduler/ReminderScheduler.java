package com.taskreminder.app.scheduler;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.repository.TaskRepository;
import com.taskreminder.app.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkReminders(){
        LocalDateTime now = LocalDateTime.now();

        List<Task> tasks = taskRepository.findTasksForReminder(now);
        for(Task t : tasks){
            emailService.sendTaskReminder(t);
            t.setReminderSent(true);
            taskRepository.save(t);
        }
    }

}
