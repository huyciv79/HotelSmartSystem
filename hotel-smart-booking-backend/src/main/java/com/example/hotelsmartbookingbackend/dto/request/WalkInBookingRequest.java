package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalkInBookingRequest {

    @NotNull(message = "Vui lòng chọn loại phòng")
    private Integer roomTypeId;

    @NotNull(message = "Vui lòng chọn ngày nhận phòng")
    private LocalDate checkInDate;

    @NotNull(message = "Vui lòng chọn ngày trả phòng")
    private LocalDate checkOutDate;

    @NotNull(message = "Vui lòng nhập số người lớn")
    @Positive(message = "Số người lớn phải lớn hơn 0")
    private Integer numberOfAdults;

    @NotNull(message = "Vui lòng nhập số trẻ em")
    @PositiveOrZero(message = "Số trẻ em không được âm")
    private Integer numberOfChildren;

    @NotNull(message = "Vui lòng nhập số lượng phòng")
    @Positive(message = "Số lượng phòng phải lớn hơn 0")
    private Integer quantity;

    private String specialRequests;

    @NotBlank(message = "Vui lòng nhập họ tên khách hàng")
    private String customerFullname;

    private String customerEmail;

    private String customerPhonenumber;

    @NotBlank(message = "Vui lòng nhập số CCCD khách hàng")
    @Pattern(regexp = "^[0-9]{12}$", message = "Số CCCD phải gồm đúng 12 chữ số")
    private String customerIdCardNumber;

    private BigDecimal paidAmount;

    private String paymentMethod;
}
