package com.inncontrol.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"photo"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"photo"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = true) // Nullable for group messages
    private User receiver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = true) // For group messaging
    private ChatGroup group;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read")
    private boolean read;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"photo"})
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "message_reads",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<User> readBy = new java.util.HashSet<>();

    @Column(name = "message_type", nullable = true)
    private String type; // 'CHAT', 'SYSTEM', or 'DELETED'

    @Column(name = "is_pinned")
    private Boolean pinned = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"parentMessage", "readBy", "receiver", "group"})
    private Message parentMessage;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"photo"})
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "message_deletions",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<User> deletedBy = new java.util.HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        read = false;
        pinned = false;
        if (type == null) type = "CHAT";
    }
}
