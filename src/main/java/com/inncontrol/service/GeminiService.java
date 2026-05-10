package com.inncontrol.service;

import com.inncontrol.dto.GeminiRequest;
import com.inncontrol.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${application.security.ai.gemini-key}")
    private String apiKey;

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateResponse(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: API Key de Gemini no configurada.";
        }

        String finalKey = apiKey.trim();

        GeminiRequest request = GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder().text(prompt).build()))
                                .build()))
                .build();

        try {
            GeminiResponse response = restTemplate.postForObject(API_URL + finalKey, request, GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                return "Estoy procesando demasiadas peticiones en este momento (Límite de API gratuito). Por favor, espera unos segundos e inténtalo de nuevo.";
            }
            return "Error al conectar con la IA: " + e.getMessage();
        }

        return "No se pudo obtener una respuesta de la IA.";
    }

    public Object listModels() {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
