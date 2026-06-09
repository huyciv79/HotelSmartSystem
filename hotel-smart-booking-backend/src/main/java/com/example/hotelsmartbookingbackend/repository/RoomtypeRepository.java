package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Roomtype;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomtypeRepository extends JpaRepository<Roomtype, Integer>, JpaSpecificationExecutor<Roomtype> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Roomtype r where r.id = :id")
    Optional<Roomtype> findByIdForUpdate(@Param("id") Integer id);
}
