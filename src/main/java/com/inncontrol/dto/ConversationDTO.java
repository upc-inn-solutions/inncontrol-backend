package com.inncontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long contactId;      // User ID or Group ID
    private String contactName;
    private String contactPhoto;
    private String contactRole;   // For groups, it could be "Grupo"
    private String lastMessage;
    private String lastMessageTime;
    private boolean lastMessageRead;
    private boolean lastMessageIsFromMe;
    private int unreadCount;
    private boolean group;
    private boolean pinned;
    private Long creatorId; // For groups: who created it
    @com.fasterxml.jackson.annotation.JsonProperty("isMember")
    private boolean isMember; // For groups: is current user still a member
    @com.fasterxml.jackson.annotation.JsonProperty("isDeleted")
    private boolean isDeleted; // For groups: is it soft-deleted
}
