package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer>, JpaSpecificationExecutor<Room> {

    long countByRoomtypeid_IdAndStatus(Integer roomTypeId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Room> findFirstByRoomtypeid_IdAndStatusOrderByRoomnumberAsc(
            Integer roomTypeId,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Room> findByRoomtypeid_IdAndStatusOrderByRoomnumberAsc(
            Integer roomTypeId,
            String status
    );
}
