package com.inncontrol.service;

import com.inncontrol.dto.ConversationDTO;
import com.inncontrol.model.*;
import com.inncontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatGroupRepository groupRepository;
    private final UserChatPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskRepository taskRepository;
    private final ChatbotService chatbotService;

    public List<ConversationDTO> getConversations(Long userId, String searchTerm) {
        try {
            User currentUser = userRepository.findById(userId).orElse(null);
            if (currentUser == null) {
                System.err.println("❌ ERROR: User with ID " + userId + " not found in DB.");
                return new ArrayList<>();
            }
            List<ConversationDTO> conversations = new ArrayList<>();

            // 1. Get Individual Chats
            List<User> otherUsers = userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(userId))
                    .collect(Collectors.toList());

            for (User other : otherUsers) {
                Optional<Message> lastMsg = messageRepository.findLastMessage(userId, other.getId());
                UserChatPreference pref = preferenceRepository.findByUserAndContactIdAndIsGroup(currentUser, other.getId(), false)
                        .orElse(null);
                boolean isPinned = pref != null && pref.isPinned();

                // Show if has messages OR has a preference record (e.g. pinned or manually kept)
                if (searchTerm == null || searchTerm.isEmpty()) {
                    if (lastMsg.isEmpty() && pref == null) continue;
                } else {
                    if (!other.getName().toLowerCase().contains(searchTerm.toLowerCase())) continue;
                }

                conversations.add(ConversationDTO.builder()
                        .contactId(other.getId())
                        .contactName(other.getName())
                        .contactPhoto(other.getPhoto())
                        .contactRole(other.getRole().name())
                        .lastMessage(lastMsg.map(Message::getContent).orElse(""))
                        .lastMessageTime(lastMsg.map(m -> m.getCreatedAt().toString()).orElse(
                            pref != null && pref.getLastActivityAt() != null ? pref.getLastActivityAt().toString() : "2000-01-01T00:00:00"
                        ))
                        .lastMessageRead(lastMsg.map(Message::isRead).orElse(true))
                        .lastMessageIsFromMe(lastMsg.map(m -> m.getSender().getId().equals(userId)).orElse(false))
                        .group(false)
                        .pinned(isPinned)
                        .unreadCount((int) messageRepository.countUnreadMessages(other.getId(), userId))
                        .build());
            }

            // 2. Get Group Chats (Members + Left but have preferences)
            java.util.List<ChatGroup> activeGroups = groupRepository.findByMembersContaining(currentUser);
            java.util.List<UserChatPreference> groupPrefs = preferenceRepository.findByUserAndIsGroup(currentUser, true);
            
            java.util.Set<ChatGroup> allGroups = new java.util.HashSet<>(activeGroups);
            for (UserChatPreference p : groupPrefs) {
                groupRepository.findById(p.getContactId()).ifPresent(allGroups::add);
            }

            for (ChatGroup group : allGroups) {
                // Si el grupo está eliminado, sólo lo mostramos si el usuario NO ha borrado el chat (existe preferencia)
                if (group.isDeleted()) {
                    if (preferenceRepository.findByUserAndContactIdAndIsGroup(currentUser, group.getId(), true).isEmpty()) {
                        continue;
                    }
                }

                if (searchTerm != null && !searchTerm.isEmpty()) {
                    if (!group.getName().toLowerCase().contains(searchTerm.toLowerCase())) continue;
                }

                Optional<Message> lastMsg = messageRepository.findLastGroupMessage(group.getId(), userId);
                UserChatPreference pref = preferenceRepository.findByUserAndContactIdAndIsGroup(currentUser, group.getId(), true)
                        .orElse(null);
                boolean isPinned = pref != null && pref.isPinned();

                conversations.add(ConversationDTO.builder()
                        .contactId(group.getId())
                        .contactName(group.getName())
                        .contactPhoto(group.getPhoto())
                        .contactRole("GRUPO")
                        .lastMessage(lastMsg.map(Message::getContent).orElse(""))
                        .lastMessageTime(lastMsg.map(m -> m.getCreatedAt().toString()).orElse(
                            pref != null && pref.getLastActivityAt() != null ? pref.getLastActivityAt().toString() : 
                            (group.getCreatedAt() != null ? group.getCreatedAt().toString() : "2000-01-01T00:00:00")
                        ))
                        .lastMessageRead(lastMsg.map(m -> m.getSender().getId().equals(userId) || m.getReadBy().stream().anyMatch(u -> u.getId().equals(userId))).orElse(true))
                        .lastMessageIsFromMe(lastMsg.map(m -> m.getSender().getId().equals(userId)).orElse(false))
                        .group(true)
                        .pinned(isPinned)
                        .unreadCount((int) messageRepository.countUnreadGroupMessages(group, currentUser))
                        .creatorId(group.getCreator() != null ? group.getCreator().getId() : null)
                        .isMember(group.getMembers().contains(currentUser))
                        .isDeleted(group.isDeleted())
                        .build());
            }

            // 3. Sort: Pinned first, then by last message time
            return conversations.stream()
                    .sorted((c1, c2) -> {
                        if (c1.isPinned() != c2.isPinned()) {
                            return c1.isPinned() ? -1 : 1;
                        }
                        return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ ERROR in getConversations: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Message> getChatHistory(Long userId1, Long userId2) {
        User u1 = userRepository.findById(userId1).orElseThrow();
        User u2 = userRepository.findById(userId2).orElseThrow();
        List<Message> history = messageRepository.findChatHistory(u1, u2);
        
        // Filtrar mensajes que el usuario ha borrado para sí mismo
        return history.stream()
                .filter(m -> m.getDeletedBy().stream().noneMatch(u -> u.getId().equals(userId1)))
                .collect(Collectors.toList());
    }

    public List<Message> getGroupHistory(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        List<Message> history = messageRepository.findByGroupOrderByCreatedAtAsc(group);
        
        return history.stream()
                .filter(m -> m.getDeletedBy().stream().noneMatch(u -> u.getId().equals(userId)))
                .collect(Collectors.toList());
    }

    public Message sendMessage(Long senderId, Long receiverId, Long groupId, String content, Long replyToId) {
        User sender = userRepository.findById(senderId).orElseThrow();
        Message.MessageBuilder builder = Message.builder()
                .sender(sender)
                .content(content)
                .type("CHAT");

        if (replyToId != null) {
            messageRepository.findById(replyToId).ifPresent(builder::parentMessage);
        }

        if (groupId != null) {
            ChatGroup group = groupRepository.findById(groupId).orElseThrow();
            if (group.isDeleted()) {
                throw new RuntimeException("No puedes enviar mensajes a este grupo porque ha sido eliminado");
            }
            builder.group(group);
            builder.receiver(sender); // TRUCO: La base de datos requiere que receiver_id no sea nulo
        } else {
            User receiver = userRepository.findById(receiverId).orElseThrow();
            builder.receiver(receiver);
            // Asegurar que la conversación se quede en la lista aunque se vacíe el historial después
            ensurePreferenceExists(sender, receiver.getId(), false);
            ensurePreferenceExists(receiver, sender.getId(), false);
        }

        Message savedMsg = messageRepository.save(builder.build());
        
        // Notificar vía WebSocket
        if (groupId != null) {
            messagingTemplate.convertAndSend("/topic/group/" + groupId, savedMsg);
        } else {
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", savedMsg);
            // También notificar al sender (para que sepa que llegó al server si lo necesita, aunque ya lo tiene)
            messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/messages", savedMsg);
        }
        
        // Actualizar última actividad para el ordenamiento de la lista
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (groupId != null) {
            ChatGroup group = groupRepository.findById(groupId).get();
            for (User member : group.getMembers()) {
                updateLastActivity(member, groupId, true, now);
            }
        } else {
            updateLastActivity(sender, receiverId, false, now);
            updateLastActivity(userRepository.findById(receiverId).get(), senderId, false, now);
        }

        // RESPUESTA AUTOMÁTICA SI ES EL ASISTENTE
        User receiver = groupId != null ? null : userRepository.findById(receiverId).orElse(null);
        if (receiver != null && "InnControl Assistant".equals(receiver.getName())) {
            // Ejecutar respuesta de IA de forma asíncrona (opcional, aquí lo haremos secuencial por simplicidad pero notificando via WS)
            try {
                com.inncontrol.dto.ChatRequest aiRequest = new com.inncontrol.dto.ChatRequest();
                aiRequest.setMessage(content);
                aiRequest.setSenderId(senderId);
                com.inncontrol.dto.ChatResponse aiResponse = chatbotService.processMessage(aiRequest);
                
                // Enviar respuesta del asistente de vuelta
                sendMessage(receiverId, senderId, null, aiResponse.getReply(), null);
            } catch (Exception e) {
                System.err.println("ERROR al procesar respuesta de IA: " + e.getMessage());
            }
        }

        return savedMsg;
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId, boolean forEveryone) {
        Message msg = messageRepository.findById(messageId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        if (forEveryone) {
            // Solo se puede eliminar para todos si eres el remitente y ha pasado menos de 1 hora
            if (msg.getSender().getId().equals(userId)) {
                java.time.Duration duration = java.time.Duration.between(msg.getCreatedAt(), java.time.LocalDateTime.now());
                if (duration.toHours() < 1) {
                    msg.setContent("Mensaje eliminado");
                    msg.setType("DELETED");
                    messageRepository.save(msg);
                    return;
                }
            }
        }
        
        // Por defecto o si no se puede para todos, se marca como borrado para mí
        msg.getDeletedBy().add(user);
        messageRepository.save(msg);
    }

    @Transactional
    public void togglePinMessage(Long messageId) {
        Message msg = messageRepository.findById(messageId).orElseThrow();
        boolean newState = !Boolean.TRUE.equals(msg.getPinned());
        
        // Si vamos a fijar, primero desfijamos otros en el mismo contexto
        if (newState) {
            List<Message> history;
            if (msg.getGroup() != null) {
                history = messageRepository.findByGroupOrderByCreatedAtAsc(msg.getGroup());
            } else {
                history = messageRepository.findChatHistory(msg.getSender(), msg.getReceiver());
            }
            for (Message m : history) {
                if (Boolean.TRUE.equals(m.getPinned())) {
                    m.setPinned(false);
                    messageRepository.save(m);
                }
            }
        }
        
        msg.setPinned(newState);
        messageRepository.save(msg);
    }

    @Transactional
    public void deleteMessages(List<Long> messageIds, Long userId) {
        for (Long id : messageIds) {
            deleteMessage(id, userId, false);
        }
    }

    private void updateLastActivity(User user, Long contactId, boolean isGroup, java.time.LocalDateTime time) {
        UserChatPreference pref = preferenceRepository.findByUserAndContactIdAndIsGroup(user, contactId, isGroup)
                .orElse(UserChatPreference.builder()
                        .user(user)
                        .contactId(contactId)
                        .isGroup(isGroup)
                        .pinned(false)
                        .build());
        pref.setLastActivityAt(time);
        preferenceRepository.save(pref);
    }

    public void markAsRead(Long senderId, Long receiverId) {
        messageRepository.markAsRead(senderId, receiverId);
    }

    @Transactional
    public void markGroupAsRead(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        List<Message> unread = messageRepository.findUnreadGroupMessages(group, user);
        for (Message m : unread) {
            m.getReadBy().add(user);
        }
        if (!unread.isEmpty()) {
            messageRepository.saveAll(unread);
        }
    }

    @Transactional
    public void togglePin(Long userId, Long contactId, boolean isGroup) {
        User user = userRepository.findById(userId).orElseThrow();
        UserChatPreference pref = preferenceRepository.findByUserAndContactIdAndIsGroup(user, contactId, isGroup)
                .orElse(UserChatPreference.builder()
                        .user(user)
                        .contactId(contactId)
                        .isGroup(isGroup)
                        .pinned(false)
                        .build());
        
        pref.setPinned(!pref.isPinned());
        preferenceRepository.save(pref);
    }

    @Transactional
    public void clearHistory(Long userId, Long contactId, boolean isGroup) {
        if (isGroup) {
            ChatGroup group = groupRepository.findById(contactId).orElseThrow();
            List<Message> history = messageRepository.findByGroupOrderByCreatedAtAsc(group);
            // In groups, clearing history just soft deletes for the user
            User user = userRepository.findById(userId).orElseThrow();
            for (Message m : history) {
                m.getDeletedBy().add(user);
                messageRepository.save(m);
            }
        } else {
            User user1 = userRepository.findById(userId).orElseThrow();
            User user2 = userRepository.findById(contactId).orElseThrow();
            List<Message> history = messageRepository.findChatHistory(user1, user2);
            
            for (Message m : history) {
                m.getDeletedBy().add(user1);
                // Si ambos usuarios han borrado el mensaje, se borra de la DB para optimizar espacio
                if (m.getDeletedBy().size() >= 2) {
                    messageRepository.delete(m);
                } else {
                    messageRepository.save(m);
                }
            }
            // Ensure preference records exist so the chat entry stays visible in the list
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            updateLastActivity(user1, contactId, false, now);
            updateLastActivity(user2, userId, false, now);
        }
    }

    private void ensurePreferenceExists(User user, Long contactId, boolean isGroup) {
        if (!preferenceRepository.existsByUserAndContactIdAndIsGroup(user, contactId, isGroup)) {
            updateLastActivity(user, contactId, isGroup, java.time.LocalDateTime.now());
        }
    }

    @Transactional
    public void deleteConversation(Long userId, Long contactId, boolean isGroup) {
        // For individual chats: soft delete all messages, hard delete if both deleted
        if (!isGroup) {
            User user1 = userRepository.findById(userId).orElseThrow();
            User user2 = userRepository.findById(contactId).orElseThrow();
            List<Message> history = messageRepository.findChatHistory(user1, user2);
            for (Message m : history) {
                m.getDeletedBy().add(user1);
                if (m.getDeletedBy().size() >= 2) {
                    messageRepository.delete(m);
                } else {
                    messageRepository.save(m);
                }
            }
        } else {
            // For groups, soft delete
            User user = userRepository.findById(userId).orElseThrow();
            ChatGroup group = groupRepository.findById(contactId).orElseThrow();
            List<Message> history = messageRepository.findByGroupOrderByCreatedAtAsc(group);
            for (Message m : history) {
                m.getDeletedBy().add(user);
                messageRepository.save(m);
            }
        }
        // Remove pin preference if exists
        User user = userRepository.findById(userId).orElseThrow();
        preferenceRepository.findByUserAndContactIdAndIsGroup(user, contactId, isGroup)
                .ifPresent(preferenceRepository::delete);
    }

    public List<com.inncontrol.dto.GroupMemberDTO> getGroupMembers(Long groupId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        return group.getMembers().stream()
                .map(u -> com.inncontrol.dto.GroupMemberDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .role(u.getRole().name().replace("ROLE_", ""))
                        .photo(u.getPhoto())
                        .isAdmin(group.getAdmins().contains(u) || (group.getCreator() != null && group.getCreator().getId().equals(u.getId())))
                        .build())
                .sorted(Comparator.comparing(com.inncontrol.dto.GroupMemberDTO::getName))
                .collect(Collectors.toList());
    }

    public ChatGroup createGroup(String name, String description, String photo, List<Long> memberIds, Long creatorId) {
        User creator = userRepository.findById(creatorId).orElseThrow();
        List<User> members = userRepository.findAllById(memberIds);
        if (!members.contains(creator)) members.add(creator);

        // Safety check for photo size
        if (photo != null && photo.length() > 1024 * 1024) {
            throw new RuntimeException("La imagen del grupo es demasiado grande (máximo 1MB)");
        }

        ChatGroup group = ChatGroup.builder()
                .name(name)
                .description(description)
                .photo(photo)
                .creator(creator)
                .members(new java.util.HashSet<>(members))
                .admins(new java.util.HashSet<>(java.util.List.of(creator)))
                .build();
        
        ChatGroup savedGroup = groupRepository.save(group);

        // Mensaje de sistema: Creación del grupo
        Message creationMsg = Message.builder()
                .sender(creator)
                .receiver(creator) // TRUCO: Cumplir con la base de datos
                .group(savedGroup)
                .content("SYSTEM_GROUP_CREATED")
                .type("SYSTEM")
                .build();
        messageRepository.save(creationMsg);

        // Mensajes de sistema: Integrantes añadidos
        for (User member : members) {
            if (!member.getId().equals(creatorId)) {
                Message addedMsg = Message.builder()
                        .sender(creator)
                        .receiver(member)
                        .group(savedGroup)
                        .content("SYSTEM_MEMBER_ADDED:" + member.getName())
                        .type("SYSTEM")
                        .build();
                messageRepository.save(addedMsg);
            }
        }

        return savedGroup;
    }

    @Transactional
    public void deleteGroup(Long groupId, Long requesterId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User requester = userRepository.findById(requesterId).orElseThrow();
        
        // Solo administradores (o específicamente el creador) pueden eliminar el grupo
        boolean isAdmin = group.getAdmins().contains(requester) || 
                          (group.getCreator() != null && group.getCreator().getId().equals(requesterId));

        if (!isAdmin) {
            throw new RuntimeException("Solo el administrador puede eliminar el grupo");
        }

        group.setDeleted(true);
        
        // Mensaje de sistema: El administrador eliminó el grupo
        Message deleteMsg = Message.builder()
                .sender(requester)
                .receiver(requester) // Placeholder
                .group(group)
                .content("SYSTEM_GROUP_DELETED")
                .type("SYSTEM")
                .build();
        messageRepository.save(deleteMsg);

        groupRepository.save(group);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        // Mensaje de sistema: El usuario salió
        Message leaveMsg = Message.builder()
                .sender(user)
                .receiver(user) // Para cumplir con el esquema
                .group(group)
                .content("SYSTEM_MEMBER_LEFT:" + user.getName())
                .type("SYSTEM")
                .build();
        messageRepository.save(leaveMsg);

        // Asegurar que el chat se mantenga en la lista con la actividad del mensaje de salida
        updateLastActivity(user, groupId, true, java.time.LocalDateTime.now());

        boolean isCreator = group.getCreator() != null && group.getCreator().getId().equals(userId);
        
        group.getMembers().remove(user);
        
        // If the creator is leaving, transfer admin to the first remaining member
        if (isCreator && !group.getMembers().isEmpty()) {
            User newAdmin = group.getMembers().stream()
                    .min(Comparator.comparing(User::getId)) // First registered member
                    .orElse(null);
            if (newAdmin != null) {
                group.setCreator(newAdmin);
                // System message
                Message sysMsg = Message.builder()
                        .sender(newAdmin)
                        .receiver(newAdmin)
                        .group(group)
                        .content("SYSTEM_NEW_ADMIN:" + newAdmin.getName())
                        .type("SYSTEM")
                        .build();
                messageRepository.save(sysMsg);
            }
        }
        
        // If no members left, delete the group
        if (group.getMembers().isEmpty()) {
            List<Message> msgs = messageRepository.findByGroupOrderByCreatedAtAsc(group);
            messageRepository.deleteAll(msgs);
            groupRepository.delete(group);
        } else {
            groupRepository.save(group);
        }
    }
    @Transactional
    public void removeFromGroup(Long groupId, Long memberId, Long adminId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        
        // Solo administradores pueden eliminar miembros
        boolean isAdmin = group.getAdmins().contains(admin) || 
                          (group.getCreator() != null && group.getCreator().getId().equals(adminId));

        if (!isAdmin) {
            throw new RuntimeException("Solo los administradores pueden eliminar miembros");
        }
        
        if (memberId.equals(adminId)) {
            throw new RuntimeException("No puedes eliminarte a ti mismo. Usa 'Salir del grupo'.");
        }

        User memberToRemove = userRepository.findById(memberId).orElseThrow();
        group.getMembers().remove(memberToRemove);
        
        // Mensaje de sistema
        Message sysMsg = Message.builder()
                .sender(group.getCreator())
                .receiver(memberToRemove)
                .group(group)
                .content("SYSTEM_MEMBER_REMOVED:" + memberToRemove.getName())
                .type("SYSTEM")
                .build();
        messageRepository.save(sysMsg);

        groupRepository.save(group);
    }

    @Transactional
    public void addMember(Long groupId, Long memberId, Long requesterId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User requester = userRepository.findById(requesterId).orElseThrow();
        
        // Cualquier miembro puede añadir a otros (Requerimiento: "los miembros si pueden añadir a otras personas")
        if (!group.getMembers().contains(requester)) {
            throw new RuntimeException("Debes ser miembro del grupo para añadir a otros");
        }

        User newMember = userRepository.findById(memberId).orElseThrow();
        if (group.getMembers().contains(newMember)) {
            throw new RuntimeException("El usuario ya es miembro del grupo");
        }

        group.getMembers().add(newMember);
        
        // Mensaje de sistema
        Message sysMsg = Message.builder()
                .sender(group.getCreator())
                .receiver(newMember)
                .group(group)
                .content("SYSTEM_MEMBER_ADDED:" + newMember.getName())
                .type("SYSTEM")
                .build();
        messageRepository.save(sysMsg);

        groupRepository.save(group);
    }

    @Transactional
    public void promoteToAdmin(Long groupId, Long memberId, Long adminId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User requester = userRepository.findById(adminId).orElseThrow();
        
        // Solo los administradores actuales pueden nombrar a otros administradores
        boolean isRequesterAdmin = group.getAdmins().contains(requester) || 
                                 (group.getCreator() != null && group.getCreator().getId().equals(adminId));

        if (!isRequesterAdmin) {
            throw new RuntimeException("Solo los administradores pueden nombrar nuevos administradores");
        }

        User newAdmin = userRepository.findById(memberId).orElseThrow();
        if (!group.getMembers().contains(newAdmin)) {
            throw new RuntimeException("El usuario debe ser miembro del grupo para ser administrador");
        }

        group.getAdmins().add(newAdmin);
        
        // Mensaje de sistema
        Message sysMsg = Message.builder()
                .sender(requester)
                .receiver(newAdmin)
                .group(group)
                .content("SYSTEM_NEW_ADMIN:" + newAdmin.getName())
                .type("SYSTEM")
                .build();
        messageRepository.save(sysMsg);

        groupRepository.save(group);
    }

    @Transactional
    public void demoteAdmin(Long groupId, Long memberId, Long adminId) {
        ChatGroup group = groupRepository.findById(groupId).orElseThrow();
        User requester = userRepository.findById(adminId).orElseThrow();
        
        boolean isRequesterAdmin = group.getAdmins().contains(requester) || 
                                 (group.getCreator() != null && group.getCreator().getId().equals(adminId));

        if (!isRequesterAdmin) {
            throw new RuntimeException("Solo los administradores pueden degradar a otros");
        }

        User targetMember = userRepository.findById(memberId).orElseThrow();
        
        if (group.getCreator() != null && group.getCreator().getId().equals(memberId)) {
            throw new RuntimeException("No se puede quitar el rango de administrador al creador");
        }

        group.getAdmins().remove(targetMember);
        
        Message sysMsg = Message.builder()
                .sender(requester)
                .receiver(targetMember)
                .group(group)
                .content("SYSTEM_ADMIN_REMOVED:" + targetMember.getName())
                .type("SYSTEM")
                .build();
        messageRepository.save(sysMsg);

        groupRepository.save(group);
    }
    private void createOrUpdateTaskMessage(User sender, User receiver, String content, java.time.LocalDateTime now, String taskIdPattern) {
        java.util.List<Message> msgs = messageRepository.findExistingTaskMessages(sender.getId(), receiver.getId(), taskIdPattern);
        Message msg;
        if (!msgs.isEmpty()) {
            msg = msgs.get(0);
            msg.setContent(content);
            msg.setCreatedAt(now);
            msg.setRead(false);
            if (msg.getReadBy() != null) msg.getReadBy().clear();
            if (msg.getDeletedBy() != null) msg.getDeletedBy().clear();
            
            if (msgs.size() > 1) {
                messageRepository.deleteAll(msgs.subList(1, msgs.size()));
            }
        } else {
            msg = Message.builder()
                    .sender(sender).receiver(receiver)
                    .content(content).type("SYSTEM_TASK").createdAt(now).build();
        }
        messageRepository.save(msg);
        messagingTemplate.convertAndSendToUser(receiver.getId().toString(), "/queue/messages", msg);
        updateLastActivity(receiver, sender.getId(), false, now);
        updateLastActivity(sender, receiver.getId(), false, now);
    }

    @Transactional
    public void sendTaskNotification(Task task, String actionType, Long actorId) {
        User systemUser = userRepository.findByName("InnControl Assistant")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("InnControl Assistant")
                        .email("system@inncontrol.com")
                        .role(Role.ROLE_EMPLEADO)
                        .password("system_internal")
                        .build()));

        User manager = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_GERENTE && !u.getEmail().equals("system@inncontrol.com"))
                .findFirst().orElse(null);

        if (manager == null) return;

        String content = String.format("TASK_EVENT|%s|%d|%s|%s|%s", 
            actionType, task.getId(), task.getTitle(), 
            (task.getAssignedTo() != null ? task.getAssignedTo().getName() : "Sin asignar"),
            task.getPriority().name());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String taskIdPattern = "%|" + task.getId() + "|%";

        // Notify Manager only if he is NOT the actor
        if (!manager.getId().equals(actorId)) {
            createOrUpdateTaskMessage(systemUser, manager, content, now, taskIdPattern);
        }

        // Notify Employee if assigned, not the actor, and not the manager (already checked)
        if (task.getAssignedTo() != null && 
            !task.getAssignedTo().getId().equals(actorId) && 
            !task.getAssignedTo().getId().equals(manager.getId())) {
            createOrUpdateTaskMessage(systemUser, task.getAssignedTo(), content, now, taskIdPattern);
        }
    }

    @Transactional
    public void deleteSystemTaskMessages(Long taskId) {
        String taskIdPattern = "%|" + taskId + "|%";
        messageRepository.deleteSystemTaskMessages(taskIdPattern);
    }

    public java.util.Map<String, Integer> getNotificationBadges(Long userId, Long lastVisitedTasks, Long lastVisitedMessages) {
        User user = userRepository.findById(userId).orElseThrow();
        
        java.time.LocalDateTime lastVisitedT = java.time.Instant.ofEpochMilli(lastVisitedTasks)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        
        java.time.LocalDateTime lastVisitedM = java.time.Instant.ofEpochMilli(lastVisitedMessages)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();

        int unreadMessages = (int) (messageRepository.countTotalUnreadPrivateMessagesSince(userId, lastVisitedM) + 
                                   messageRepository.countTotalUnreadGroupMessagesSince(userId, user, lastVisitedM));
        
        int newTasks = (int) taskRepository.countByAssignedToIdAndStatusAndCreatedAtGreaterThan(
                userId, TaskStatus.PENDIENTE, lastVisitedT);
        
        java.util.Map<String, Integer> badges = new java.util.HashMap<>();
        badges.put("messages", unreadMessages);
        badges.put("tasks", newTasks);
        return badges;
    }
}
