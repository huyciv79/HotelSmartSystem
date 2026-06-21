package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Vui lòng chọn loại phòng")
    private Integer roomTypeId;

    @NotNull(message = "Vui lòng chọn ngày nhận phòng")
    @FutureOrPresent(message = "Ngày nhận phòng không được là ngày trong quá khứ")
    private LocalDate checkInDate;

    @NotNull(message = "Vui lòng chọn ngày trả phòng")
    private LocalDate checkOutDate;

    @NotNull(message = "Vui lòng nhập số người lớn")
    @Positive(message = "Số người lớn phải lớn hơn 0")
    private Integer numberOfAdults;

    @NotNull(message = "Vui lòng nhập số trẻ em")
    @PositiveOrZero(message = "Số trẻ em không được âm")
    private Integer numberOfChildren;

    @NotBlank(message = "Vui lòng chọn phương thức check-in")
    @Pattern(
            regexp = "FaceID|Face Recognition|Face ID|QR Code|Manual",
            message = "Phương thức check-in chỉ được là FaceID, QR Code hoặc Manual"
    )
    private String checkInMethod;

    @Size(max = 2000, message = "Yêu cầu đặc biệt không được vượt quá 2000 ký tự")
    private String specialRequests;
}
