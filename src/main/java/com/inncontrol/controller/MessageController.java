package com.inncontrol.controller;

import com.inncontrol.dto.ConversationDTO;
import com.inncontrol.dto.GroupMemberDTO;
import com.inncontrol.dto.GroupRequest;
import com.inncontrol.model.ChatGroup;
import com.inncontrol.model.Message;
import com.inncontrol.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    public List<ConversationDTO> getConversations(@RequestParam Long userId, @RequestParam(required = false) String search) {
        return messageService.getConversations(userId, search);
    }

    @GetMapping("/history")
    public List<Message> getHistory(@RequestParam Long userId1, @RequestParam Long userId2) {
        return messageService.getChatHistory(userId1, userId2);
    }

    @GetMapping("/group/history")
    public List<Message> getGroupHistory(@RequestParam Long groupId, @RequestParam Long userId) {
        return messageService.getGroupHistory(groupId, userId);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> payload) {
        try {
            Long senderId = toLong(payload.get("senderId"));
            Long receiverId = toLong(payload.get("receiverId"));
            Long groupId = toLong(payload.get("groupId"));
            Long replyToId = toLong(payload.get("replyToId"));
            String content = payload.get("content") != null ? payload.get("content").toString() : "";
            Message msg = messageService.sendMessage(senderId, receiverId, groupId, content, replyToId);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId, 
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean forEveryone) {
        messageService.deleteMessage(messageId, userId, forEveryone);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/messages/bulk-delete")
    public ResponseEntity<Void> deleteMessages(@RequestBody List<Long> messageIds, @RequestParam Long userId) {
        messageService.deleteMessages(messageIds, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/message/{messageId}/pin")
    public ResponseEntity<Void> togglePinMessage(@PathVariable Long messageId) {
        messageService.togglePinMessage(messageId);
        return ResponseEntity.ok().build();
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (Exception e) { return null; }
    }

    @RequestMapping(value = "/read", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Void> markAsRead(
            @RequestParam(required = false) Long userId, 
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId) {
        
        Long finalUserId = (userId != null) ? userId : receiverId;
        Long finalContactId = (contactId != null) ? contactId : senderId;
        
        if (finalUserId != null && finalContactId != null) {
            // Queremos marcar como leídos los mensajes que el contacto (finalContactId) me envió a mí (finalUserId)
            messageService.markAsRead(finalContactId, finalUserId);
        }
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/group/read", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Void> markGroupAsRead(
            @RequestParam(required = false) Long userId, 
            @RequestParam(required = false) Long groupId,
            @RequestBody(required = false) Map<String, Long> payload) {
        
        Long finalUserId = userId;
        Long finalGroupId = groupId;
        
        if (payload != null) {
            if (finalUserId == null) finalUserId = payload.get("userId");
            if (finalGroupId == null) finalGroupId = payload.get("groupId");
        }
        
        if (finalUserId != null && finalGroupId != null) {
            messageService.markGroupAsRead(finalGroupId, finalUserId);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public Map<String, Integer> getNotificationBadges(
            @RequestParam Long userId, 
            @RequestParam(required = false, defaultValue = "0") Long lastVisitedTasks,
            @RequestParam(required = false, defaultValue = "0") Long lastVisitedMessages) {
        return messageService.getNotificationBadges(userId, lastVisitedTasks, lastVisitedMessages);
    }

    @DeleteMapping("/clear-history")
    public ResponseEntity<Void> clearHistory(
            @RequestParam Long userId, 
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Long groupId) {
        
        boolean isGroup = (groupId != null);
        Long id = isGroup ? groupId : contactId;
        if (id != null) {
            messageService.clearHistory(userId, id, isGroup);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteConversation(
            @RequestParam Long userId, 
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Long groupId) {
        
        boolean isGroup = (groupId != null);
        Long id = isGroup ? groupId : contactId;
        if (id != null) {
            messageService.deleteConversation(userId, id, isGroup);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/pin")
    public ResponseEntity<Void> togglePin(@RequestBody Map<String, Object> payload) {
        Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
        Long contactId = payload.get("contactId") != null ? Long.valueOf(payload.get("contactId").toString()) : null;
        boolean isGroup = payload.get("isGroup") != null && Boolean.parseBoolean(payload.get("isGroup").toString());
        if (userId != null && contactId != null) {
            messageService.togglePin(userId, contactId, isGroup);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/group/members")
    public List<GroupMemberDTO> getGroupMembers(@RequestParam Long groupId) {
        return messageService.getGroupMembers(groupId);
    }

    @PostMapping("/group")
    public ResponseEntity<Void> createGroup(@RequestBody GroupRequest request) {
        messageService.createGroup(
            request.getName(), 
            request.getDescription(), 
            request.getPhoto(), 
            request.getMemberIds(), 
            request.getCreatorId()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId, @RequestParam Long requesterId) {
        messageService.deleteGroup(groupId, requesterId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/group/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        messageService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/group/{groupId}/remove")
    public ResponseEntity<Void> removeFromGroup(
            @PathVariable Long groupId,
            @RequestParam Long memberId,
            @RequestParam Long adminId) {
        messageService.removeFromGroup(groupId, memberId, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/group/{groupId}/add")
    public ResponseEntity<Void> addMember(
            @PathVariable Long groupId,
            @RequestParam Long memberId,
            @RequestParam Long adminId) {
        messageService.addMember(groupId, memberId, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/group/{groupId}/promote")
    public ResponseEntity<Void> promoteToAdmin(
            @PathVariable Long groupId,
            @RequestParam Long memberId,
            @RequestParam Long adminId) {
        messageService.promoteToAdmin(groupId, memberId, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/group/{groupId}/demote")
    public ResponseEntity<Void> demoteAdmin(
            @PathVariable Long groupId,
            @RequestParam Long memberId,
            @RequestParam Long adminId) {
        messageService.demoteAdmin(groupId, memberId, adminId);
        return ResponseEntity.ok().build();
    }
}
