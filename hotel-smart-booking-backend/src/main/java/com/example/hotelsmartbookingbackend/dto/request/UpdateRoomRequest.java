package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoomRequest {

    @Size(max = 50, message = "Tình trạng phòng không được vượt quá 50 ký tự")
    private String status;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @Size(max = 100, message = "Mã kiểm soát admin không được vượt quá 100 ký tự")
    private String adminpasscode;

    private Integer roomtypeid;
}

