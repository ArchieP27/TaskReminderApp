package com.taskreminder.app.Repository;

import com.taskreminder.app.Entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task,Integer> {
    List<Task> findByStatus(String status);
    List<Task> findByPriority(String  priority);
    List<Task> searchByTitle(String keyword);
    List<Task> findByDueDate(String dueDate);
    List<Task> findByTitleContainingIgnoreCase(String keyword);
}
