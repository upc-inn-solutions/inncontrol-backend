package com.inncontrol.service;

import com.inncontrol.model.Room;
import com.inncontrol.model.RoomStatus;
import com.inncontrol.repository.RoomRepository;
import com.inncontrol.repository.TaskRepository;
import com.inncontrol.repository.UserRepository;
import com.inncontrol.model.Task;
import com.inncontrol.model.TaskPriority;
import com.inncontrol.model.TaskStatus;
import com.inncontrol.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssignmentAIService aiService;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada"));
    }

    public Room createRoom(Room room) {
        if(roomRepository.findByNumber(room.getNumber()).isPresent()) {
            throw new RuntimeException("El número de habitación ya existe");
        }
        return roomRepository.save(room);
    }

    public Room updateRoomStatus(Long id, RoomStatus status) {
        Room room = getRoomById(id);
        room.setStatus(status);
        Room updatedRoom = roomRepository.save(room);

        if (status == RoomStatus.LIMPIEZA) {
            triggerAutoCleaningTask(updatedRoom);
        }

        return updatedRoom;
    }

    private void triggerAutoCleaningTask(Room room) {
        // Verificar si ya existe una tarea de limpieza pendiente para esta habitación
        boolean exists = taskRepository.findAll().stream()
                .anyMatch(t -> t.getRoom() != null && 
                               t.getRoom().getId().equals(room.getId()) && 
                               t.getStatus() != TaskStatus.COMPLETADA &&
                               t.getTitle().contains("Limpieza"));
        
        if (exists) return;

        // 1. Crear la tarea
        Task task = Task.builder()
                .title("Limpieza de Habitación " + room.getNumber())
                .description("Tarea generada automáticamente por cambio de estado a LIMPIEZA.")
                .priority(TaskPriority.MEDIA)
                .status(TaskStatus.PENDIENTE)
                .room(room)
                .createdAt(LocalDateTime.now())
                .build();

        // 2. Usar IA para asignar al mejor empleado disponible
        try {
            Map<String, Object> suggestion = aiService.suggestBestEmployeeForTask(task.getTitle(), "MEDIA");
            Long empId = (Long) suggestion.get("employeeId");
            User bestEmp = userRepository.findById(empId).orElse(null);
            
            if (bestEmp != null) {
                task.setAssignedTo(bestEmp);
                task.setStatus(TaskStatus.EN_PROGRESO);
                System.out.println("🤖 IA asignó automáticamente a " + bestEmp.getName() + " para limpiar la " + room.getNumber());
            }
        } catch (Exception e) {
            System.out.println("⚠️ IA no pudo asignar automáticamente: " + e.getMessage());
        }

        taskRepository.save(task);
    }

    public Room updateRoom(Long id, Room roomDetails) {
        Room room = getRoomById(id);
        room.setNumber(roomDetails.getNumber());
        room.setType(roomDetails.getType());
        room.setCapacity(roomDetails.getCapacity());
        room.setFloor(roomDetails.getFloor());
        
        RoomStatus oldStatus = room.getStatus();
        room.setStatus(roomDetails.getStatus());
        Room updatedRoom = roomRepository.save(room);

        if (roomDetails.getStatus() == RoomStatus.LIMPIEZA && oldStatus != RoomStatus.LIMPIEZA) {
            triggerAutoCleaningTask(updatedRoom);
        }

        return updatedRoom;
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
