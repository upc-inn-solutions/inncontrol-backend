package com.inncontrol.repository;

import com.inncontrol.model.Room;
import com.inncontrol.model.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumber(String number);
    List<Room> findByStatus(RoomStatus status);
}
