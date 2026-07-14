package com.example.hotelsmartbookingbackend.specification;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.entity.Room;
import org.springframework.data.jpa.domain.Specification;

public final class RoomSpecification {

    private RoomSpecification() {
    }

    public static Specification<Room> withFilters(RoomFilterCriteria criteria) {
        return Specification.allOf(
                hasStatus(criteria.getStatus()),
                roomNumberContains(criteria.getKeyword()),
                byRoomType(criteria.getRoomTypeId()),
                byFloorNumber(criteria.getFloorNumber())
        );
    }

    private static Specification<Room> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("status")), status.toLowerCase());
        };
    }

    private static Specification<Room> roomNumberContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("roomNumber")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Room> byRoomType(Integer roomTypeId) {
        return (root, query, cb) -> {
            if (roomTypeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("roomType").get("id"), roomTypeId);
        };
    }

    private static Specification<Room> byFloorNumber(Integer floorNumber) {
        return (root, query, cb) -> {
            if (floorNumber == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("floorNumber"), floorNumber);
        };
    }
}

