package com.inncontrol.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private Long senderId;
}
