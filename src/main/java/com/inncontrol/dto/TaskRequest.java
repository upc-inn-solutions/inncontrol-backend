package com.inncontrol.dto;

import com.inncontrol.model.TaskPriority;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private Long assignedToId;
    private Long roomId; // Opcional
    private LocalDateTime dueDate;
}
