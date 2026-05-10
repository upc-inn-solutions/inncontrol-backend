package com.inncontrol.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_chat_preferences")
public class UserChatPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contact_id")
    private Long contactId; // ID of the other user or group ID

    @Column(name = "is_group")
    private boolean isGroup;

    @Column(name = "is_pinned")
    private boolean pinned;

    @Column(name = "last_activity_at")
    private java.time.LocalDateTime lastActivityAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (lastActivityAt == null) {
            lastActivityAt = java.time.LocalDateTime.now();
        }
    }
}
