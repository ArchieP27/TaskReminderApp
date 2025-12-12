package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.Task;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private static int counter=100;

    public TaskService(){
        tasks.add(new Task(1,"Lean Spring Boot","Basics of Project","2025-10-15","Pending","High"));
        tasks.add(new Task(2,"Practice Java","Collections & OOPS","2025-01-25","Pending","Medium"));
    }

    public List<Task> getAllTasks(){
        return tasks;
    }

    public static int nextId(){
        return counter++;
    }

    public void addTask(Task task){
        tasks.add(task);
    }
}
