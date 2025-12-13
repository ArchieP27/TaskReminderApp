package com.taskreminder.app.Service;

import com.taskreminder.app.Entity.Task;
import com.taskreminder.app.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getAllTasks(){
        return taskRepository.findAll();
    }

    public Task addTask(Task  task){
        return taskRepository.save(task);
    }

    public Task updateTask(Task  task){
        return taskRepository.save(task);
    }

    public void deleteTask(Integer id){
        taskRepository.deleteById(id);
    }

    public Optional<Task> findById(Integer id){
        return taskRepository.findById(id);
    }


    public void markTask(Integer id){
        Task task = taskRepository.findById(id).orElseThrow(()->new RuntimeException("Task Now Found!"));
        task.setStatus("Completed");
        taskRepository.save(task);
    }

    public List<Task> getFilteredTasks(String status, String priority, String keyword, String sort) {

        List<Task> tasks = taskRepository.findAll();

        if (status != null && !status.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> status.equalsIgnoreCase(t.getStatus()))
                    .toList();
        }

        if (priority != null && !priority.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> priority.equalsIgnoreCase(t.getPriority()))
                    .toList();
        }

        if (keyword != null && !keyword.isEmpty()) {
            tasks = tasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }

        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "dueDate" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getDueDate))
                        .toList();
                case "priority" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getPriority))
                        .toList();
                case "createdAt" -> tasks = tasks.stream()
                        .sorted(Comparator.comparing(Task::getCreatedAt))
                        .toList();
            }
        }

        return tasks;
    }

    public List<Task> findByStatus(String status){
        return taskRepository.findByStatus(status);
    }

    public List<Task> findByPriority(String priority){
        return taskRepository.findByPriority(priority);
    }

    public List<Task> findByDueDate(String dueDate){
        return taskRepository.findByDueDate(dueDate);
    }

    public List<Task> searchByTitle(String keyword){
        return taskRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

//    private final List<Task> tasks = new ArrayList<>();
//    private static int counter=100;
//
//    public TaskService(){
//        tasks.add(new Task(1,"Lean Spring Boot","Basics of Project","2025-10-15","Pending","High"));
//        tasks.add(new Task(2,"Practice Java","Collections & OOPS","2025-01-25","Pending","Medium"));
//    }
//
//    public List<Task> getAllTasks(){
//        return tasks;
//    }
//
//    public static int nextId(){
//        return counter++;
//    }
//
//    public void addTask(Task task){
//        tasks.add(task);
//    }
//
//    public Optional<Task> findById(Integer id){
//        return tasks.stream().filter(task -> task.getId().equals(id)).findFirst();
//    }
//
//    public void updateTask(Task updated){
//        for(int i=0;i<tasks.size();i++){
//            if(tasks.get(i).getId().equals(updated.getId())){
//                tasks.set(i,updated);
//                return;
//            }
//        }
//    }
//
//    public void deleteTask(Integer id){
//        Iterator<Task> it = tasks.iterator();
//        while(it.hasNext()){
//            if(it.next().getId().equals(id)){
//                it.remove();
//                return;
//            }
//        }
//    }
}
