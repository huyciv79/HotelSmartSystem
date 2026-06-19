package com.example.hotelsmartbookingbackend.config;

import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;

@Configuration
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner seedDatabase(BookingRepository bookingRepository) {
        return args -> {
            try {
                if (bookingRepository.findById(1).isEmpty()) {
                    Booking booking = new Booking();
                    booking.setId(1);
                    booking.setBookingreference("BOOK-PAYPAL-TEST");
                    booking.setBookingtype("Online");
                    booking.setCheckinmethod("Manual");
                    booking.setTotalamount(new BigDecimal("100.00"));
                    booking.setPaidamount(BigDecimal.ZERO);
                    booking.setDepositamount(new BigDecimal("20.00"));
                    booking.setDiscountamount(BigDecimal.ZERO);
                    booking.setTaxamount(BigDecimal.ZERO);
                    booking.setServicechargeamount(BigDecimal.ZERO);
                    booking.setFinalamount(new BigDecimal("100.00"));
                    booking.setStatus("Pending");
                    booking.setCreatedat(Instant.now());
                    booking.setUpdatedat(Instant.now());
                    
                    bookingRepository.save(booking);
                    System.out.println("---- DATABASE SEEDED: Created sample booking with ID 1 ----");
                }
            } catch (Exception e) {
                System.err.println("Error seeding database: " + e.getMessage());
            }
        };
    }
}
