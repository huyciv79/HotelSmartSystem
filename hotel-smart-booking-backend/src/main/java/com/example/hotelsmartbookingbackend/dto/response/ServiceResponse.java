package com.example.hotelsmartbookingbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ServiceResponse {

    private Integer id;

    private String name;

    private String description;

    private BigDecimal price;

    private String unit;

    private Boolean isactive;

    private Instant createdAt;

    private Instant updatedAt;
}