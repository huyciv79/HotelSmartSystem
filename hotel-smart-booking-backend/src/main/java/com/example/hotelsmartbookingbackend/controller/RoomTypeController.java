package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryDTO;
import com.example.hotelsmartbookingbackend.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomTypeSummaryDTO>>> getRoomTypeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "baseprice") String sortBy,
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

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<RoomTypeSummaryDTO> result = roomTypeService.getRoomTypeList(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach loai phong thanh cong", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> getRoomTypeDetail(@PathVariable Integer id) {
        RoomTypeDetailDTO detail = roomTypeService.getRoomTypeDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lay chi tiet loai phong thanh cong", detail));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> createRoomType(
            @Valid @ModelAttribute CreateRoomTypeRequest request) {
        RoomTypeDetailDTO created = roomTypeService.createRoomType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao loai phong thanh cong", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomTypeDetailDTO>> updateRoomType(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateRoomTypeRequest request) {
        RoomTypeDetailDTO updated = roomTypeService.updateRoomType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat loai phong thanh cong", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(@PathVariable Integer id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa mem loai phong thanh cong", null));
    }
}
