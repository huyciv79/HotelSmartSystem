package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Roomtype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoomtypeRepository extends JpaRepository<Roomtype, Integer>, JpaSpecificationExecutor<Roomtype> {
}
