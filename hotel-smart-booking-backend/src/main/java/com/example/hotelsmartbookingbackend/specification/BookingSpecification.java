package com.example.hotelsmartbookingbackend.specification;

import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BookingSpecification implements Specification<Booking> {
    
    private final BookingFilter criteria;
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    @Override
    public Predicate toPredicate(Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("userid", jakarta.persistence.criteria.JoinType.LEFT);
        }

        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }

        if (criteria.getBookingReference() != null && !criteria.getBookingReference().isEmpty()) {
            predicates.add(cb.like(root.get("bookingreference"), "%" + criteria.getBookingReference() + "%"));
        }

        if (criteria.getGuestEmail() != null && !criteria.getGuestEmail().isEmpty()) {
            predicates.add(cb.like(root.get("userid").get("email"), "%" + criteria.getGuestEmail() + "%"));
        }

        if (criteria.getGuestName() != null && !criteria.getGuestName().isEmpty()) {
            predicates.add(cb.like(root.get("userid").get("fullname"), "%" + criteria.getGuestName() + "%"));
        }

        if (criteria.getBookingType() != null && !criteria.getBookingType().isEmpty()) {
            predicates.add(cb.equal(root.get("bookingtype"), criteria.getBookingType()));
        }

        if (criteria.getCheckInDateFrom() != null) {
            Instant startOfDay = criteria.getCheckInDateFrom().atStartOfDay(HOTEL_ZONE).toInstant();
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdat"), startOfDay));
        }

        if (criteria.getCheckInDateTo() != null) {
            Instant endOfDay = criteria.getCheckInDateTo().atTime(LocalTime.MAX).atZone(HOTEL_ZONE).toInstant();
            predicates.add(cb.lessThanOrEqualTo(root.get("createdat"), endOfDay));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
