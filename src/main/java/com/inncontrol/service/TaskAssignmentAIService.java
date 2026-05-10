package com.inncontrol.service;

import com.inncontrol.model.Task;
import com.inncontrol.model.User;
import com.inncontrol.repository.TaskRepository;
import com.inncontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskAssignmentAIService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public Map<String, Object> suggestBestEmployeeForTask(String taskTitle, String priority) {
        // 1. Obtener empleados activos
        List<User> employees = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("ROLE_EMPLEADO"))
                .toList();
                
        if (employees.isEmpty()) {
            throw new RuntimeException("No hay empleados (Staff) registrados para asignar tareas.");
        }

        User bestCandidate = null;
        double minWorkloadScore = Double.MAX_VALUE;
        String reason = "";

        // 2. Algoritmo de IA de carga balanceada
        for (User emp : employees) {
            List<Task> empTasks = taskRepository.findByAssignedTo(emp);
            
            // Calculamos score de carga: Urgente (3 pts), Alta (2 pts), Media (1 pt)
            double currentScore = empTasks.stream()
                .filter(t -> !"COMPLETADA".equals(t.getStatus()))
                .mapToDouble(t -> {
                    String p = t.getPriority().toString();
                    if (p.equals("URGENTE")) return 3.0;
                    if (p.equals("ALTA")) return 2.0;
                    return 1.0;
                }).sum();

            if (currentScore < minWorkloadScore) {
                minWorkloadScore = currentScore;
                bestCandidate = emp;
            }
        }

        // 3. Generar razonamiento de IA
        if (minWorkloadScore == 0) {
            reason = "El sistema detectó que " + bestCandidate.getName() + " tiene disponibilidad inmediata (0 tareas activas).";
        } else {
            reason = String.format("IA determinó que %s es el perfil óptimo con un índice de carga de %.1f (el más bajo del equipo actual).", 
                    bestCandidate.getName(), minWorkloadScore);
        }

        return Map.of(
            "employeeId", bestCandidate.getId(),
            "employeeName", bestCandidate.getName(),
            "reasoning", reason,
            "workloadScore", minWorkloadScore
        );
    }
}
