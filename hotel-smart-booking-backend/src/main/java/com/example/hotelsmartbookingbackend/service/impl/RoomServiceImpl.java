package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.service.RoomService;
import com.example.hotelsmartbookingbackend.specification.RoomSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomtypeRepository roomtypeRepository;

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

        if (request.getAdminpasscode() != null) {
            room.setAdminpasscode(request.getAdminpasscode());
        }

        if (request.getRoomtypeid() != null) {
            Roomtype roomtype = roomtypeRepository.findById(request.getRoomtypeid())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với mã: " + request.getRoomtypeid()));
            room.setRoomtypeid(roomtype);
        }

        room.setUpdatedat(Instant.now());
        Room updatedRoom = roomRepository.save(room);

        return mapToDetailDTO(updatedRoom);
    }

    private RoomSummaryDTO mapToSummaryDTO(Room room) {
        return RoomSummaryDTO.builder()
                .id(room.getId())
                .roomnumber(room.getRoomnumber())
                .floornumber(room.getFloornumber())
                .status(room.getStatus())
                .roomtypeid(room.getRoomtypeid().getId())
                .roomtypename(room.getRoomtypeid().getName())
                .createdAt(room.getCreatedat())
                .updatedAt(room.getUpdatedat())
                .build();
    }

    private RoomDetailDTO mapToDetailDTO(Room room) {
        return RoomDetailDTO.builder()
                .id(room.getId())
                .roomnumber(room.getRoomnumber())
                .floornumber(room.getFloornumber())
                .status(room.getStatus())
                .note(room.getNote())
                .adminpasscode(room.getAdminpasscode())
                .roomtypeid(room.getRoomtypeid().getId())
                .roomtypename(room.getRoomtypeid().getName())
                .createdAt(room.getCreatedat())
                .updatedAt(room.getUpdatedat())
                .build();
    }
}

