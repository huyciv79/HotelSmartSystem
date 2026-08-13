package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.Min;
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

    @Size(max = 20, message = "Số phòng không được vượt quá 20 ký tự")
    private String roomNumber;

    @Min(value = 1, message = "Số tầng phải từ 1 trở lên")
    private Integer floorNumber;

    @Size(max = 50, message = "Tình trạng phòng không được vượt quá 50 ký tự")
    private String status;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @Size(max = 100, message = "Mã kiểm soát admin không được vượt quá 100 ký tự")
    private String adminPasscode;

    private Integer roomTypeId;

    @Min(value = 1, message = "Sức chứa người lớn phải lớn hơn 0")
    private Integer adultCapacity;

    @Min(value = 0, message = "Sức chứa trẻ em không được âm")
    private Integer childCapacity;

    private Integer totalCapacity;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = false, message = "Diện tích phải lớn hơn 0")
    private java.math.BigDecimal area;

    @Size(max = 100, message = "Kiểu giường không được vượt quá 100 ký tự")
    private String bedType;

    @Min(value = 1, message = "Số lượng giường phải từ 1 trở lên")
    private Integer bedCount;
}

