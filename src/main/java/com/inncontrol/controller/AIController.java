package com.inncontrol.controller;

import com.inncontrol.dto.ChatRequest;
import com.inncontrol.dto.ChatResponse;
import com.inncontrol.service.ChatbotService;
import com.inncontrol.service.GeminiService;
import com.inncontrol.service.TaskAssignmentAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final ChatbotService chatbotService;
    private final TaskAssignmentAIService taskAssignmentAIService;
    private final GeminiService geminiService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatbotService.processMessage(request));
    }

    @GetMapping("/suggest-assignment")
    public ResponseEntity<?> suggestAssignment(
            @RequestParam String taskTitle, 
            @RequestParam String priority) {
        
        try {
            var suggestion = taskAssignmentAIService.suggestBestEmployeeForTask(taskTitle, priority);
            return ResponseEntity.ok(suggestion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> listModels() {
        return ResponseEntity.ok(geminiService.listModels());
    }
}
