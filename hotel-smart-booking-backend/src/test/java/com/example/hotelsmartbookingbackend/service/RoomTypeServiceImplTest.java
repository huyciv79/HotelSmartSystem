package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.repository.RoomTypeImageRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.service.impl.RoomTypeServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @DisplayName("Should throw exception when room type ID is not found")
    void testGetRoomTypeDetail_NotFound() {
        when(roomTypeRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roomTypeService.getRoomTypeDetail(999)
        );

        assertTrue(exception.getMessage().contains("Không tìm thấy loại phòng"));
    }

    @Test
    @DisplayName("Should throw exception when creating room type without images")
    void testCreateRoomType_MissingImages() {
        CreateRoomTypeRequest request = new CreateRoomTypeRequest();
        request.setName("Deluxe Suite");
        request.setImages(Collections.emptyList());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roomTypeService.createRoomType(request)
        );

        assertTrue(exception.getMessage().contains("Vui lòng tải lên ít nhất một ảnh"));
    }
}
