package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Integer> {

    List<RoomTypeImage> findByRoomType_IdOrderByDisplayOrderAsc(Integer roomTypeId);

    Optional<RoomTypeImage> findFirstByRoomType_IdAndIsPrimaryTrue(Integer roomTypeId);
}
