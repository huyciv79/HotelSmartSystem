package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FeedbackRequest {

    @NotNull(message = "Mã đặt phòng không được để trống")
    private Integer bookingId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá thấp nhất là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá cao nhất là 5 sao")
    private Integer rating;

    private String comment;

    @Size(max = 500, message = "Ưu điểm không vượt quá 500 ký tự")
    private String pros;

    @Size(max = 500, message = "Nhược điểm không vượt quá 500 ký tự")
    private String cons;

    private List<String> images;
}
