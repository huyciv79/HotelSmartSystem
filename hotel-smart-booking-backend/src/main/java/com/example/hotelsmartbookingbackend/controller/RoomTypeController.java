package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryResponse;
import com.example.hotelsmartbookingbackend.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomTypeSummaryResponse>>> getRoomTypeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "basePrice") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minAdults,
            @RequestParam(required = false) Integer minTotalCapacity,
            @RequestParam(required = false) String status) {

        RoomTypeFilterCriteria criteria = RoomTypeFilterCriteria.builder()
                .keyword(keyword)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minAdults(minAdults)
                .minTotalCapacity(minTotalCapacity)
                .status(status)
                .build();

        String sortField = sortBy;
        if (sortBy.equalsIgnoreCase("baseprice")) {
            sortField = "basePrice";
        } else if (sortBy.equalsIgnoreCase("adultcapacity")) {
            sortField = "adultCapacity";
        } else if (sortBy.equalsIgnoreCase("childcapacity")) {
            sortField = "childCapacity";
        } else if (sortBy.equalsIgnoreCase("roomtypeid") || sortBy.equalsIgnoreCase("id")) {
            sortField = "id";
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<RoomTypeSummaryResponse> result =
                roomTypeService.getRoomTypeList(criteria, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách loại phòng thành công", result)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeDetailResponse>> getRoomTypeDetail(
            @PathVariable Integer id) {

        RoomTypeDetailResponse detail = roomTypeService.getRoomTypeDetail(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết loại phòng thành công", detail)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomTypeDetailResponse>> createRoomType(
            @Valid @ModelAttribute CreateRoomTypeRequest request) {

        RoomTypeDetailResponse created = roomTypeService.createRoomType(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo loại phòng thành công", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomTypeDetailResponse>> updateRoomType(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateRoomTypeRequest request) {

        RoomTypeDetailResponse updated = roomTypeService.updateRoomType(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật loại phòng thành công", updated)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(@PathVariable Integer id) {
        roomTypeService.deleteRoomType(id);

        return ResponseEntity.ok(
                ApiResponse.success("Xóa mềm loại phòng thành công", null)
        );
    }
}