package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Roomtypeimage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomtypeimageRepository extends JpaRepository<Roomtypeimage, Integer> {

    List<Roomtypeimage> findByRoomtypeid_IdOrderByDisplayorderAsc(Integer roomTypeId);

    Optional<Roomtypeimage> findFirstByRoomtypeid_IdAndIsprimaryTrue(Integer roomTypeId);
}
