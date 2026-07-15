package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.service.RoomService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import com.example.hotelsmartbookingbackend.specification.RoomSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.hotelsmartbookingbackend.dto.response.RoomStatusResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final WebSocketService webSocketService;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "Available",
            "Occupied",
            "Cleaning",
            "Maintenance",
            "Reserved"
    );
    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryDTO> getRoomList(RoomFilterCriteria criteria, Pageable pageable) {
        Specification<Room> spec = RoomSpecification.withFilters(criteria);
        Page<Room> page = roomRepository.findAll(spec, pageable);

        List<RoomSummaryDTO> content = page.getContent().stream()
                .map(this::mapToSummaryDTO)
                .toList();

        return PageResponse.<RoomSummaryDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDetailDTO getRoomDetail(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với mã: " + id));

        return mapToDetailDTO(room);
    }

    @Override
    @Transactional
    public RoomDetailDTO updateRoom(Integer id, UpdateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với mã: " + id));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String provided = request.getStatus().trim();
            boolean ok = ALLOWED_STATUSES.stream().anyMatch(s -> s.equalsIgnoreCase(provided));
            if (!ok) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ. Giá trị hợp lệ: " + ALLOWED_STATUSES);
            }
            // normalize to canonical case
            String matched = ALLOWED_STATUSES.stream().filter(s -> s.equalsIgnoreCase(provided)).findFirst().get();
            room.setStatus(matched);
        }

        if (request.getNote() != null) {
            room.setNote(request.getNote());
        }

        if (request.getAdminPasscode() != null) {
            room.setAdminPasscode(request.getAdminPasscode());
        }

        if (request.getRoomTypeId() != null) {
            RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với mã: " + request.getRoomTypeId()));
            room.setRoomType(roomType);
        }

        room.setUpdatedAt(Instant.now());
        Room updatedRoom = roomRepository.save(room);

        // Broadcast status update via WebSocket
        webSocketService.broadcastRoomStatus(updatedRoom.getId(), updatedRoom.getRoomNumber(), updatedRoom.getStatus());

        return mapToDetailDTO(updatedRoom);
    }

    private RoomSummaryDTO mapToSummaryDTO(Room room) {
        return RoomSummaryDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floorNumber(room.getFloorNumber())
                .status(room.getStatus())
                .roomTypeId(room.getRoomType().getId())
                .roomTypeName(room.getRoomType().getName())
                .adminPasscode(room.getAdminPasscode())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private RoomDetailDTO mapToDetailDTO(Room room) {
        return RoomDetailDTO.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floorNumber(room.getFloorNumber())
                .status(room.getStatus())
                .note(room.getNote())
                .adminPasscode(room.getAdminPasscode())
                .roomTypeId(room.getRoomType().getId())
                .roomTypeName(room.getRoomType().getName())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<RoomStatusResponse> getAllRoomStatuses() {

        return roomRepository.findAll()
                .stream()
                .map(room ->
                        RoomStatusResponse.builder()
                                .roomId(room.getId())
                                .roomNumber(room.getRoomNumber())
                                .status(room.getStatus())
                                .updatedAt(room.getUpdatedAt())
                                .build())
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public RoomStatusResponse getRoomStatus(Integer roomId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy phòng với mã: " + roomId
                        ));

        return RoomStatusResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .status(room.getStatus())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
    @Override
    @Transactional
    public RoomStatusResponse updateRoomStatus(
            Integer roomId,
            UpdateRoomRequest request
    ) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy phòng với mã: " + roomId
                        ));

        String provided = request.getStatus();

        if (provided == null || provided.isBlank()) {
            throw new IllegalArgumentException(
                    "Trạng thái phòng không được để trống"
            );
        }

        boolean valid = ALLOWED_STATUSES
                .stream()
                .anyMatch(
                        s -> s.equalsIgnoreCase(provided)
                );

        if (!valid) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ"
            );
        }

        String matched = ALLOWED_STATUSES
                .stream()
                .filter(
                        s -> s.equalsIgnoreCase(provided)
                )
                .findFirst()
                .get();

        room.setStatus(matched);
        room.setUpdatedAt(Instant.now());

        roomRepository.save(room);

        RoomStatusResponse response =
                RoomStatusResponse.builder()
                        .roomId(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .status(room.getStatus())
                        .updatedAt(room.getUpdatedAt())
                        .build();

        return response;
    }
}
