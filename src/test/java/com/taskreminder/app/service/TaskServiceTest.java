package com.taskreminder.app.service;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.repository.TaskRepository;
import com.taskreminder.app.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void testGetAllTasks() {
        List<Task> mockTasks = List.of(new Task(), new Task());
        when(taskRepository.findAll()).thenReturn(mockTasks);
        List<Task> result = taskService.getAllTasks();
        assertEquals(2, result.size());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void testFindByIdSuccess() {
        Task mockTask = new Task();
        mockTask.setId(1);
        when(taskRepository.findById(1)).thenReturn(Optional.of(mockTask));
        Optional<Task> result = taskService.findById(1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void testFindByIdNotFound() {
        when(taskRepository.findById(99)).thenReturn(Optional.empty());
        Optional<Task> result = taskService.findById(99);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddTask() {
        Task newTask = new Task();
        newTask.setTitle("Test Task");
        when(taskRepository.save(any(Task.class))).thenReturn(new Task());
        Task result = taskService.addTask(newTask);
        assertNotNull(result);
        verify(taskRepository, times(1)).save(newTask);
    }

    @Test
    void testUpdateTask() {
        Task existing = new Task();
        existing.setId(5);

        when(taskRepository.save(existing)).thenReturn(existing);
        Task result = taskService.updateTask(existing);
        assertEquals(5, result.getId());
        verify(taskRepository).save(existing);
    }

    @Test
    void testDeleteTask() {
        Integer taskId = 10;
        doNothing().when(taskRepository).deleteById(taskId);
        taskService.deleteTask(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void testMarkTaskSuccess() {
        Task task = new Task();
        task.setId(1);
        task.setStatus(TaskStatus.PENDING);
        when(taskRepository.findById(1)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        taskService.markTask(1);
        assertEquals(TaskStatus.COMPLETED,task.getStatus());
        assertNotNull(task.getCompletedAt());
        verify(taskRepository,times(1)).findById(1);
        verify(taskRepository,times(1)).save(task);
    }

    @Test
    void testMarkTaskNotFound() {
        when(taskRepository.findById(99)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> taskService.markTask(99)
        );
        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testMarkTaskAlreadyCompleted() {
        Task task = new Task();
        task.setId(2);
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(2)).thenReturn(Optional.of(task));
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> taskService.markTask(2)
        );
        assertEquals(
                "Task is already completed and cannot be marked again.",
                exception.getMessage()
        );
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testGetTasksDueTodaySuccess() {
        LocalDate today = LocalDate.now();

        Task task1 = new Task();
        task1.setStatus(TaskStatus.PENDING);

        Task task2 = new Task();
        task2.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByDueDateAndStatusNot(
                today,
                TaskStatus.COMPLETED
        )).thenReturn(List.of(task1, task2));

        List<Task> result = taskService.getTasksDueToday();

        assertEquals(2, result.size());
        verify(taskRepository, times(1))
                .findByDueDateAndStatusNot(today, TaskStatus.COMPLETED);
    }

    @Test
    void testGetTasksDueTodayEmpty() {
        LocalDate today = LocalDate.now();

        when(taskRepository.findByDueDateAndStatusNot(
                today,
                TaskStatus.COMPLETED
        )).thenReturn(List.of());

        List<Task> result = taskService.getTasksDueToday();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1))
                .findByDueDateAndStatusNot(today, TaskStatus.COMPLETED);
    }

    @Test
    void testGetUpcomingTasksSuccess() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(5);

        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);

        when(taskRepository.findByDueDateBetweenAndStatusNot(
                today,
                end,
                TaskStatus.COMPLETED
        )).thenReturn(List.of(task));

        List<Task> result = taskService.getUpcomingTasks(5);

        assertEquals(1, result.size());
        verify(taskRepository, times(1))
                .findByDueDateBetweenAndStatusNot(today, end, TaskStatus.COMPLETED);
    }

    @Test
    void testGetUpcomingTasksEmpty() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(3);

        when(taskRepository.findByDueDateBetweenAndStatusNot(
                today,
                end,
                TaskStatus.COMPLETED
        )).thenReturn(List.of());

        List<Task> result = taskService.getUpcomingTasks(3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1))
                .findByDueDateBetweenAndStatusNot(today, end, TaskStatus.COMPLETED);
    }

    @Test
    void testGetOverdueTasksSuccess() {
        LocalDate today = LocalDate.now();

        Task overdueTask = new Task();
        overdueTask.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByDueDateBeforeAndStatusNot(
                today,
                TaskStatus.COMPLETED
        )).thenReturn(List.of(overdueTask));

        List<Task> result = taskService.getOverdueTasks();

        assertEquals(1, result.size());
        verify(taskRepository, times(1))
                .findByDueDateBeforeAndStatusNot(today, TaskStatus.COMPLETED);
    }

    @Test
    void testGetOverdueTasksEmpty() {
        LocalDate today = LocalDate.now();

        when(taskRepository.findByDueDateBeforeAndStatusNot(
                today,
                TaskStatus.COMPLETED
        )).thenReturn(List.of());

        List<Task> result = taskService.getOverdueTasks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1))
                .findByDueDateBeforeAndStatusNot(today, TaskStatus.COMPLETED);
    }

}
