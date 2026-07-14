package com.example.hotelsmartbookingbackend.specification;

import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import org.springframework.data.jpa.domain.Specification;

public final class RoomTypeSpecification {

    private RoomTypeSpecification() {
    }

    public static Specification<RoomType> withFilters(RoomTypeFilterCriteria criteria) {
        return Specification.allOf(
                hasStatus(criteria.getStatus()),
                nameContains(criteria.getKeyword()),
                minPrice(criteria.getMinPrice()),
                maxPrice(criteria.getMaxPrice()),
                minAdults(criteria.getMinAdults()),
                minTotalCapacity(criteria.getMinTotalCapacity())
        );
    }

    private static Specification<RoomType> hasStatus(String status) {
        return (root, query, cb) -> {
            if ("all".equalsIgnoreCase(status)) {
                return cb.conjunction();
            }
            if (status != null && "all".equalsIgnoreCase(status.trim())) {
                return cb.conjunction();
            }

            String effectiveStatus = (status == null || status.isBlank()) ? "Active" : status;
            return cb.equal(cb.lower(root.get("status")), effectiveStatus.toLowerCase());
        };
    }

    private static Specification<RoomType> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    private static Specification<RoomType> minPrice(java.math.BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
        };
    }

    private static Specification<RoomType> maxPrice(java.math.BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
        };
    }

    private static Specification<RoomType> minAdults(Integer minAdults) {
        return (root, query, cb) -> {
            if (minAdults == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("adultCapacity"), minAdults);
        };
    }

    private static Specification<RoomType> minTotalCapacity(Integer minTotalCapacity) {
        return (root, query, cb) -> {
            if (minTotalCapacity == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("totalCapacity"), minTotalCapacity);
        };
    }
}
