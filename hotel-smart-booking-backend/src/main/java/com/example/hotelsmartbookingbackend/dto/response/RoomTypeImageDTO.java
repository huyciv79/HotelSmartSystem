package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomTypeImageDTO {
    private Integer id;
    private String imageUrl;
    private Boolean primary;
    private Integer displayOrder;
}
