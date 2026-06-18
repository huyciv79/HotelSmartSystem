package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeImageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryResponse;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.Roomtypeimage;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeimageRepository;
import com.example.hotelsmartbookingbackend.service.RoomTypeService;
import com.example.hotelsmartbookingbackend.specification.RoomTypeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private static final String AVAILABLE_ROOM_STATUS = "Available";

    private final RoomtypeRepository roomtypeRepository;
    private final RoomtypeimageRepository roomtypeimageRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomTypeSummaryResponse> getRoomTypeList(RoomTypeFilterCriteria criteria, Pageable pageable) {
        Specification<Roomtype> spec = RoomTypeSpecification.withFilters(criteria);
        Page<Roomtype> page = roomtypeRepository.findAll(spec, pageable);

        List<RoomTypeSummaryResponse> content = page.getContent().stream()
                .map(this::mapToSummaryDTO)
                .toList();

        return PageResponse.<RoomTypeSummaryResponse>builder()
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
    public RoomTypeDetailResponse getRoomTypeDetail(Integer id) {
        Roomtype roomtype = roomtypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với mã: " + id));

        List<Roomtypeimage> images = roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(id);

        return RoomTypeDetailResponse.builder()
                .id(roomtype.getId())
                .name(roomtype.getName())
                .description(roomtype.getDescription())
                .basePrice(roomtype.getBaseprice())
                .adultCapacity(roomtype.getAdultcapacity())
                .childCapacity(roomtype.getChildcapacity())
                .totalCapacity(roomtype.getTotalcapacity())
                .area(roomtype.getArea())
                .bedType(roomtype.getBedtype())
                .amenities(roomtype.getAmenities())
                .status(roomtype.getStatus())
                .images(images.stream().map(this::mapToImageDTO).toList())
                .createdAt(roomtype.getCreatedat())
                .updatedAt(roomtype.getUpdatedat())
                .build();
    }

    private RoomTypeSummaryResponse mapToSummaryDTO(Roomtype roomtype) {
        return RoomTypeSummaryResponse.builder()
                .id(roomtype.getId())
                .name(roomtype.getName())
                .description(roomtype.getDescription())
                .basePrice(roomtype.getBaseprice())
                .adultCapacity(roomtype.getAdultcapacity())
                .childCapacity(roomtype.getChildcapacity())
                .totalCapacity(roomtype.getTotalcapacity())
                .area(roomtype.getArea())
                .bedType(roomtype.getBedtype())
                .primaryImageUrl(resolvePrimaryImageUrl(roomtype))
                .status(roomtype.getStatus())
                .build();
    }

    private String resolvePrimaryImageUrl(Roomtype roomtype) {
        return roomtypeimageRepository.findFirstByRoomtypeid_IdAndIsprimaryTrue(roomtype.getId())
                .map(Roomtypeimage::getImageurl)
                .or(() -> roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(roomtype.getId()).stream()
                        .findFirst()
                        .map(Roomtypeimage::getImageurl))
                .orElse(roomtype.getImages());
    }

    private RoomTypeImageResponse mapToImageDTO(Roomtypeimage image) {
        return RoomTypeImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageurl())
                .primary(image.getIsprimary())
                .displayOrder(image.getDisplayorder())
                .build();
    }

    private long countAvailableRooms(Integer roomTypeId) {
        return roomRepository.countByRoomtypeid_IdAndStatus(roomTypeId, AVAILABLE_ROOM_STATUS);
    }
}
