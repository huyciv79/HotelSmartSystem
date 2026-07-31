package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomStatusResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;
    private RoomType sampleRoomType;
    private RoomType newRoomType;

    @BeforeEach
    void setUp() {
        sampleRoomType = new RoomType();
        sampleRoomType.setId(1);
        sampleRoomType.setName("Standard Room");

        newRoomType = new RoomType();
        newRoomType.setId(2);
        newRoomType.setName("Deluxe Suite");

        sampleRoom = new Room();
        sampleRoom.setId(1);
        sampleRoom.setRoomNumber("101");
        sampleRoom.setFloorNumber(1);
        sampleRoom.setStatus("Available");
        sampleRoom.setNote("Clean room");
        sampleRoom.setAdminPasscode("123456");
        sampleRoom.setRoomType(sampleRoomType);
        sampleRoom.setCreatedAt(Instant.now());
        sampleRoom.setUpdatedAt(Instant.now());
    }

    // ==========================================
    // 1. getRoomList Test Cases (UTCID01 - UTCID02)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful getRoomList returning paginated room summary list")
    void should_getRoomListSuccessfully_when_criteriaMatchesRooms() {
        RoomFilterCriteria criteria = RoomFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(sampleRoom), pageable, 1);

        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<RoomSummaryDTO> response = roomService.getRoomList(criteria, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("101", response.getContent().get(0).getRoomNumber());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("UTCID02 - Successful getRoomList returning empty PageResponse when criteria matches no rooms")
    void should_getEmptyRoomListSuccessfully_when_criteriaMatchesNoRooms() {
        RoomFilterCriteria criteria = RoomFilterCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PageResponse<RoomSummaryDTO> response = roomService.getRoomList(criteria, pageable);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    // ==========================================
    // 2. getRoomDetail Test Cases (UTCID03 - UTCID04)
    // ==========================================

    @Test
    @DisplayName("UTCID03 - Successful getRoomDetail when room ID exists")
    void should_getRoomDetailSuccessfully_when_roomIdExists() {
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        RoomDetailDTO dto = roomService.getRoomDetail(1);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("101", dto.getRoomNumber());
        assertEquals("Available", dto.getStatus());
        assertEquals("Standard Room", dto.getRoomTypeName());
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when getRoomDetail with non-existing room ID")
    void should_throwException_when_getRoomDetailWithRoomNotFound() {
        when(roomRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomService.getRoomDetail(99));

        assertEquals("Không tìm thấy phòng với mã: 99", ex.getMessage());
    }

    // ==========================================
    // 3. updateRoom Test Cases (UTCID05 - UTCID11, UTCID20)
    // ==========================================

    @Test
    @DisplayName("UTCID05 - Successful updateRoom with all valid fields and status case normalization")
    void should_updateRoomSuccessfully_when_allValidFieldsProvidedAndNormalizeStatusCase() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("cleaning"); // lowercase input
        request.setNote("Deep cleaning");
        request.setAdminPasscode("654321");
        request.setRoomTypeId(2);

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomTypeRepository.findById(2)).thenReturn(Optional.of(newRoomType));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomDetailDTO dto = roomService.updateRoom(1, request);

        assertNotNull(dto);
        assertEquals("Cleaning", dto.getStatus()); // Normalized to title-case
        assertEquals("Deep cleaning", dto.getNote());
        assertEquals("654321", dto.getAdminPasscode());
        assertEquals("Deluxe Suite", dto.getRoomTypeName());
        verify(webSocketService).broadcastRoomStatus(1, "101", "Cleaning");
    }

    @Test
    @DisplayName("UTCID06 - Successful updateRoom skipping status update when status is null or blank")
    void should_updateRoomSuccessfully_when_statusIsBlankOrNull() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("   "); // Blank status
        request.setNote("Updated note only");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomDetailDTO dto = roomService.updateRoom(1, request);

        assertNotNull(dto);
        assertEquals("Available", dto.getStatus()); // Original status preserved
        assertEquals("Updated note only", dto.getNote());
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when updateRoom with non-existing room ID")
    void should_throwException_when_updateRoomWithRoomNotFound() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        when(roomRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomService.updateRoom(99, request));

        assertEquals("Không tìm thấy phòng với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when updateRoom with invalid status string")
    void should_throwException_when_updateRoomWithInvalidStatus() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("Broken");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> roomService.updateRoom(1, request));

        assertTrue(ex.getMessage().startsWith("Trạng thái không hợp lệ. Giá trị hợp lệ:"));
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when updateRoom with non-existing roomTypeId")
    void should_throwException_when_updateRoomWithRoomTypeNotFound() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setRoomTypeId(99);

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomTypeRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomService.updateRoom(1, request));

        assertEquals("Không tìm thấy loại phòng với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID10 - Successful updateRoom updating note field only")
    void should_updateRoomNoteOnly_when_onlyNoteProvided() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setNote("Special note");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomDetailDTO dto = roomService.updateRoom(1, request);

        assertNotNull(dto);
        assertEquals("Special note", dto.getNote());
        assertEquals("123456", dto.getAdminPasscode());
    }

    @Test
    @DisplayName("UTCID11 - Successful updateRoom updating adminPasscode field only")
    void should_updateRoomAdminPasscodeOnly_when_onlyAdminPasscodeProvided() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setAdminPasscode("999999");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomDetailDTO dto = roomService.updateRoom(1, request);

        assertNotNull(dto);
        assertEquals("999999", dto.getAdminPasscode());
    }

    @Test
    @DisplayName("UTCID20 - Successful updateRoom normalizing uppercase status RESERVED to Reserved")
    void should_updateRoomSuccessfully_when_uppercaseStatusProvided() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("RESERVED");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomDetailDTO dto = roomService.updateRoom(1, request);

        assertNotNull(dto);
        assertEquals("Reserved", dto.getStatus());
        verify(webSocketService).broadcastRoomStatus(1, "101", "Reserved");
    }

    // ==========================================
    // 4. getAllRoomStatuses Test Cases (UTCID12)
    // ==========================================

    @Test
    @DisplayName("UTCID12 - Successful getAllRoomStatuses returning list of room status responses")
    void should_getAllRoomStatusesSuccessfully_when_roomsExist() {
        when(roomRepository.findAll()).thenReturn(List.of(sampleRoom));

        List<RoomStatusResponse> responses = roomService.getAllRoomStatuses();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1, responses.get(0).getRoomId());
        assertEquals("101", responses.get(0).getRoomNumber());
        assertEquals("Available", responses.get(0).getStatus());
    }

    // ==========================================
    // 5. getRoomStatus Test Cases (UTCID13 - UTCID14)
    // ==========================================

    @Test
    @DisplayName("UTCID13 - Successful getRoomStatus when room ID exists")
    void should_getRoomStatusSuccessfully_when_roomIdExists() {
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        RoomStatusResponse response = roomService.getRoomStatus(1);

        assertNotNull(response);
        assertEquals(1, response.getRoomId());
        assertEquals("101", response.getRoomNumber());
        assertEquals("Available", response.getStatus());
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when getRoomStatus with non-existing room ID")
    void should_throwException_when_getRoomStatusWithRoomNotFound() {
        when(roomRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomService.getRoomStatus(99));

        assertEquals("Không tìm thấy phòng với mã: 99", ex.getMessage());
    }

    // ==========================================
    // 6. updateRoomStatus Test Cases (UTCID15 - UTCID19, UTCID21)
    // ==========================================

    @Test
    @DisplayName("UTCID15 - Successful updateRoomStatus with valid lowercase status")
    void should_updateRoomStatusSuccessfully_when_validStatusProvided() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("maintenance"); // lowercase input

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomStatusResponse response = roomService.updateRoomStatus(1, request);

        assertNotNull(response);
        assertEquals("Maintenance", response.getStatus()); // Normalized to title-case
        verify(roomRepository).save(sampleRoom);
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when updateRoomStatus with non-existing room ID")
    void should_throwException_when_updateRoomStatusWithRoomNotFound() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("Available");

        when(roomRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomService.updateRoomStatus(99, request));

        assertEquals("Không tìm thấy phòng với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when updateRoomStatus with blank status")
    void should_throwException_when_updateRoomStatusWithNullOrBlankStatus() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("  ");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> roomService.updateRoomStatus(1, request));

        assertEquals("Trạng thái phòng không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID18 - Throw exception when updateRoomStatus with invalid status value")
    void should_throwException_when_updateRoomStatusWithInvalidStatus() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("UnknownStatus");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> roomService.updateRoomStatus(1, request));

        assertEquals("Trạng thái không hợp lệ", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when updateRoomStatus with null status")
    void should_throwException_when_updateRoomStatusWithNullStatus() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus(null);

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> roomService.updateRoomStatus(1, request));

        assertEquals("Trạng thái phòng không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID21 - Successful updateRoomStatus normalizing uppercase status OCCUPIED to Occupied")
    void should_updateRoomStatusSuccessfully_when_uppercaseStatusProvided() {
        UpdateRoomRequest request = new UpdateRoomRequest();
        request.setStatus("OCCUPIED");

        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomStatusResponse response = roomService.updateRoomStatus(1, request);

        assertNotNull(response);
        assertEquals("Occupied", response.getStatus());
    }
}
