package com.example.hotelsmartbookingbackend.specification;

import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import org.springframework.data.jpa.domain.Specification;

public final class RoomTypeSpecification {

    private RoomTypeSpecification() {
    }

    public static Specification<Roomtype> withFilters(RoomTypeFilterCriteria criteria) {
        return Specification.allOf(
                hasStatus(criteria.getStatus()),
                nameContains(criteria.getKeyword()),
                minPrice(criteria.getMinPrice()),
                maxPrice(criteria.getMaxPrice()),
                minAdults(criteria.getMinAdults()),
                minTotalCapacity(criteria.getMinTotalCapacity())
        );
    }

    private static Specification<Roomtype> hasStatus(String status) {
        return (root, query, cb) -> {
            if ("all".equalsIgnoreCase(status)) {
                return cb.conjunction();
            }
            String effectiveStatus = (status == null || status.isBlank()) ? "Active" : status;
            return cb.equal(cb.lower(root.get("status")), effectiveStatus.toLowerCase());
        };
    }

    private static Specification<Roomtype> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Roomtype> minPrice(java.math.BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("baseprice"), minPrice);
        };
    }

    private static Specification<Roomtype> maxPrice(java.math.BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("baseprice"), maxPrice);
        };
    }

    private static Specification<Roomtype> minAdults(Integer minAdults) {
        return (root, query, cb) -> {
            if (minAdults == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("adultcapacity"), minAdults);
        };
    }

    private static Specification<Roomtype> minTotalCapacity(Integer minTotalCapacity) {
        return (root, query, cb) -> {
            if (minTotalCapacity == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("totalcapacity"), minTotalCapacity);
        };
    }
}
