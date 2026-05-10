package com.inncontrol.repository;

import com.inncontrol.model.Message;
import com.inncontrol.model.User;
import com.inncontrol.model.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // History for User-to-User
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(User user1, User user2);

    // History for Group
    List<Message> findByGroupOrderByCreatedAtAsc(ChatGroup group);

    // Last message for User-to-User sorting (only individual messages, excluding deleted by viewer)
    @Query(value = "SELECT m.* FROM messages m " +
                   "LEFT JOIN message_deletions md ON m.id = md.message_id AND md.user_id = :viewerId " +
                   "WHERE ((m.sender_id = :viewerId AND m.receiver_id = :otherId) OR (m.sender_id = :otherId AND m.receiver_id = :viewerId)) " +
                   "AND m.group_id IS NULL AND md.user_id IS NULL " +
                   "ORDER BY m.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Message> findLastMessage(@Param("viewerId") Long viewerId, @Param("otherId") Long otherId);

    // Last message for Group sorting (excluding deleted by viewer)
    @Query(value = "SELECT m.* FROM messages m " +
                   "LEFT JOIN message_deletions md ON m.id = md.message_id AND md.user_id = :viewerId " +
                   "WHERE m.group_id = :groupId AND md.user_id IS NULL " +
                   "ORDER BY m.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Message> findLastGroupMessage(@Param("groupId") Long groupId, @Param("viewerId") Long viewerId);

    @Query("SELECT m FROM Message m WHERE m.group = :group AND m.sender != :user AND :user NOT MEMBER OF m.readBy AND (m.type IS NULL OR m.type != 'SYSTEM')")
    List<Message> findUnreadGroupMessages(ChatGroup group, User user);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.read = true WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.read = false")
    void markAsRead(Long senderId, Long receiverId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.read = false")
    long countUnreadMessages(Long senderId, Long receiverId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.group = :group AND m.sender != :user AND :user NOT MEMBER OF m.readBy AND (m.type IS NULL OR m.type != 'SYSTEM')")
    long countUnreadGroupMessages(ChatGroup group, User user);

    @Query("SELECT m FROM Message m WHERE m.type = 'SYSTEM_TASK' AND m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.content LIKE :taskIdPattern ORDER BY m.createdAt DESC")
    List<Message> findExistingTaskMessages(Long senderId, Long receiverId, String taskIdPattern);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.read = false AND m.group IS NULL AND m.createdAt > :since")
    long countTotalUnreadPrivateMessagesSince(Long userId, java.time.LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.group.id IN (SELECT g.id FROM ChatGroup g JOIN g.members u WHERE u.id = :userId) AND m.sender.id != :userId AND :user NOT MEMBER OF m.readBy AND (m.type IS NULL OR m.type != 'SYSTEM') AND m.createdAt > :since")
    long countTotalUnreadGroupMessagesSince(Long userId, User user, java.time.LocalDateTime since);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.type = 'SYSTEM_TASK' AND m.content LIKE :taskIdPattern")
    void deleteSystemTaskMessages(String taskIdPattern);
}
