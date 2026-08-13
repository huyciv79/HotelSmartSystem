package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeImageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryResponse;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.RoomTypeImage;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeImageRepository;
import com.example.hotelsmartbookingbackend.service.RoomTypeService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.example.hotelsmartbookingbackend.specification.RoomTypeSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private static final String ACTIVE_STATUS = "Active";
    private static final String INACTIVE_STATUS = "Inactive";

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomTypeSummaryResponse> getRoomTypeList(
            RoomTypeFilterCriteria criteria,
            Pageable pageable) {

        Specification<RoomType> spec = RoomTypeSpecification.withFilters(criteria);
        Page<RoomType> page = roomTypeRepository.findAll(spec, pageable);

        List<RoomTypeSummaryResponse> content = page.getContent().stream()
                .map(this::mapToSummaryResponse)
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
        RoomType roomType = findRoomTypeById(id);
        return mapToDetailResponse(roomType);
    }

    @Override
    @Transactional
    public RoomTypeDetailResponse createRoomType(CreateRoomTypeRequest request) {
        List<MultipartFile> imageFiles = getNonEmptyImages(request.getImages());

        if (imageFiles.isEmpty()) {
            throw new RuntimeException("Vui lòng tải lên ít nhất một ảnh loại phòng");
        }

        imageFiles.forEach(this::validateImageFile);

        List<String> uploadedImageUrls = new ArrayList<>();

        try {
            Instant now = Instant.now();

            RoomType roomType = new RoomType();
            roomType.setName(requireText(request.getName(), "Tên loại phòng không được để trống"));
            roomType.setDescription(request.getDescription());
            roomType.setBasePrice(request.getBasePrice());
            roomType.setAmenities(request.getAmenities());
            roomType.setStatus(normalizeStatus(request.getStatus(), ACTIVE_STATUS));
            roomType.setCreatedAt(now);
            roomType.setUpdatedAt(now);

            RoomType savedRoomType = roomTypeRepository.save(roomType);

            List<RoomTypeImage> images = uploadRoomTypeImages(
                    savedRoomType,
                    imageFiles,
                    0,
                    true,
                    uploadedImageUrls
            );

            roomTypeImageRepository.saveAll(images);

            savedRoomType.setImages(images.get(0).getImageUrl());
            savedRoomType.setUpdatedAt(Instant.now());

            roomTypeRepository.save(savedRoomType);

            entityManager.flush();
            entityManager.refresh(savedRoomType);

            return mapToDetailResponse(savedRoomType);
        } catch (RuntimeException e) {
            cleanupUploadedRoomTypeImages(uploadedImageUrls);
            throw e;
        }
    }

    @Override
    @Transactional
    public RoomTypeDetailResponse updateRoomType(Integer id, UpdateRoomTypeRequest request) {
        RoomType roomType = findRoomTypeById(id);

        if (request.getName() != null) {
            roomType.setName(requireText(request.getName(), "Tên loại phòng không được để trống"));
        }

        if (request.getDescription() != null) {
            roomType.setDescription(request.getDescription());
        }

        if (request.getBasePrice() != null) {
            roomType.setBasePrice(request.getBasePrice());
        }

        if (request.getAmenities() != null) {
            roomType.setAmenities(request.getAmenities());
        }

        if (request.getStatus() != null) {
            roomType.setStatus(normalizeStatus(request.getStatus(), roomType.getStatus()));
        }

        boolean replaceImages = Boolean.TRUE.equals(request.getReplaceImages());
        List<MultipartFile> imageFiles = getNonEmptyImages(request.getImages());

        imageFiles.forEach(this::validateImageFile);

        List<String> uploadedImageUrls = new ArrayList<>();

        try {
            if (replaceImages) {
                List<RoomTypeImage> currentImages =
                        roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(id);

                roomTypeImageRepository.deleteAll(currentImages);
                entityManager.flush();
            }

            if (!imageFiles.isEmpty()) {
                List<RoomTypeImage> currentImages = replaceImages
                        ? List.of()
                        : roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(id);

                int startDisplayOrder = replaceImages ? 0 : nextDisplayOrder(currentImages);

                boolean shouldSetFirstNewImagePrimary = replaceImages ||
                        currentImages.stream()
                                .noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()));

                List<RoomTypeImage> uploadedImages = uploadRoomTypeImages(
                        roomType,
                        imageFiles,
                        startDisplayOrder,
                        shouldSetFirstNewImagePrimary,
                        uploadedImageUrls
                );

                roomTypeImageRepository.saveAll(uploadedImages);
            }

            syncPrimaryImage(roomType);

            roomType.setUpdatedAt(Instant.now());
        } catch (RuntimeException e) {
            cleanupUploadedRoomTypeImages(uploadedImageUrls);
            throw e;
        }

        roomTypeRepository.save(roomType);

        entityManager.flush();
        entityManager.refresh(roomType);

        return mapToDetailResponse(roomType);
    }

    @Override
    @Transactional
    public void deleteRoomType(Integer id) {
        RoomType roomType = findRoomTypeById(id);
        roomType.setStatus(INACTIVE_STATUS);
        roomType.setUpdatedAt(Instant.now());

        roomTypeRepository.save(roomType);
    }

    private RoomType findRoomTypeById(Integer id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng với mã: " + id));
    }

    private RoomTypeSummaryResponse mapToSummaryResponse(RoomType roomType) {
        return RoomTypeSummaryResponse.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .basePrice(roomType.getBasePrice())
                .adultCapacity(getRepresentativeAdultCapacity(roomType))
                .childCapacity(getRepresentativeChildCapacity(roomType))
                .totalCapacity(getRepresentativeTotalCapacity(roomType))
                .area(getRepresentativeArea(roomType))
                .bedType(getRepresentativeBedType(roomType))
                .primaryImageUrl(resolvePrimaryImageUrl(roomType))
                .status(roomType.getStatus())
                .build();
    }

    private RoomTypeDetailResponse mapToDetailResponse(RoomType roomType) {
        List<RoomTypeImage> images =
                roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(roomType.getId());

        return RoomTypeDetailResponse.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .basePrice(roomType.getBasePrice())
                .adultCapacity(getRepresentativeAdultCapacity(roomType))
                .childCapacity(getRepresentativeChildCapacity(roomType))
                .totalCapacity(getRepresentativeTotalCapacity(roomType))
                .area(getRepresentativeArea(roomType))
                .bedType(getRepresentativeBedType(roomType))
                .amenities(roomType.getAmenities())
                .status(roomType.getStatus())
                .images(images.stream().map(this::mapToImageResponse).toList())
                .createdAt(roomType.getCreatedAt())
                .updatedAt(roomType.getUpdatedAt())
                .build();
    }

    private Integer getRepresentativeAdultCapacity(RoomType roomType) {
        if (roomType.getRooms() == null || roomType.getRooms().isEmpty()) return null;
        return roomType.getRooms().stream()
                .map(com.example.hotelsmartbookingbackend.entity.Room::getAdultCapacity)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private Integer getRepresentativeChildCapacity(RoomType roomType) {
        if (roomType.getRooms() == null || roomType.getRooms().isEmpty()) return null;
        return roomType.getRooms().stream()
                .map(com.example.hotelsmartbookingbackend.entity.Room::getChildCapacity)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private Integer getRepresentativeTotalCapacity(RoomType roomType) {
        if (roomType.getRooms() == null || roomType.getRooms().isEmpty()) return null;
        return roomType.getRooms().stream()
                .map(com.example.hotelsmartbookingbackend.entity.Room::getTotalCapacity)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private java.math.BigDecimal getRepresentativeArea(RoomType roomType) {
        if (roomType.getRooms() == null || roomType.getRooms().isEmpty()) return null;
        return roomType.getRooms().stream()
                .map(com.example.hotelsmartbookingbackend.entity.Room::getArea)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private String getRepresentativeBedType(RoomType roomType) {
        if (roomType.getRooms() == null || roomType.getRooms().isEmpty()) return null;
        return roomType.getRooms().stream()
                .map(com.example.hotelsmartbookingbackend.entity.Room::getBedType)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private RoomTypeImageResponse mapToImageResponse(RoomTypeImage image) {
        return RoomTypeImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .primary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private String resolvePrimaryImageUrl(RoomType roomType) {
        return roomTypeImageRepository.findFirstByRoomType_IdAndIsPrimaryTrue(roomType.getId())
                .map(RoomTypeImage::getImageUrl)
                .or(() -> roomTypeImageRepository
                        .findByRoomType_IdOrderByDisplayOrderAsc(roomType.getId())
                        .stream()
                        .findFirst()
                        .map(RoomTypeImage::getImageUrl))
                .orElse(roomType.getImages());
    }

    private List<MultipartFile> getNonEmptyImages(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }

        return images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .toList();
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Ảnh loại phòng không được để trống");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Kích thước ảnh loại phòng không được vượt quá 5MB");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG, PNG, GIF, WEBP");
        }
    }

    private List<RoomTypeImage> uploadRoomTypeImages(
            RoomType roomType,
            List<MultipartFile> imageFiles,
            int startDisplayOrder,
            boolean firstImagePrimary,
            List<String> uploadedImageUrls) {

        List<RoomTypeImage> uploadedImages = new ArrayList<>();

        for (int index = 0; index < imageFiles.size(); index++) {
            MultipartFile file = imageFiles.get(index);

            try {
                String imageUrl = supabaseStorageService.uploadRoomTypeImage(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType()
                );

                uploadedImageUrls.add(imageUrl);

                RoomTypeImage image = new RoomTypeImage();
                image.setRoomType(roomType);
                image.setImageUrl(imageUrl);
                image.setIsPrimary(firstImagePrimary && index == 0);
                image.setDisplayOrder(startDisplayOrder + index);
                image.setCreatedAt(Instant.now());

                uploadedImages.add(image);
            } catch (IOException e) {
                throw new RuntimeException("Không thể đọc file ảnh loại phòng: " + e.getMessage(), e);
            }
        }

        return uploadedImages;
    }

    private void syncPrimaryImage(RoomType roomType) {
        List<RoomTypeImage> images =
                roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(roomType.getId());

        if (images.isEmpty()) {
            roomType.setImages(null);
            return;
        }

        RoomTypeImage primaryImage = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .findFirst()
                .orElseGet(() -> {
                    RoomTypeImage firstImage = images.get(0);
                    firstImage.setIsPrimary(true);
                    return roomTypeImageRepository.save(firstImage);
                });

        roomType.setImages(primaryImage.getImageUrl());
    }

    private int nextDisplayOrder(List<RoomTypeImage> images) {
        return images.stream()
                .map(RoomTypeImage::getDisplayOrder)
                .filter(displayOrder -> displayOrder != null)
                .max(Integer::compareTo)
                .map(displayOrder -> displayOrder + 1)
                .orElse(0);
    }

    private String normalizeStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }

        String normalized = status.trim();

        if (ACTIVE_STATUS.equalsIgnoreCase(normalized)) {
            return ACTIVE_STATUS;
        }

        if (INACTIVE_STATUS.equalsIgnoreCase(normalized)) {
            return INACTIVE_STATUS;
        }

        throw new RuntimeException("Trạng thái loại phòng chỉ được là Active hoặc Inactive");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }

    private void cleanupUploadedRoomTypeImages(List<String> uploadedImageUrls) {
        if (uploadedImageUrls == null || uploadedImageUrls.isEmpty()) {
            return;
        }

        uploadedImageUrls.forEach(supabaseStorageService::deleteRoomTypeImage);
    }
}