package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRoomTypeRequest {

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 100, message = "Tên loại phòng không được vượt quá 100 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá cơ bản phải lớn hơn 0")
    private BigDecimal basePrice;

    @Min(value = 1, message = "Sức chứa người lớn phải từ 1 người trở lên")
    private Integer adultCapacity;

    @Min(value = 0, message = "Sức chứa trẻ em không được là số âm")
    private Integer childCapacity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Diện tích phòng phải lớn hơn 0")
    private BigDecimal area;

    private String bedType;

    private String amenities;

    @Size(max = 50, message = "Trạng thái không được vượt quá 50 ký tự")
    private String status;

    @NotEmpty(message = "Vui lòng tải lên ít nhất một ảnh loại phòng")
    private List<MultipartFile> images;
}
