package com.example.hotelsmartbookingbackend.repository;

import com.example.hotelsmartbookingbackend.entity.Customerrequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerrequestRepository extends JpaRepository<Customerrequest, Integer> {
    List<Customerrequest> findByBookingid_Id(Integer bookingId);
    List<Customerrequest> findByRequesttypeAndStatusOrderByCreatedatDesc(String requesttype, String status);
}
