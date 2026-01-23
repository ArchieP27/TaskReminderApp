package com.taskreminder.app.service;

import com.taskreminder.app.entity.Task;
import com.taskreminder.app.entity.User;
import com.taskreminder.app.enums.TaskPriority;
import com.taskreminder.app.enums.TaskStatus;
import com.taskreminder.app.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Mock
    private UserService userService;

    @Test
    void testAddTaskSuccess() {
        Task task = new Task();
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.addTask(task);

        assertNotNull(result);
        verify(taskRepository).save(task);
    }

    @Test
    void testFindByIdAndUserIdSuccess() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(10);
        task.setUser(user);

        when(taskRepository.findById(10)).thenReturn(Optional.of(task));

        Task result = taskService.findByIdAndUserId(10, 1);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void testFindByIdAndUserIdWrongUser() {
        User user = new User();
        user.setId(2);

        Task task = new Task();
        task.setUser(user);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        Task result = taskService.findByIdAndUserId(1, 1);

        assertNull(result);
    }

    @Test
    void testUpdateTaskSuccess() {
        User user = new User();
        user.setId(1);

        Task existing = new Task();
        existing.setId(5);
        existing.setUser(user);
        existing.setCreatedAt(LocalDate.now());

        when(taskRepository.findById(5)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task updated = new Task();
        updated.setId(5);
        updated.setTitle("Updated");

        Task result = taskService.updateTask(updated, 1);

        assertEquals("Updated", result.getTitle());
        assertEquals(user, result.getUser());
    }

    @Test
    void testUpdateTaskNotFound() {
        Task task = new Task();
        task.setId(99);

        when(taskRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> taskService.updateTask(task, 1)
        );

        assertEquals("Task not found", ex.getMessage());
    }

    @Test
    void testMoveToTrashSuccess() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(3);
        task.setUser(user);
        task.setDeleted(false);

        when(taskRepository.findByIdAndUserIdIncludingDeleted(3, 1))
                .thenReturn(Optional.of(task));

        taskService.moveToTrash(3, 1);

        assertTrue(task.isDeleted());
        assertNotNull(task.getDeletedAt());

        verify(taskRepository).save(task);
    }

    @Test
    void testMoveToTrashTaskNotFound() {
        when(taskRepository.findByIdAndUserIdIncludingDeleted(10, 1))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> taskService.moveToTrash(10, 1)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void testMoveToTrashAlreadyDeleted() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(3);
        task.setUser(user);
        task.setDeleted(true);

        when(taskRepository.findByIdAndUserIdIncludingDeleted(3, 1))
                .thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> taskService.moveToTrash(3, 1)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    void testRestoreTaskSuccess() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(3);
        task.setDeleted(true);

        when(taskRepository.findByIdAndUserIdIncludingDeleted(3, 1))
                .thenReturn(Optional.of(task));

        when(userService.getUserById(1))
                .thenReturn(Optional.of(user));

        taskService.restoreTask(3, 1);

        assertFalse(task.isDeleted());
        assertNull(task.getDeletedAt());
        assertEquals(user, task.getUser());

        verify(taskRepository).save(task);
    }

    @Test
    void testPermanentDeleteSuccess() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setId(3);
        task.setDeleted(true);
        task.setUser(user);

        when(taskRepository.findByIdAndUserIdAndDeletedTrue(3, 1))
                .thenReturn(Optional.of(task));

        taskService.permanentDelete(3, 1);

        verify(taskRepository).delete(task);
    }

    @Test
    void testEmptyTrashSuccess() {
        List<Task> tasks = List.of(new Task(), new Task());

        when(taskRepository.findByUser_IdAndDeletedTrue(1))
                .thenReturn(tasks);

        taskService.emptyTrash(1);

        verify(taskRepository).deleteAll(tasks);
    }


    @Test
    void testMarkTaskSuccess() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);
        task.setUser(user);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        taskService.markTask(1, 1);

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
        verify(taskRepository).save(task);
    }

    @Test
    void testMarkTaskAlreadyCompleted() {
        User user = new User();
        user.setId(1);

        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETED);
        task.setUser(user);

        when(taskRepository.findById(1)).thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> taskService.markTask(1, 1)
        );
    }

    @Test
    void testGetPagedTasksWithStatusAndPriority() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Task> page = new PageImpl<>(List.of(new Task()));

        when(taskRepository.findByUser_IdAndDeletedFalseAndStatusAndPriority(
                1, TaskStatus.PENDING, TaskPriority.HIGH, pageable
        )).thenReturn(page);

        Page<Task> result = taskService.getPagedTasks(
                1, pageable, TaskStatus.PENDING, TaskPriority.HIGH, null
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetTasksDueToday() {
        when(taskRepository.findByUser_IdAndDeletedFalseAndDueDateAndStatusNot(
                eq(1),
                any(LocalDate.class),
                eq(TaskStatus.COMPLETED)
        )).thenReturn(List.of(new Task()));

        List<Task> result = taskService.getTasksDueToday(1);

        assertEquals(1, result.size());
    }

    @Test
    void testGetUpcomingTasks() {
        Integer userId = 1;
        int days = 7;

        Task task = new Task();
        task.setDeleted(false);
        task.setDueDate(LocalDate.now().plusDays(3));
        task.setStatus(TaskStatus.PENDING);

        when(taskRepository.findByUser_IdAndDeletedFalseAndDueDateBetweenAndStatusNot(
                eq(userId),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(TaskStatus.COMPLETED)
        )).thenReturn(List.of(task));

        List<Task> result = taskService.getUpcomingTasks(userId, days);

        assertEquals(1, result.size());
    }


    @Test
    void testGetOverdueTasks() {
        when(taskRepository.findByUser_IdAndDeletedFalseAndDueDateBeforeAndStatusNot(
                eq(1),
                any(LocalDate.class),
                eq(TaskStatus.COMPLETED)
        )).thenReturn(List.of(new Task()));

        List<Task> result = taskService.getOverdueTasks(1);

        assertEquals(1, result.size());
    }

    @Test
    void testGetUpcomingReminders() {
        when(taskRepository.findUpcomingRemindersForUser(
                eq(1), any(), any()
        )).thenReturn(List.of(new Task()));

        List<Task> result = taskService.getUpcomingReminders(1);

        assertFalse(result.isEmpty());
    }

    @Test
    void testGetRecentTasks() {
        Task t1 = new Task();
        t1.setCreatedAt(LocalDate.now());

        Task t2 = new Task();
        t2.setCreatedAt(LocalDate.now().minusDays(1));

        when(taskRepository.findByUser_IdAndDeletedFalse(1))
                .thenReturn(List.of(t2, t1));

        List<Task> result = taskService.getRecentTasks(1, 1);

        assertEquals(1, result.size());
        assertEquals(t1, result.get(0));
    }


    @Test
    void testGetHighPriorityTasks() {
        Page<Task> page = new PageImpl<>(List.of(new Task()));

        when(taskRepository.findByUser_IdAndDeletedFalseAndPriority(
                eq(1),
                eq(TaskPriority.HIGH),
                any(Pageable.class)
        )).thenReturn(page);

        List<Task> result = taskService.getHighPriorityTasks(1);

        assertEquals(1, result.size());
    }

    @Test
    void testGetPendingTasks() {
        when(taskRepository.findByUser_IdAndDeletedFalseAndStatus(
                eq(1),
                eq(TaskStatus.PENDING)
        )).thenReturn(List.of(new Task()));

        assertEquals(1, taskService.getPendingTasks(1).size());
    }

    @Test
    void testGetCompletedTasks() {
        Page<Task> page = new PageImpl<>(List.of(new Task()));

        when(taskRepository.findByUser_IdAndDeletedFalseAndStatus(
                eq(1),
                eq(TaskStatus.COMPLETED),
                any(Pageable.class)
        )).thenReturn(page);

        List<Task> result = taskService.getCompletedTasks(1);

        assertEquals(1, result.size());
    }



    @Test
    void testGetInProgressTasks() {
        when(taskRepository.findByUser_IdAndDeletedFalseAndStatus(
                eq(1),
                eq(TaskStatus.IN_PROGRESS)
        )).thenReturn(List.of(new Task()));

        List<Task> result = taskService.getInProgressTasks(1);

        assertEquals(1, result.size());
    }

}
