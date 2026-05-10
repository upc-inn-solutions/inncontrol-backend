package com.inncontrol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PinRequest {
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("contactId")
    private Long contactId;
    
    @JsonProperty("isGroup")
    private boolean isGroup;
}
