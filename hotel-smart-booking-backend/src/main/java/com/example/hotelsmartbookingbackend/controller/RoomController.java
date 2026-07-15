package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import com.example.hotelsmartbookingbackend.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.hotelsmartbookingbackend.dto.response.RoomStatusResponse;
import java.util.List;


@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryDTO>>> getRoomList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "roomNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer roomTypeId,
            @RequestParam(required = false) Integer floorNumber) {

        RoomFilterCriteria criteria = RoomFilterCriteria.builder()
                .keyword(keyword)
                .status(status)
                .roomTypeId(roomTypeId)
                .floorNumber(floorNumber)
                .build();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<RoomSummaryDTO> result = roomService.getRoomList(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDetailDTO>> getRoomDetail(@PathVariable Integer id) {
        RoomDetailDTO detail = roomService.getRoomDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết phòng thành công", detail));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDetailDTO>> updateRoom(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomDetailDTO updated = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng thành công", updated));
    }
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<RoomStatusResponse>>> getAllRoomStatuses() {

        List<RoomStatusResponse> result =
                roomService.getAllRoomStatuses();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy trạng thái phòng thành công",
                        result
                )
        );
    }
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomStatusResponse>> getRoomStatus(
            @PathVariable Integer id
    ) {

        RoomStatusResponse result =
                roomService.getRoomStatus(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy trạng thái phòng thành công",
                        result
                )
        );
    }
}

