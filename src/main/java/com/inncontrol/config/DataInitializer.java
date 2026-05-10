package com.inncontrol.config;

import com.inncontrol.model.*;
import com.inncontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // System.out.println("⚠️ INICIANDO LIMPIEZA Y CARGA FORZADA DE DATOS...");

        // Desactivamos la limpieza automática para que los datos del usuario persistan
        /*
        taskRepository.deleteAll();
        roomRepository.deleteAll();
        inventoryRepository.deleteAll();
        
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_EMPLEADO)
                .forEach(userRepository::delete);
        
        userRepository.flush();
        */

        if (userRepository.count() > 0) {
            System.out.println("✅ Base de datos detectada con datos. Saltando inicialización demo.");
            return;
        }

        System.out.println("🛠️ Creando ecosistema inicial InnControl...");

        // 2. Crear Empleados
        User emp1 = createEmployee("Carlos Ruiz", "carlos@inncontrol.com");
        User emp2 = createEmployee("Elena Gomez", "elena@inncontrol.com");
        User emp3 = createEmployee("Marco Polo", "marco@inncontrol.com");
        User emp4 = createEmployee("Sofia Loren", "sofia@inncontrol.com");

        // 3. Crear Habitaciones
        Room r101 = roomRepository.save(createRoom("101", "SIMPLE", 1));
        Room r102 = roomRepository.save(createRoom("102", "SIMPLE", 1));
        Room r201 = roomRepository.save(createRoom("201", "DOBLE", 2));
        Room r202 = roomRepository.save(createRoom("202", "DOBLE", 2));
        Room r301 = roomRepository.save(createRoom("301", "SUITE", 3));
        Room r401 = roomRepository.save(createRoom("401", "PRESIDENCIAL", 4));

        // 4. Crear Inventario
        inventoryRepository.save(createInventoryItem("Toallas de Baño", "BLANQUERIA", 50, 10));
        inventoryRepository.save(createInventoryItem("Jabón Artesanal", "AMENITIES", 5, 20)); 
        inventoryRepository.save(createInventoryItem("Café Premium", "ALIMENTOS", 100, 15));
        inventoryRepository.save(createInventoryItem("Papel Higiénico", "LIMPIEZA", 8, 30)); 
        inventoryRepository.save(createInventoryItem("Agua Mineral 500ml", "ALIMENTOS", 200, 50));

        // 5. Crear Tareas
        createTask("Limpieza Profunda", "Realizar limpieza completa tras check-out", "URGENTE", r101, emp1, "EN_PROGRESO");
        createTask("Reparación AC", "El aire acondicionado hace ruido extraño", "ALTA", r202, null, "PENDIENTE");
        createTask("Reposición Frigobar", "Llenar con bebidas y snacks", "MEDIA", r301, emp2, "COMPLETADA");
        createTask("Mantenimiento de Luces", "Cambiar focos LED en el pasillo central", "BAJA", null, emp3, "EN_PROGRESO");

        System.out.println("✅ ¡DATOS REALES CARGADOS EXITOSAMENTE!");
    }

    private User createEmployee(String name, String email) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(Role.ROLE_EMPLEADO)
                .build();
        return userRepository.save(user);
    }

    private Room createRoom(String number, String type, int cap) {
        return Room.builder()
                .number(number)
                .type(type)
                .capacity(cap)
                .status(RoomStatus.valueOf("LIBRE"))
                .build();
    }

    private InventoryItem createInventoryItem(String name, String cat, int current, int min) {
        return InventoryItem.builder()
                .name(name)
                .category(cat)
                .currentQuantity(current)
                .minQuantity(min)
                .build();
    }

    private void createTask(String title, String desc, String priority, Room room, User user, String status) {
        taskRepository.save(Task.builder()
                .title(title)
                .description(desc)
                .priority(TaskPriority.valueOf(priority))
                .status(TaskStatus.valueOf(status))
                .room(room)
                .assignedTo(user)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
