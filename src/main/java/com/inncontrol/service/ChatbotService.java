package com.inncontrol.service;

import com.inncontrol.dto.ChatRequest;
import com.inncontrol.dto.ChatResponse;
import com.inncontrol.model.RoomStatus;
import com.inncontrol.model.TaskStatus;
import com.inncontrol.repository.InventoryRepository;
import com.inncontrol.repository.RoomRepository;
import com.inncontrol.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final RoomRepository roomRepository;
    private final TaskRepository taskRepository;
    private final InventoryRepository inventoryRepository;
    private final GeminiService geminiService;

    public ChatResponse processMessage(ChatRequest request) {
        String userMsg = request.getMessage();
        Long senderId = request.getSenderId();
        
        // 1. GATHER CONTEXT (Real data from DB)
        long freeRooms = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.LIBRE).count();
        long totalRooms = roomRepository.count();
        
        // Tareas globales
        long totalPendingTasks = taskRepository.findAll().stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDIENTE).count();
                
        // Tareas del usuario actual
        var userTasks = taskRepository.findAll().stream()
                .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().getId().equals(senderId))
                .collect(Collectors.toList());
                
        long userPendingTasks = userTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDIENTE).count();
        
        String userPendingTasksList = userTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDIENTE)
                .map(t -> "- " + t.getTitle() + " (Prioridad: " + t.getPriority() + ")")
                .collect(Collectors.joining("\n"));

        long lowStockItems = inventoryRepository.findAll().stream()
                .filter(i -> i.getCurrentQuantity() <= i.getMinQuantity()).count();
        String criticalList = inventoryRepository.findAll().stream()
                .filter(i -> i.getCurrentQuantity() <= i.getMinQuantity())
                .map(i -> i.getName() + " (Quedan: " + i.getCurrentQuantity() + ")")
                .collect(Collectors.joining(", "));

        // 2. BUILD SYSTEM PROMPT (The "Intelligence")
        String systemPrompt = String.format(
            "Eres el Asistente Inteligente de InnControl. DATOS ACTUALES:\n" +
            "- Habitaciones Libres: %d de un total de %d.\n" +
            "- Tareas Totales del hotel: %d pendientes.\n" +
            "- Tareas asignadas ESPECÍFICAMENTE al usuario que te está hablando: %d pendientes.\n" +
            "Lista de sus tareas pendientes:\n%s\n" +
            "- Stock Crítico del hotel: %d artículos (%s).\n\n" +
            "INSTRUCCIONES DE FORMATO:\n" +
            "1. NO TE PRESENTES (ya te saludaste al inicio). Ve directo a la respuesta.\n" +
            "2. Tienes libertad total para usar negritas, listas numeradas o viñetas.\n" +
            "3. Si el usuario pregunta por SUS tareas, usa SOLO la lista de sus tareas pendientes.\n" +
            "4. Sé breve y profesional.\n\n" +
            "Mensaje del usuario: \"%s\"",
            freeRooms, totalRooms, totalPendingTasks, userPendingTasks, 
            userPendingTasksList.isEmpty() ? "Ninguna actualmente" : userPendingTasksList,
            lowStockItems, criticalList.isEmpty() ? "Ninguno" : criticalList, 
            userMsg
        );

        // 3. CALL REAL IA
        String aiReply = geminiService.generateResponse(systemPrompt);

        return ChatResponse.builder()
                .reply(aiReply)
                .source("GEMINI_2.5_FLASH")
                .build();
    }
}
