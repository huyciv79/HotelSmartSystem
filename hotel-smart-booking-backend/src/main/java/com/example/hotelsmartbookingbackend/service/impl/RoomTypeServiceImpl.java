package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeImageDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryDTO;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.Roomtypeimage;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeimageRepository;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private static final String ACTIVE_STATUS = "Active";
    private static final String INACTIVE_STATUS = "Inactive";
    private static final String AVAILABLE_ROOM_STATUS = "Available";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    private final RoomtypeRepository roomtypeRepository;
    private final RoomtypeimageRepository roomtypeimageRepository;
    private final RoomRepository roomRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomTypeSummaryDTO> getRoomTypeList(RoomTypeFilterCriteria criteria, Pageable pageable) {
        Specification<Roomtype> spec = RoomTypeSpecification.withFilters(criteria);
        Page<Roomtype> page = roomtypeRepository.findAll(spec, pageable);

        List<RoomTypeSummaryDTO> content = page.getContent().stream()
                .map(this::mapToSummaryDTO)
                .toList();

        return PageResponse.<RoomTypeSummaryDTO>builder()
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
    public RoomTypeDetailDTO getRoomTypeDetail(Integer id) {
        Roomtype roomtype = roomtypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay loai phong voi ma: " + id));

        return mapToDetailDTO(roomtype);
    }

    @Override
    @Transactional
    public RoomTypeDetailDTO createRoomType(CreateRoomTypeRequest request) {
        List<MultipartFile> imageFiles = getNonEmptyImages(request.getImages());
        if (imageFiles.isEmpty()) {
            throw new RuntimeException("Vui long tai len it nhat mot anh loai phong");
        }
        imageFiles.forEach(this::validateImageFile);

        List<String> uploadedImageUrls = new ArrayList<>();

        try {
            Instant now = Instant.now();

            Roomtype roomtype = new Roomtype();
            roomtype.setName(requireText(request.getName(), "Ten loai phong khong duoc de trong"));
            roomtype.setDescription(request.getDescription());
            roomtype.setBaseprice(request.getBasePrice());
            roomtype.setAdultcapacity(request.getAdultCapacity());
            roomtype.setChildcapacity(request.getChildCapacity());
            roomtype.setArea(request.getArea());
            roomtype.setBedtype(request.getBedType());
            roomtype.setAmenities(request.getAmenities());
            roomtype.setStatus(normalizeStatus(request.getStatus(), ACTIVE_STATUS));
            roomtype.setCreatedat(now);
            roomtype.setUpdatedat(now);

            Roomtype savedRoomtype = roomtypeRepository.save(roomtype);
            List<Roomtypeimage> images = uploadRoomTypeImages(
                    savedRoomtype,
                    imageFiles,
                    0,
                    true,
                    uploadedImageUrls);
            roomtypeimageRepository.saveAll(images);

            savedRoomtype.setImages(images.get(0).getImageurl());
            savedRoomtype.setUpdatedat(Instant.now());
            roomtypeRepository.save(savedRoomtype);
            entityManager.flush();
            entityManager.refresh(savedRoomtype);

            return mapToDetailDTO(savedRoomtype);
        } catch (RuntimeException e) {
            cleanupUploadedRoomTypeImages(uploadedImageUrls);
            throw e;
        }
    }

    @Override
    @Transactional
    public RoomTypeDetailDTO updateRoomType(Integer id, UpdateRoomTypeRequest request) {
        Roomtype roomtype = roomtypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay loai phong voi ma: " + id));

        if (request.getName() != null) {
            roomtype.setName(requireText(request.getName(), "Ten loai phong khong duoc de trong"));
        }
        if (request.getDescription() != null) {
            roomtype.setDescription(request.getDescription());
        }
        if (request.getBasePrice() != null) {
            roomtype.setBaseprice(request.getBasePrice());
        }
        if (request.getAdultCapacity() != null) {
            roomtype.setAdultcapacity(request.getAdultCapacity());
        }
        if (request.getChildCapacity() != null) {
            roomtype.setChildcapacity(request.getChildCapacity());
        }
        if (request.getArea() != null) {
            roomtype.setArea(request.getArea());
        }
        if (request.getBedType() != null) {
            roomtype.setBedtype(request.getBedType());
        }
        if (request.getAmenities() != null) {
            roomtype.setAmenities(request.getAmenities());
        }
        if (request.getStatus() != null) {
            roomtype.setStatus(normalizeStatus(request.getStatus(), roomtype.getStatus()));
        }

        boolean replaceImages = Boolean.TRUE.equals(request.getReplaceImages());
        List<MultipartFile> imageFiles = getNonEmptyImages(request.getImages());
        imageFiles.forEach(this::validateImageFile);

        List<String> uploadedImageUrls = new ArrayList<>();

        try {
            if (replaceImages) {
                List<Roomtypeimage> currentImages =
                        roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(id);
                roomtypeimageRepository.deleteAll(currentImages);
            }

            if (!imageFiles.isEmpty()) {
                List<Roomtypeimage> currentImages = replaceImages
                        ? List.of()
                        : roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(id);
                int startDisplayOrder = replaceImages ? 0 : nextDisplayOrder(currentImages);
                boolean shouldSetFirstNewImagePrimary = replaceImages || currentImages.stream()
                        .noneMatch(image -> Boolean.TRUE.equals(image.getIsprimary()));

                List<Roomtypeimage> uploadedImages = uploadRoomTypeImages(
                        roomtype,
                        imageFiles,
                        startDisplayOrder,
                        shouldSetFirstNewImagePrimary,
                        uploadedImageUrls);
                roomtypeimageRepository.saveAll(uploadedImages);
            }

            syncPrimaryImage(roomtype);
            roomtype.setUpdatedat(Instant.now());
            roomtypeRepository.save(roomtype);
            entityManager.flush();
            entityManager.refresh(roomtype);

            return mapToDetailDTO(roomtype);
        } catch (RuntimeException e) {
            cleanupUploadedRoomTypeImages(uploadedImageUrls);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteRoomType(Integer id) {
        Roomtype roomtype = roomtypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay loai phong voi ma: " + id));

        roomtype.setStatus(INACTIVE_STATUS);
        roomtype.setUpdatedat(Instant.now());
        roomtypeRepository.save(roomtype);
    }

    private RoomTypeSummaryDTO mapToSummaryDTO(Roomtype roomtype) {
        return RoomTypeSummaryDTO.builder()
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

    private RoomTypeDetailDTO mapToDetailDTO(Roomtype roomtype) {
        List<Roomtypeimage> images = roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(roomtype.getId());

        return RoomTypeDetailDTO.builder()
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

    private String resolvePrimaryImageUrl(Roomtype roomtype) {
        return roomtypeimageRepository.findFirstByRoomtypeid_IdAndIsprimaryTrue(roomtype.getId())
                .map(Roomtypeimage::getImageurl)
                .or(() -> roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(roomtype.getId()).stream()
                        .findFirst()
                        .map(Roomtypeimage::getImageurl))
                .orElse(roomtype.getImages());
    }

    private RoomTypeImageDTO mapToImageDTO(Roomtypeimage image) {
        return RoomTypeImageDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageurl())
                .primary(image.getIsprimary())
                .displayOrder(image.getDisplayorder())
                .build();
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
            throw new RuntimeException("Anh loai phong khong duoc de trong");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Kich thuoc anh loai phong khong duoc vuot qua 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Dinh dang anh khong hop le. Chi chap nhan JPG, JPEG, PNG, GIF, WEBP");
        }
    }

    private List<Roomtypeimage> uploadRoomTypeImages(
            Roomtype roomtype,
            List<MultipartFile> imageFiles,
            int startDisplayOrder,
            boolean firstImagePrimary,
            List<String> uploadedImageUrls) {
        List<Roomtypeimage> uploadedImages = new ArrayList<>();

        for (int index = 0; index < imageFiles.size(); index++) {
            MultipartFile file = imageFiles.get(index);
            try {
                String imageUrl = supabaseStorageService.uploadRoomTypeImage(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType());
                uploadedImageUrls.add(imageUrl);

                Roomtypeimage image = new Roomtypeimage();
                image.setRoomtypeid(roomtype);
                image.setImageurl(imageUrl);
                image.setIsprimary(firstImagePrimary && index == 0);
                image.setDisplayorder(startDisplayOrder + index);
                image.setCreatedat(Instant.now());
                uploadedImages.add(image);
            } catch (IOException e) {
                throw new RuntimeException("Khong the doc file anh loai phong: " + e.getMessage(), e);
            }
        }

        return uploadedImages;
    }

    private void cleanupUploadedRoomTypeImages(List<String> uploadedImageUrls) {
        if (uploadedImageUrls.isEmpty()) {
            return;
        }

        uploadedImageUrls.forEach(supabaseStorageService::deleteRoomTypeImage);
    }

    private void syncPrimaryImage(Roomtype roomtype) {
        List<Roomtypeimage> images = roomtypeimageRepository.findByRoomtypeid_IdOrderByDisplayorderAsc(roomtype.getId());
        if (images.isEmpty()) {
            roomtype.setImages(null);
            return;
        }

        Roomtypeimage primaryImage = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsprimary()))
                .findFirst()
                .orElseGet(() -> {
                    Roomtypeimage firstImage = images.get(0);
                    firstImage.setIsprimary(true);
                    return roomtypeimageRepository.save(firstImage);
                });

        roomtype.setImages(primaryImage.getImageurl());
    }

    private int nextDisplayOrder(List<Roomtypeimage> images) {
        return images.stream()
                .map(Roomtypeimage::getDisplayorder)
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

        throw new RuntimeException("Trang thai loai phong chi duoc la Active hoac Inactive");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private long countAvailableRooms(Integer roomTypeId) {
        return roomRepository.countByRoomtypeid_IdAndStatus(roomTypeId, AVAILABLE_ROOM_STATUS);
    }
}
