package com.example.hotelsmartbookingbackend.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomFilterCriteria {
    private String keyword;
    private String status;
    private Integer roomTypeId;
    private Integer floorNumber;
}

