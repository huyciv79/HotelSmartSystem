package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateRoomTypeRequest {

    @Size(max = 100, message = "Ten loai phong khong duoc vuot qua 100 ky tu")
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Gia co ban phai lon hon 0")
    private BigDecimal basePrice;

    private Integer adultCapacity;

    private Integer childCapacity;

    private BigDecimal area;

    private String bedType;

    private String amenities;

    @Size(max = 50, message = "Trang thai khong duoc vuot qua 50 ky tu")
    private String status;

    private Boolean replaceImages;

    private List<MultipartFile> images;
}
