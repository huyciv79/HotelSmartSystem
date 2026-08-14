package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
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
    private final BookingDetailRepository bookingDetailRepository;
    private final WebSocketService webSocketService;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "Available",
            "Occupied",
            "Cleaning",
            "Maintenance",
            "Reserved",
            "Inactive"
    );

    @Override
    @Transactional
    public RoomDetailDTO createRoom(CreateRoomRequest request) {
        String roomNumber = request.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumberIgnoreCase(roomNumber)) {
            throw new RuntimeException("Số phòng '" + roomNumber + "' đã tồn tại trong hệ thống");
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng phòng"));

        String providedStatus = request.getStatus();
        String status = "Available";
        if (providedStatus != null && !providedStatus.isBlank()) {
            String matched = ALLOWED_STATUSES.stream()
                    .filter(s -> s.equalsIgnoreCase(providedStatus.trim()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Trạng thái phòng không hợp lệ: " + providedStatus));
            status = matched;
        }

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setFloorNumber(request.getFloorNumber());
        room.setStatus(status);
        room.setAdminPasscode(request.getAdminPasscode() != null && !request.getAdminPasscode().isBlank()
                ? request.getAdminPasscode().trim()
                : null);
        room.setNote(request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote().trim()
                : null);
        room.setAdultCapacity(request.getAdultCapacity() != null ? request.getAdultCapacity() : 2);
        room.setChildCapacity(request.getChildCapacity() != null ? request.getChildCapacity() : 0);
        int calcTotal = (request.getAdultCapacity() != null ? request.getAdultCapacity() : 2)
                + (request.getChildCapacity() != null ? request.getChildCapacity() : 0);
        room.setTotalCapacity(request.getTotalCapacity() != null ? request.getTotalCapacity() : calcTotal);
        room.setArea(request.getArea());
        room.setBedType(request.getBedType());
        room.setBedCount(request.getBedCount() != null ? request.getBedCount() : 1);
        room.setCreatedAt(Instant.now());
        room.setUpdatedAt(Instant.now());

        Room savedRoom = roomRepository.save(room);
        return mapToDetailDTO(savedRoom);
    }

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

        if (request.getRoomNumber() != null && !request.getRoomNumber().isBlank()) {
            String newRoomNumber = request.getRoomNumber().trim();
            if (!newRoomNumber.equalsIgnoreCase(room.getRoomNumber())) {
                if (roomRepository.existsByRoomNumberIgnoreCase(newRoomNumber)) {
                    throw new RuntimeException("Số phòng '" + newRoomNumber + "' đã tồn tại trong hệ thống");
                }
                room.setRoomNumber(newRoomNumber);
            }
        }

        if (request.getFloorNumber() != null) {
            room.setFloorNumber(request.getFloorNumber());
        }

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
            room.setNote(request.getNote().trim());
        }

        if (request.getAdminPasscode() != null) {
            room.setAdminPasscode(request.getAdminPasscode().trim());
        }

        if (request.getRoomTypeId() != null) {
            RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với mã: " + request.getRoomTypeId()));
            room.setRoomType(roomType);
        }

        if (request.getAdultCapacity() != null) {
            room.setAdultCapacity(request.getAdultCapacity());
        }
        if (request.getChildCapacity() != null) {
            room.setChildCapacity(request.getChildCapacity());
        }
        if (request.getTotalCapacity() != null) {
            room.setTotalCapacity(request.getTotalCapacity());
        } else if (request.getAdultCapacity() != null || request.getChildCapacity() != null) {
            int adult = room.getAdultCapacity() != null ? room.getAdultCapacity() : 0;
            int child = room.getChildCapacity() != null ? room.getChildCapacity() : 0;
            room.setTotalCapacity(adult + child);
        }
        if (request.getArea() != null) {
            room.setArea(request.getArea());
        }
        if (request.getBedType() != null) {
            room.setBedType(request.getBedType());
        }
        if (request.getBedCount() != null) {
            room.setBedCount(request.getBedCount());
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
                .adultCapacity(room.getAdultCapacity())
                .childCapacity(room.getChildCapacity())
                .totalCapacity(room.getTotalCapacity())
                .area(room.getArea())
                .bedType(room.getBedType())
                .bedCount(room.getBedCount())
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

    @Override
    @Transactional
    public void deleteRoom(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với mã: " + id));

        if ("Occupied".equalsIgnoreCase(room.getStatus())) {
            throw new RuntimeException("Không thể xóa phòng đang có khách ở (Occupied)");
        }

        boolean hasBookings = bookingDetailRepository.existsByRoom_Id(id);
        if (!hasBookings) {
            roomRepository.delete(room);
            webSocketService.broadcastRoomStatus(id, room.getRoomNumber(), "Inactive");
        } else {
            room.setStatus("Maintenance");
            room.setNote("Đã ngưng vận hành (Xóa khỏi sơ đồ)");
            room.setUpdatedAt(Instant.now());
            Room savedRoom = roomRepository.save(room);
            webSocketService.broadcastRoomStatus(savedRoom.getId(), savedRoom.getRoomNumber(), "Maintenance");
        }
    }
}
