package com.inncontrol.repository;

import com.inncontrol.model.Task;
import com.inncontrol.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User user);
    List<Task> findByRoomId(Long roomId);
    
    long countByAssignedToIdAndStatusAndCreatedAtGreaterThan(Long assignedToId, com.inncontrol.model.TaskStatus status, java.time.LocalDateTime createdAt);
}
