package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoomRepository extends JpaRepository<Room, Integer>, JpaSpecificationExecutor<Room> {

    long countByRoomtypeid_IdAndStatus(Integer roomTypeId, String status);
}
