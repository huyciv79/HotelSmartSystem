package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryResponse;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.RoomTypeImage;
import com.example.hotelsmartbookingbackend.repository.RoomTypeImageRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomTypeImageRepository roomTypeImageRepository;
    @Mock
    private SupabaseStorageService supabaseStorageService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    private RoomType sampleRoomType;
    private RoomTypeImage primaryImage;
    private RoomTypeImage secondaryImage;
    private MultipartFile validImageFile;

    @BeforeEach
    void setUp() {
        sampleRoomType = new RoomType();
        sampleRoomType.setId(1);
        sampleRoomType.setName("Deluxe Ocean View");
        sampleRoomType.setDescription("Luxurious ocean view room");
        sampleRoomType.setBasePrice(new BigDecimal("1500000.00"));
        sampleRoomType.setAdultCapacity(2);
        sampleRoomType.setChildCapacity(1);
        sampleRoomType.setArea(new BigDecimal("35.5"));
        sampleRoomType.setBedType("King Bed");
        sampleRoomType.setAmenities("WiFi, TV, Minibar");
        sampleRoomType.setStatus("Active");
        sampleRoomType.setImages("https://cdn.supabase.co/fallback.jpg");
        sampleRoomType.setCreatedAt(Instant.now());
        sampleRoomType.setUpdatedAt(Instant.now());

        primaryImage = new RoomTypeImage();
        primaryImage.setId(10);
        primaryImage.setRoomType(sampleRoomType);
        primaryImage.setImageUrl("https://cdn.supabase.co/primary.jpg");
        primaryImage.setIsPrimary(true);
        primaryImage.setDisplayOrder(0);

        secondaryImage = new RoomTypeImage();
        secondaryImage.setId(11);
        secondaryImage.setRoomType(sampleRoomType);
        secondaryImage.setImageUrl("https://cdn.supabase.co/secondary.jpg");
        secondaryImage.setIsPrimary(false);
        secondaryImage.setDisplayOrder(1);

        validImageFile = mock(MultipartFile.class);
        lenient().when(validImageFile.isEmpty()).thenReturn(false);
        lenient().when(validImageFile.getSize()).thenReturn(1024L);
        lenient().when(validImageFile.getContentType()).thenReturn("image/png");
        lenient().when(validImageFile.getOriginalFilename()).thenReturn("room.png");
        try {
            lenient().when(validImageFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        } catch (IOException ignored) {}
    }

    // ==========================================
    // 1. getRoomTypeList Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful getRoomTypeList with primary image resolution")
    void should_getRoomTypeListSuccessfully_when_primaryImageExists() {
        RoomTypeFilterCriteria criteria = RoomTypeFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomType> page = new PageImpl<>(List.of(sampleRoomType), pageable, 1);

        when(roomTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(roomTypeImageRepository.findFirstByRoomType_IdAndIsPrimaryTrue(1)).thenReturn(Optional.of(primaryImage));

        PageResponse<RoomTypeSummaryResponse> response = roomTypeService.getRoomTypeList(criteria, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("https://cdn.supabase.co/primary.jpg", response.getContent().get(0).getPrimaryImageUrl());
    }

    @Test
    @DisplayName("UTCID02 - Successful getRoomTypeList falling back to 1st image in gallery when primary image missing")
    void should_getRoomTypeListSuccessfully_when_primaryImageMissingFallbackToFirstGalleryImage() {
        RoomTypeFilterCriteria criteria = RoomTypeFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomType> page = new PageImpl<>(List.of(sampleRoomType), pageable, 1);

        when(roomTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(roomTypeImageRepository.findFirstByRoomType_IdAndIsPrimaryTrue(1)).thenReturn(Optional.empty());
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of(secondaryImage));

        PageResponse<RoomTypeSummaryResponse> response = roomTypeService.getRoomTypeList(criteria, pageable);

        assertNotNull(response);
        assertEquals("https://cdn.supabase.co/secondary.jpg", response.getContent().get(0).getPrimaryImageUrl());
    }

    @Test
    @DisplayName("UTCID03 - Successful getRoomTypeList falling back to roomType.images when gallery is empty")
    void should_getRoomTypeListSuccessfully_when_galleryEmptyFallbackToRoomTypeImages() {
        RoomTypeFilterCriteria criteria = RoomTypeFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomType> page = new PageImpl<>(List.of(sampleRoomType), pageable, 1);

        when(roomTypeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(roomTypeImageRepository.findFirstByRoomType_IdAndIsPrimaryTrue(1)).thenReturn(Optional.empty());
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of());

        PageResponse<RoomTypeSummaryResponse> response = roomTypeService.getRoomTypeList(criteria, pageable);

        assertNotNull(response);
        assertEquals("https://cdn.supabase.co/fallback.jpg", response.getContent().get(0).getPrimaryImageUrl());
    }

    // ==========================================
    // 2. getRoomTypeDetail Test Cases (UTCID04 - UTCID05)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful getRoomTypeDetail when room type ID exists")
    void should_getRoomTypeDetailSuccessfully_when_roomTypeIdExists() {
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of(primaryImage, secondaryImage));

        RoomTypeDetailResponse response = roomTypeService.getRoomTypeDetail(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Deluxe Ocean View", response.getName());
        assertEquals(2, response.getImages().size());
        assertTrue(Boolean.TRUE.equals(response.getImages().get(0).getPrimary()));
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when getRoomTypeDetail with non-existing room type ID")
    void should_throwException_when_getRoomTypeDetailWithRoomTypeNotFound() {
        when(roomTypeRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.getRoomTypeDetail(99));

        assertEquals("Không tìm thấy loại phòng với mã: 99", ex.getMessage());
    }

    // ==========================================
    // 3. createRoomType Test Cases (UTCID06 - UTCID12, UTCID20 - UTCID21)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Successful createRoomType with valid request & image and normalize status case")
    void should_createRoomTypeSuccessfully_when_validRequestAndImageProvided() throws IOException {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Suite Room");
        request.setDescription("Executive Suite");
        request.setBasePrice(new BigDecimal("2000000.00"));
        request.setAdultCapacity(2);
        request.setChildCapacity(2);
        request.setArea(new BigDecimal("45.0"));
        request.setBedType("Super King");
        request.setAmenities("Jacuzzi, Sea View");
        request.setStatus("active"); // lowercase input
        request.setImages(List.of(validImageFile));

        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(i -> {
            RoomType rt = i.getArgument(0);
            if (rt.getId() == null) rt.setId(2);
            return rt;
        });

        when(supabaseStorageService.uploadRoomTypeImage(any(), any(), any())).thenReturn("https://cdn.supabase.co/new.jpg");
        when(roomTypeImageRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        RoomTypeDetailResponse response = roomTypeService.createRoomType(request);

        assertNotNull(response);
        assertEquals("Active", response.getStatus()); // Normalized to title-case
        verify(supabaseStorageService).uploadRoomTypeImage(any(), any(), any());
        verify(entityManager).flush();
        verify(entityManager).refresh(any());
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when createRoomType with empty images list")
    void should_throwException_when_createRoomTypeWithNoImagesProvided() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setImages(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Vui lòng tải lên ít nhất một ảnh loại phòng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when createRoomType with image exceeding 5MB limit")
    void should_throwException_when_createRoomTypeWithImageExceedingSizeLimit() {
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(6 * 1024 * 1024L);

        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setImages(List.of(largeFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Kích thước ảnh loại phòng không được vượt quá 5MB", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when createRoomType with invalid image MIME type")
    void should_throwException_when_createRoomTypeWithInvalidImageContentType() {
        MultipartFile pdfFile = mock(MultipartFile.class);
        when(pdfFile.isEmpty()).thenReturn(false);
        when(pdfFile.getSize()).thenReturn(1024L);
        when(pdfFile.getContentType()).thenReturn("application/pdf");

        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setImages(List.of(pdfFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG, PNG, GIF, WEBP", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when createRoomType with blank name")
    void should_throwException_when_createRoomTypeWithBlankName() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("   ");
        request.setImages(List.of(validImageFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Tên loại phòng không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when createRoomType with invalid status value")
    void should_throwException_when_createRoomTypeWithInvalidStatus() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Valid Name");
        request.setStatus("Pending");
        request.setImages(List.of(validImageFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Trạng thái loại phòng chỉ được là Active hoặc Inactive", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID12 - Trigger Supabase cleanup when DB save fails during createRoomType")
    void should_cleanupUploadedImagesFromSupabase_when_dbSaveFailsAfterUpload() throws IOException {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Suite Room");
        request.setImages(List.of(validImageFile));

        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(i -> {
            RoomType rt = i.getArgument(0);
            if (rt.getId() == null) rt.setId(2);
            return rt;
        });

        when(supabaseStorageService.uploadRoomTypeImage(any(), any(), any())).thenReturn("https://cdn.supabase.co/to-delete.jpg");
        when(roomTypeImageRepository.saveAll(any())).thenThrow(new RuntimeException("Database error on image save"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Database error on image save", ex.getMessage());
        verify(supabaseStorageService).deleteRoomTypeImage("https://cdn.supabase.co/to-delete.jpg");
    }

    @Test
    @DisplayName("UTCID20 - Throw exception when createRoomType with null images list")
    void should_throwException_when_createRoomTypeWithNullImagesList() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setImages(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Vui lòng tải lên ít nhất một ảnh loại phòng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when IOException occurs while reading image bytes")
    void should_throwException_when_fileGetBytesThrowsIOException() throws IOException {
        MultipartFile errorFile = mock(MultipartFile.class);
        when(errorFile.isEmpty()).thenReturn(false);
        when(errorFile.getSize()).thenReturn(1024L);
        when(errorFile.getContentType()).thenReturn("image/png");
        when(errorFile.getBytes()).thenThrow(new IOException("Disk read error"));

        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Suite Room");
        request.setImages(List.of(errorFile));

        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(i -> i.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertTrue(ex.getMessage().startsWith("Không thể đọc file ảnh loại phòng: Disk read error"));
    }

    // ==========================================
    // 4. updateRoomType Test Cases (UTCID13 - UTCID17, UTCID22 - UTCID26)
    // ==========================================

    @Test
    @DisplayName("UTCID13 - Successful updateRoomType appending new image to existing gallery")
    void should_updateRoomTypeSuccessfully_when_appendingNewImageToGallery() throws IOException {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setName("Updated Deluxe");
        request.setReplaceImages(false);
        request.setImages(List.of(validImageFile));

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of(primaryImage));
        when(supabaseStorageService.uploadRoomTypeImage(any(), any(), any())).thenReturn("https://cdn.supabase.co/appended.jpg");

        RoomTypeDetailResponse response = roomTypeService.updateRoomType(1, request);

        assertNotNull(response);
        assertEquals("Updated Deluxe", sampleRoomType.getName());
        verify(roomTypeImageRepository).saveAll(any());
        verify(entityManager).flush();
        verify(entityManager).refresh(any());
    }

    @Test
    @DisplayName("UTCID14 - Successful updateRoomType replacing entire image gallery when replaceImages is true")
    void should_updateRoomTypeSuccessfully_when_replaceImagesIsTrue() throws IOException {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setReplaceImages(true);
        request.setImages(List.of(validImageFile));

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of(primaryImage, secondaryImage));
        when(supabaseStorageService.uploadRoomTypeImage(any(), any(), any())).thenReturn("https://cdn.supabase.co/replacement.jpg");

        RoomTypeDetailResponse response = roomTypeService.updateRoomType(1, request);

        assertNotNull(response);
        verify(roomTypeImageRepository).deleteAll(any());
        verify(roomTypeImageRepository).saveAll(any());
    }

    @Test
    @DisplayName("UTCID15 - Throw exception when updateRoomType with non-existing room type ID")
    void should_throwException_when_updateRoomTypeWithRoomTypeNotFound() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        when(roomTypeRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.updateRoomType(99, request));

        assertEquals("Không tìm thấy loại phòng với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when updateRoomType with blank name")
    void should_throwException_when_updateRoomTypeWithBlankName() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setName("   ");

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.updateRoomType(1, request));

        assertEquals("Tên loại phòng không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when updateRoomType with invalid status value")
    void should_throwException_when_updateRoomTypeWithInvalidStatus() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setStatus("Archived");

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.updateRoomType(1, request));

        assertEquals("Trạng thái loại phòng chỉ được là Active hoặc Inactive", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID22 - Successful updateRoomType preserving existing status when status is null")
    void should_updateRoomTypeSuccessfully_when_statusIsNull() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setStatus(null);

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        RoomTypeDetailResponse response = roomTypeService.updateRoomType(1, request);

        assertNotNull(response);
        assertEquals("Active", sampleRoomType.getStatus());
    }

    @Test
    @DisplayName("UTCID23 - Successful updateRoomType without uploading new images when images list is null")
    void should_updateRoomTypeSuccessfully_when_imagesListIsNull() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setImages(null);

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        RoomTypeDetailResponse response = roomTypeService.updateRoomType(1, request);

        assertNotNull(response);
        verify(supabaseStorageService, never()).uploadRoomTypeImage(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID24 - Clear images field when replaceImages is true and gallery becomes empty")
    void should_clearImagesField_when_replaceImagesIsTrueAndGalleryBecomesEmpty() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setReplaceImages(true);
        request.setImages(null);

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of());

        roomTypeService.updateRoomType(1, request);

        assertNull(sampleRoomType.getImages());
    }

    @Test
    @DisplayName("UTCID25 - Promote first image to primary when no image in gallery has isPrimary true")
    void should_promoteFirstImageToPrimary_when_noImageHasPrimaryTrue() {
        secondaryImage.setIsPrimary(false);

        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeImageRepository.findByRoomType_IdOrderByDisplayOrderAsc(1)).thenReturn(List.of(secondaryImage));
        when(roomTypeImageRepository.save(secondaryImage)).thenAnswer(i -> i.getArgument(0));

        roomTypeService.updateRoomType(1, request);

        assertTrue(secondaryImage.getIsPrimary());
        assertEquals("https://cdn.supabase.co/secondary.jpg", sampleRoomType.getImages());
    }

    @Test
    @DisplayName("UTCID26 - Successful updateRoomType normalizing uppercase status INACTIVE to Inactive")
    void should_updateRoomTypeSuccessfully_when_uppercaseStatusProvided() {
        UpdateRoomTypeRequest request = new UpdateRoomTypeRequest();
        request.setStatus("INACTIVE");

        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        RoomTypeDetailResponse response = roomTypeService.updateRoomType(1, request);

        assertNotNull(response);
        assertEquals("Inactive", sampleRoomType.getStatus());
    }

    // ==========================================
    // 5. deleteRoomType Test Cases (UTCID18 - UTCID19)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful deleteRoomType performing soft-delete by setting status to Inactive")
    void should_deleteRoomTypeSuccessfully_when_roomTypeIdExists() {
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(sampleRoomType));

        roomTypeService.deleteRoomType(1);

        assertEquals("Inactive", sampleRoomType.getStatus());
        verify(roomTypeRepository).save(sampleRoomType);
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when deleteRoomType with non-existing room type ID")
    void should_throwException_when_deleteRoomTypeWithRoomTypeNotFound() {
        when(roomTypeRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.deleteRoomType(99));

        assertEquals("Không tìm thấy loại phòng với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("Throw exception when image file size exceeds 5MB limit")
    void should_throwException_when_imageFileSizeExceedsLimit() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "images", "large.png", "image/png", new byte[6 * 1024 * 1024]);

        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Large Image Room");
        request.setImages(List.of(largeFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Kích thước ảnh loại phòng không được vượt quá 5MB", ex.getMessage());
    }

    @Test
    @DisplayName("Throw exception when image file content type is invalid")
    void should_throwException_when_imageFileContentTypeIsInvalid() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "images", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("PDF Image Room");
        request.setImages(List.of(pdfFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, JPEG, PNG, GIF, WEBP", ex.getMessage());
    }

    @Test
    @DisplayName("Throw exception when room type status is invalid during create")
    void should_throwException_when_statusIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("images", "pic.jpg", "image/jpeg", new byte[]{1, 2});
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Invalid Status Room");
        request.setStatus("UnknownStatus");
        request.setImages(List.of(file));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomTypeService.createRoomType(request));

        assertEquals("Trạng thái loại phòng chỉ được là Active hoặc Inactive", ex.getMessage());
    }
}

