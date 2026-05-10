package com.inncontrol.service;

import com.inncontrol.dto.TaskRequest;
import com.inncontrol.model.Room;
import com.inncontrol.model.Task;
import com.inncontrol.model.TaskStatus;
import com.inncontrol.model.User;
import com.inncontrol.repository.RoomRepository;
import com.inncontrol.repository.TaskRepository;
import com.inncontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return taskRepository.findByAssignedTo(user);
    }

    public Task createTask(TaskRequest request, Long actorId) {
        User assignedTo = null;
        if (request.getAssignedToId() != null) {
            assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Usuario asignado no encontrado"));
        }
        
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Habitación no encontrada"));
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TaskStatus.PENDIENTE)
                .assignedTo(assignedTo)
                .room(room)
                .dueDate(request.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);
        messageService.sendTaskNotification(savedTask, "CREATED", actorId);
        return savedTask;
    }

    @Transactional
    public Task updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
        
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Usuario asignado no encontrado"));
            task.setAssignedTo(assignedTo);
        } else {
            task.setAssignedTo(null);
        }

        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Habitación no encontrada"));
            task.setRoom(room);
        } else {
            task.setRoom(null);
        }

        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long id, TaskStatus status, Long actorId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
        task.setStatus(status);
        Task savedTask = taskRepository.save(task);
        messageService.sendTaskNotification(savedTask, "STATUS_UPDATED:" + status.name(), actorId);
        return savedTask;
    }

    public void deleteTask(Long id) {
        messageService.deleteSystemTaskMessages(id);
        taskRepository.deleteById(id);
    }
}
