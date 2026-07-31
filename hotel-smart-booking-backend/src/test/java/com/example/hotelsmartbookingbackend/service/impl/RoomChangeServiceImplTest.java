package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;
import com.example.hotelsmartbookingbackend.entity.*;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.*;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomChangeServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private BookingRoomAccessRepository bookingRoomAccessRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private CustomerRequestRepository customerRequestRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RoomChangeServiceImpl roomChangeService;

    private User receptionistUser;
    private User customerUser;
    private User otherCustomerUser;
    private Booking sampleBooking;
    private BookingDetail sampleBookingDetail;
    private Room oldRoom;
    private Room newRoomSameType;
    private Room newRoomHigherType;
    private Room newRoomLowerType;
    private RoomType standardType;
    private RoomType deluxeType;
    private RoomType economyType;
    private BookingRoomAccess sampleRoomAccess;

    @BeforeEach
    void setUp() {
        receptionistUser = new User();
        receptionistUser.setId(1);
        receptionistUser.setEmail("staff@hotel.com");
        receptionistUser.setFullName("Staff Person");
        receptionistUser.setRole(Role.receptionist);

        customerUser = new User();
        customerUser.setId(2);
        customerUser.setEmail("customer@example.com");
        customerUser.setFullName("Customer Person");
        customerUser.setRole(Role.customer);

        otherCustomerUser = new User();
        otherCustomerUser.setId(9);
        otherCustomerUser.setEmail("other@example.com");
        otherCustomerUser.setRole(Role.customer);

        standardType = new RoomType();
        standardType.setId(1);
        standardType.setName("Standard Room");
        standardType.setBasePrice(new BigDecimal("1000000.00"));

        deluxeType = new RoomType();
        deluxeType.setId(2);
        deluxeType.setName("Deluxe Room");
        deluxeType.setBasePrice(new BigDecimal("1500000.00"));

        economyType = new RoomType();
        economyType.setId(3);
        economyType.setName("Economy Room");
        economyType.setBasePrice(new BigDecimal("800000.00"));

        oldRoom = new Room();
        oldRoom.setId(101);
        oldRoom.setRoomNumber("101");
        oldRoom.setStatus("Occupied");
        oldRoom.setRoomType(standardType);

        newRoomSameType = new Room();
        newRoomSameType.setId(102);
        newRoomSameType.setRoomNumber("102");
        newRoomSameType.setStatus("Available");
        newRoomSameType.setRoomType(standardType);

        newRoomHigherType = new Room();
        newRoomHigherType.setId(201);
        newRoomHigherType.setRoomNumber("201");
        newRoomHigherType.setStatus("Available");
        newRoomHigherType.setRoomType(deluxeType);

        newRoomLowerType = new Room();
        newRoomLowerType.setId(301);
        newRoomLowerType.setRoomNumber("301");
        newRoomLowerType.setStatus("Available");
        newRoomLowerType.setRoomType(economyType);

        sampleBooking = new Booking();
        sampleBooking.setId(1);
        sampleBooking.setBookingReference("BK20260729001");
        sampleBooking.setStatus(BookingStatus.CHECKED_IN);
        sampleBooking.setTotalAmount(new BigDecimal("2000000.00"));
        sampleBooking.setTaxAmount(new BigDecimal("200000.00"));
        sampleBooking.setFinalAmount(new BigDecimal("2200000.00"));
        sampleBooking.setDiscountAmount(BigDecimal.ZERO);
        sampleBooking.setUser(customerUser);

        ZonedDateTime checkIn = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).minusDays(1);
        ZonedDateTime checkOut = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).plusDays(2);

        sampleBookingDetail = new BookingDetail();
        sampleBookingDetail.setId(1);
        sampleBookingDetail.setBooking(sampleBooking);
        sampleBookingDetail.setRoom(oldRoom);
        sampleBookingDetail.setRoomType(standardType);
        sampleBookingDetail.setQuantity(1);
        sampleBookingDetail.setPriceAtBooking(new BigDecimal("1000000.00"));
        sampleBookingDetail.setExpectedCheckIn(checkIn.toInstant());
        sampleBookingDetail.setExpectedCheckOut(checkOut.toInstant());

        sampleRoomAccess = new BookingRoomAccess();
        sampleRoomAccess.setId(1);
        sampleRoomAccess.setBooking(sampleBooking);
        sampleRoomAccess.setRoom(oldRoom);
        sampleRoomAccess.setRoomKeyExpiredAt(checkOut.toInstant());
    }

    // ==========================================
    // 1. changeRoom Test Cases (UTCID01 - UTCID11, UTCID29 - UTCID32)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful room change to SAME room type")
    void should_changeRoomSuccessfully_when_swapToSameRoomType() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);
        request.setReason("Noise complaint");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("SAME_TYPE", response.getChangeType());
        assertEquals("101", response.getOldRoomNumber());
        assertEquals("102", response.getNewRoomNumber());
        assertEquals("Available", oldRoom.getStatus());
        assertEquals("Occupied", newRoomSameType.getStatus());
        verify(webSocketService).broadcastRoomStatus(101, "101", "Available");
        verify(webSocketService).broadcastRoomStatus(102, "102", "Occupied");
    }

    @Test
    @DisplayName("UTCID02 - Successful room change to HIGHER room type (UPGRADE)")
    void should_changeRoomSuccessfully_when_swapToHigherRoomTypeUpgrade() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(201);
        request.setReason("Customer upgrade");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(201)).thenReturn(Optional.of(newRoomHigherType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("UPGRADE", response.getChangeType());
        assertTrue(response.getPriceDifference().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("UTCID03 - Successful room change to LOWER room type (DOWNGRADE)")
    void should_changeRoomSuccessfully_when_swapToLowerRoomTypeDowngrade() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(301);
        request.setReason("Downgrade request");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(301)).thenReturn(Optional.of(newRoomLowerType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("DOWNGRADE", response.getChangeType());
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when staff email is not found in DB")
    void should_throwException_when_changeRoomWithStaffNotFound() {
        RoomChangeRequest request = new RoomChangeRequest();
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "unknown@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Không tìm thấy tài khoản nhân viên"));
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when staff user role is customer")
    void should_throwException_when_changeRoomWithUnauthorizedStaffRole() {
        RoomChangeRequest request = new RoomChangeRequest();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "customer@example.com"));

        assertTrue(ex.getMessage().contains("Bạn không có quyền thực hiện chức năng chuyển phòng"));
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when booking ID is not found in DB")
    void should_throwException_when_changeRoomWithBookingNotFound() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(99);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Không tìm thấy đơn đặt phòng với ID"));
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when booking status is not CHECKED_IN")
    void should_throwException_when_changeRoomWithBookingNotCheckedIn() {
        sampleBooking.setStatus(BookingStatus.PAID);

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("Không thể chuyển phòng. Đơn đặt phòng"));
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when current room is null (unassigned)")
    void should_throwException_when_changeRoomWithCurrentRoomUnassigned() {
        sampleBookingDetail.setRoom(null);

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("chưa được gán phòng cụ thể"));
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when new room ID is not found in DB")
    void should_throwException_when_changeRoomWithNewRoomNotFound() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(999);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Không tìm thấy phòng với ID"));
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when new room ID equals current room ID")
    void should_throwException_when_changeRoomWithNewRoomSameAsCurrentRoom() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(101);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(101)).thenReturn(Optional.of(oldRoom));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("Phòng mới không được trùng với phòng hiện tại"));
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when new room status is not Available")
    void should_throwException_when_changeRoomWithNewRoomOccupied() {
        newRoomSameType.setStatus("Occupied");

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("hiện không trống"));
    }

    @Test
    @DisplayName("UTCID29 - Catch notification exception silently during changeRoom")
    void should_catchNotificationExceptionSilently_when_changingRoom() {
        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        doThrow(new RuntimeException("Notification server error")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> roomChangeService.changeRoom(request, "staff@hotel.com"));
    }

    @Test
    @DisplayName("UTCID30 - Clamp remainingNights to minimum 1 when today equals checkoutDate")
    void should_clampRemainingNightsToOne_when_todayEqualsCheckoutDate() {
        ZonedDateTime todayOut = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));
        sampleBookingDetail.setExpectedCheckOut(todayOut.toInstant());

        deluxeType.setBasePrice(new BigDecimal("2500000.00")); // 1 night = 2,750,000 > current finalAmount 2,200,000

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(201); // Upgrade

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(201)).thenReturn(Optional.of(newRoomHigherType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("UPGRADE", response.getChangeType());
    }

    @Test
    @DisplayName("UTCID31 - Fallback to first BookingRoomAccess record when old room is not matched in list")
    void should_fallbackToFirstAccessRecord_when_oldRoomNotFoundInAccessList() {
        Room otherRoom = new Room();
        otherRoom.setId(999);
        sampleRoomAccess.setRoom(otherRoom); // Access points to a different room

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(newRoomSameType, sampleRoomAccess.getRoom());
    }

    @Test
    @DisplayName("UTCID32 - Use staff email as changedByStaff when staff fullName is null")
    void should_useStaffEmail_when_staffFullNameIsNull() {
        receptionistUser.setFullName(null);

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));

        RoomChangeResponse response = roomChangeService.changeRoom(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("staff@hotel.com", response.getChangedByStaff());
    }

    // ==========================================
    // 2. Room Change Request Methods (UTCID12 - UTCID17, UTCID33 - UTCID38)
    // ==========================================

    @Test
    @DisplayName("UTCID12 - Successful submitRoomChangeRequest by booking owner")
    void should_submitRoomChangeRequestSuccessfully_when_customerSubmitsForCheckedInBooking() {
        CustomerRoomChangeRequest request = new CustomerRoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(2);
        request.setReason("Want ocean view");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> {
            CustomerRequest r = i.getArgument(0);
            r.setId(10);
            return r;
        });

        CustomerRequestResponse response = roomChangeService.submitRoomChangeRequest(request, "customer@example.com");

        assertNotNull(response);
        assertEquals(10, response.getRequestId());
        assertEquals("Pending", response.getStatus());
        assertEquals("RoomChange", response.getRequestType());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when user submitting room change request is unauthorized")
    void should_throwException_when_submitRoomChangeRequestWithUnauthorizedUser() {
        CustomerRoomChangeRequest request = new CustomerRoomChangeRequest();
        request.setBookingId(1);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitRoomChangeRequest(request, "other@example.com"));

        assertEquals("Bạn không có quyền gửi yêu cầu chuyển phòng cho đơn đặt phòng này.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID14 - Successful approveRoomChangeRequest by staff")
    void should_approveRoomChangeRequestSuccessfully_when_staffApprovesWithAvailableRoom() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setBooking(sampleBooking);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");
        req.setNewValue("1");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.approveRoomChangeRequest(10, 102, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Approved", response.getStatus());
    }

    @Test
    @DisplayName("UTCID15 - Throw exception when approved room's roomType does not match requested roomType")
    void should_throwException_when_approveRoomChangeRequestWithRoomTypeMismatch() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setBooking(sampleBooking);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");
        req.setNewValue("2");

        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(10, 102, "staff@hotel.com"));

        assertEquals("Phòng được chọn không thuộc hạng phòng mà khách hàng yêu cầu đổi sang.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID16 - Successful rejectRoomChangeRequest by staff")
    void should_rejectRoomChangeRequestSuccessfully_when_staffRejects() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setBooking(sampleBooking);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.rejectRoomChangeRequest(10, "Fully booked", "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Rejected", response.getStatus());
        assertEquals("Fully booked", response.getRejectionReason());
    }

    @Test
    @DisplayName("UTCID17 - Successful getPendingRoomChangeRequests by staff")
    void should_getPendingRoomChangeRequestsSuccessfully_when_receptionistQueries() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setBooking(sampleBooking);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findByRequestTypeAndStatusOrderByCreatedAtDesc("RoomChange", "Pending"))
                .thenReturn(List.of(req));

        List<CustomerRequestResponse> list = roomChangeService.getPendingRoomChangeRequests("staff@hotel.com");

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("UTCID33 - Catch notification exception silently during submitRoomChangeRequest")
    void should_catchNotificationExceptionSilently_when_submittingRoomChangeRequest() {
        CustomerRoomChangeRequest request = new CustomerRoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(2);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification error")).when(notificationService).sendNotificationToRoles(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> roomChangeService.submitRoomChangeRequest(request, "customer@example.com"));
    }

    @Test
    @DisplayName("UTCID34 - Throw exception when approving request with non-RoomChange requestType")
    void should_throwException_when_approveRoomChangeRequestWithWrongType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setRequestType("StayExtension");

        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(10, 102, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("không phải là loại chuyển phòng"));
    }

    @Test
    @DisplayName("UTCID35 - Throw exception when approving non-pending room change request")
    void should_throwException_when_approveRoomChangeRequestWithNonPendingStatus() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setRequestType("RoomChange");
        req.setStatus("Approved");

        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(10, 102, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("đã được xử lý từ trước"));
    }

    @Test
    @DisplayName("UTCID36 - Throw exception when new room is occupied during approveRoomChangeRequest")
    void should_throwException_when_approveRoomChangeRequestWithOccupiedRoom() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");

        newRoomSameType.setStatus("Occupied");

        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));
        when(roomRepository.findById(102)).thenReturn(Optional.of(newRoomSameType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(10, 102, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("hiện tại không trống"));
    }

    @Test
    @DisplayName("UTCID37 - Throw exception when rejecting request with wrong type or non-pending status")
    void should_throwException_when_rejectRoomChangeRequestWithWrongType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setRequestType("StayExtension");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.rejectRoomChangeRequest(10, "Reason", "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("không phải là loại chuyển phòng"));
    }

    @Test
    @DisplayName("UTCID38 - Catch notification exception silently during rejectRoomChangeRequest")
    void should_catchNotificationExceptionSilently_when_rejectingRoomChangeRequest() {
        CustomerRequest req = new CustomerRequest();
        req.setId(10);
        req.setBooking(sampleBooking);
        req.setRequestType("RoomChange");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(req));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification error")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> roomChangeService.rejectRoomChangeRequest(10, "Reason", "staff@hotel.com"));
    }

    // ==========================================
    // 3. Stay Extension Request Methods (UTCID18 - UTCID23, UTCID39 - UTCID45)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful submitStayExtensionRequest by customer")
    void should_submitStayExtensionRequestSuccessfully_when_validNewCheckOutProvided() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerStayExtensionRequest request = new CustomerStayExtensionRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(newOut.toString());
        request.setDescription("Extend 2 nights");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> {
            CustomerRequest r = i.getArgument(0);
            r.setId(20);
            return r;
        });

        CustomerRequestResponse response = roomChangeService.submitStayExtensionRequest(request, "customer@example.com");

        assertNotNull(response);
        assertEquals(20, response.getRequestId());
        assertEquals("Pending", response.getStatus());
        assertEquals("StayExtension", response.getRequestType());
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when submitStayExtensionRequest has newCheckOut <= currentCheckOut")
    void should_throwException_when_submitStayExtensionRequestWithNewCheckOutBeforeCurrentCheckOut() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();

        CustomerStayExtensionRequest request = new CustomerStayExtensionRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(currentOut.toString());

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitStayExtensionRequest(request, "customer@example.com"));

        assertTrue(ex.getMessage().startsWith("Ngày trả phòng mới phải sau ngày trả phòng hiện tại"));
    }

    @Test
    @DisplayName("UTCID20 - Successful approveStayExtensionRequest by staff")
    void should_approveStayExtensionRequestSuccessfully_when_staffApproves() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(1L);
        when(bookingDetailRepository.existsOverlappingBookingForRoom(any(), any(), any(), any())).thenReturn(false);
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Approved", response.getStatus());
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when physical room has overlapping future booking during stay extension")
    void should_throwException_when_approveStayExtensionRequestWithPhysicalRoomOverlap() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(1L);
        when(bookingDetailRepository.existsOverlappingBookingForRoom(any(), any(), any(), any())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Không thể gia hạn chính phòng này vì phòng vật lý"));
    }

    @Test
    @DisplayName("UTCID22 - Successful rejectStayExtensionRequest by staff")
    void should_rejectStayExtensionRequestSuccessfully_when_staffRejects() {
        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.rejectStayExtensionRequest(20, "Fully booked", "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Rejected", response.getStatus());
    }

    @Test
    @DisplayName("UTCID23 - Successful getPendingStayExtensionRequests by staff")
    void should_getPendingStayExtensionRequestsSuccessfully_when_receptionistQueries() {
        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findByRequestTypeAndStatusOrderByCreatedAtDesc("StayExtension", "Pending"))
                .thenReturn(List.of(req));

        List<CustomerRequestResponse> list = roomChangeService.getPendingStayExtensionRequests("staff@hotel.com");

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("UTCID39 - Throw exception when total active rooms is 0 during stay extension availability check")
    void should_throwException_when_totalRoomsIsZeroInStayExtension() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(0L); // No active rooms

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com"));

        assertEquals("Không có phòng đang hoạt động cho loại phòng này", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID40 - Throw exception when booked quantity exceeds total available rooms in stay extension")
    void should_throwException_when_bookedQuantityExceedsAvailableRoomsInStayExtension() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(2L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(2L); // 0 remaining rooms!

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Không đủ phòng trống vào ngày"));
    }

    @Test
    @DisplayName("UTCID42 - Clamp finalAmount to zero when discount exceeds total cost during stay extension")
    void should_clampFinalAmountToZero_when_discountExceedsTotalInStayExtension() {
        sampleBooking.setDiscountAmount(new BigDecimal("9999999.00")); // Huge discount

        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(1L);
        when(bookingDetailRepository.existsOverlappingBookingForRoom(any(), any(), any(), any())).thenReturn(false);
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com");

        assertEquals(BigDecimal.ZERO, sampleBooking.getFinalAmount());
    }

    @Test
    @DisplayName("UTCID43 - Safely handle empty roomAccesses list during approveStayExtensionRequest")
    void should_handleEmptyRoomAccessesSafely_when_approvingStayExtension() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(1L);
        when(bookingDetailRepository.existsOverlappingBookingForRoom(any(), any(), any(), any())).thenReturn(false);
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of()); // Empty accesses
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com"));
    }

    @Test
    @DisplayName("UTCID44 - Catch notification exception silently during approveStayExtensionRequest")
    void should_catchNotificationExceptionSilently_when_approvingStayExtension() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = currentOut.plusDays(2);

        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setBooking(sampleBooking);
        req.setRequestType("StayExtension");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(any(), any(), any(), any(), any())).thenReturn(1L);
        when(bookingDetailRepository.existsOverlappingBookingForRoom(any(), any(), any(), any())).thenReturn(false);
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification error")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> roomChangeService.approveStayExtensionRequest(20, "staff@hotel.com"));
    }

    @Test
    @DisplayName("UTCID45 - Throw exception when rejecting stay extension request with wrong type")
    void should_throwException_when_rejectStayExtensionRequestWithWrongType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(20);
        req.setRequestType("RoomChange");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(20)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.rejectStayExtensionRequest(20, "Reason", "staff@hotel.com"));

        assertEquals("Yêu cầu này không phải là yêu cầu gia hạn", ex.getMessage());
    }

    // ==========================================
    // 4. Early Check-Out Request Methods (UTCID24 - UTCID28, UTCID46 - UTCID49)
    // ==========================================

    @Test
    @DisplayName("UTCID24 - Successful submitEarlyCheckOutRequest by customer")
    void should_submitEarlyCheckOutRequestSuccessfully_when_validNewCheckOutProvided() {
        LocalDate checkIn = sampleBookingDetail.getExpectedCheckIn().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = checkIn.plusDays(1);

        CustomerEarlyCheckOutRequest request = new CustomerEarlyCheckOutRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(newOut.toString());
        request.setDescription("Leaving early");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> {
            CustomerRequest r = i.getArgument(0);
            r.setId(30);
            return r;
        });

        CustomerRequestResponse response = roomChangeService.submitEarlyCheckOutRequest(request, "customer@example.com");

        assertNotNull(response);
        assertEquals(30, response.getRequestId());
        assertEquals("Pending", response.getStatus());
        assertEquals("EarlyCheckOut", response.getRequestType());
    }

    @Test
    @DisplayName("UTCID25 - Throw exception when submitEarlyCheckOutRequest has newCheckOut >= currentCheckOut")
    void should_throwException_when_submitEarlyCheckOutRequestWithNewCheckOutAfterCurrentCheckOut() {
        LocalDate currentOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();

        CustomerEarlyCheckOutRequest request = new CustomerEarlyCheckOutRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(currentOut.toString());

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitEarlyCheckOutRequest(request, "customer@example.com"));

        assertEquals("Ngày check-out mới phải sớm hơn ngày check-out hiện tại", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID26 - Successful approveEarlyCheckOutRequest by staff")
    void should_approveEarlyCheckOutRequestSuccessfully_when_staffApproves() {
        LocalDate checkIn = sampleBookingDetail.getExpectedCheckIn().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate newOut = checkIn.plusDays(1);

        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setBooking(sampleBooking);
        req.setRequestType("EarlyCheckOut");
        req.setStatus("Pending");
        req.setNewValue(newOut.toString());

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(30)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.approveEarlyCheckOutRequest(30, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Approved", response.getStatus());
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("UTCID27 - Successful rejectEarlyCheckOutRequest by staff")
    void should_rejectEarlyCheckOutRequestSuccessfully_when_staffRejects() {
        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setBooking(sampleBooking);
        req.setRequestType("EarlyCheckOut");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(30)).thenReturn(Optional.of(req));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = roomChangeService.rejectEarlyCheckOutRequest(30, "Policy violation", "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Rejected", response.getStatus());
    }

    @Test
    @DisplayName("UTCID28 - Successful getPendingEarlyCheckOutRequests by staff")
    void should_getPendingEarlyCheckOutRequestsSuccessfully_when_receptionistQueries() {
        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setBooking(sampleBooking);
        req.setRequestType("EarlyCheckOut");
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findByRequestTypeAndStatusOrderByCreatedAtDesc("EarlyCheckOut", "Pending"))
                .thenReturn(List.of(req));

        List<CustomerRequestResponse> list = roomChangeService.getPendingEarlyCheckOutRequests("staff@hotel.com");

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("UTCID46 - Throw exception when submitEarlyCheckOutRequest has newCheckOut before checkIn date")
    void should_throwException_when_submitEarlyCheckOutRequestWithNewCheckOutBeforeCheckIn() {
        LocalDate checkIn = sampleBookingDetail.getExpectedCheckIn().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        LocalDate invalidNewOut = checkIn.minusDays(1); // Before check-in!

        CustomerEarlyCheckOutRequest request = new CustomerEarlyCheckOutRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(invalidNewOut.toString());

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitEarlyCheckOutRequest(request, "customer@example.com"));

        assertTrue(ex.getMessage().startsWith("Ngày check-out mới không được trước ngày check-in"));
    }

    @Test
    @DisplayName("UTCID47 - Throw exception when approving early check-out with wrong requestType")
    void should_throwException_when_approveEarlyCheckOutRequestWithWrongType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setRequestType("StayExtension");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(30)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveEarlyCheckOutRequest(30, "staff@hotel.com"));

        assertEquals("Yêu cầu này không phải là yêu cầu check-out sớm", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID48 - Minimum 1 night stay cost calculation when newCheckOut equals checkIn during approveEarlyCheckOutRequest")
    void should_calculateMinimumOneNightCost_when_newCheckOutEqualsCheckInInEarlyCheckOut() {
        LocalDate checkIn = sampleBookingDetail.getExpectedCheckIn().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();

        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setBooking(sampleBooking);
        req.setRequestType("EarlyCheckOut");
        req.setStatus("Pending");
        req.setNewValue(checkIn.toString()); // Same day as check-in (0 nights -> 1 night min)

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(30)).thenReturn(Optional.of(req));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(sampleRoomAccess));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        roomChangeService.approveEarlyCheckOutRequest(30, "staff@hotel.com");

        assertEquals(new BigDecimal("1000000.00"), sampleBooking.getTotalAmount()); // 1 night * 1,000,000
    }

    @Test
    @DisplayName("UTCID49 - Throw exception when rejecting early check-out request with wrong type")
    void should_throwException_when_rejectEarlyCheckOutRequestWithWrongType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(30);
        req.setRequestType("RoomChange");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(30)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.rejectEarlyCheckOutRequest(30, "Reason", "staff@hotel.com"));

        assertEquals("Yêu cầu này không phải là yêu cầu check-out sớm", ex.getMessage());
    }

    // ==========================================
    // Additional Branch Coverage Tests
    // ==========================================

    @Test
    @DisplayName("Throw exception when target room status is Occupied during staff room change")
    void should_throwException_when_changeRoomWithTargetRoomOccupied() {
        Room targetOccupiedRoom = new Room();
        targetOccupiedRoom.setId(102);
        targetOccupiedRoom.setRoomNumber("102");
        targetOccupiedRoom.setStatus("Occupied");
        targetOccupiedRoom.setRoomType(standardType);

        RoomChangeRequest request = new RoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);
        request.setReason("Room upgrade");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(roomRepository.findById(102)).thenReturn(Optional.of(targetOccupiedRoom)); // status = "Occupied"

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.changeRoom(request, "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("hiện không trống"));
    }

    @Test
    @DisplayName("Throw exception when customer requests room change for booking that is not Checked In")
    void should_throwException_when_requestRoomChangeWithNotCheckedInBooking() {
        CustomerRoomChangeRequest request = new CustomerRoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(1);
        request.setReason("Noise issue");

        Booking unconfirmedBooking = new Booking();
        unconfirmedBooking.setId(1);
        unconfirmedBooking.setStatus(BookingStatus.CONFIRMED); // Not Checked In!
        unconfirmedBooking.setUser(customerUser);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(unconfirmedBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitRoomChangeRequest(request, "customer@example.com"));
        assertTrue(ex.getMessage().contains("khi đang lưu trú"));
    }

    @Test
    @DisplayName("Throw exception when approving a room change request that is already Approved")
    void should_throwException_when_approveRoomChangeRequestWithAlreadyProcessedStatus() {
        CustomerRequest req = new CustomerRequest();
        req.setId(50);
        req.setRequestType("RoomChange");
        req.setStatus("Approved");

        when(customerRequestRepository.findById(50)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(50, 102, "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("đã được xử lý từ trước"));
    }

    @Test
    @DisplayName("Throw exception when rejecting a request with wrong request type")
    void should_throwException_when_rejectRoomChangeRequestWithInvalidRequestType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(51);
        req.setRequestType("StayExtension"); // Wrong type!
        req.setStatus("Pending");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(51)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.rejectRoomChangeRequest(51, "Reason", "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("không phải là loại chuyển phòng"));
    }

    @Test
    @DisplayName("Throw exception when customer requests stay extension with invalid new check-out date")
    void should_throwException_when_requestStayExtensionWithNewCheckOutDateBeforeCurrent() {
        LocalDate currentCheckOut = sampleBookingDetail.getExpectedCheckOut().atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        CustomerStayExtensionRequest request = new CustomerStayExtensionRequest();
        request.setBookingId(1);
        request.setNewCheckOutDate(currentCheckOut.minusDays(1).toString()); // Invalid date!
        request.setDescription("Need more time");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(sampleBookingDetail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitStayExtensionRequest(request, "customer@example.com"));
        assertTrue(ex.getMessage().contains("Ngày trả phòng mới phải sau ngày trả phòng hiện tại"));
    }

    @Test
    @DisplayName("Throw exception when customer requests room change for non-checked-in booking")
    void should_throwException_when_requestRoomChangeWithNonCheckedInStatus() {
        CustomerRoomChangeRequest request = new CustomerRoomChangeRequest();
        request.setBookingId(1);
        request.setNewRoomId(102);

        Booking confirmedBooking = new Booking();
        confirmedBooking.setId(1);
        confirmedBooking.setStatus(BookingStatus.CONFIRMED); // Not CHECKED_IN!
        confirmedBooking.setUser(customerUser);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(confirmedBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.submitRoomChangeRequest(request, "customer@example.com"));
        assertEquals("Chỉ có thể gửi yêu cầu chuyển phòng khi đang lưu trú (Đã Check-in).", ex.getMessage());
    }

    @Test
    @DisplayName("Throw exception when approving a request with wrong request type")
    void should_throwException_when_approveRoomChangeRequestWithNonRoomChangeType() {
        CustomerRequest req = new CustomerRequest();
        req.setId(52);
        req.setRequestType("StayExtension"); // Not RoomChange!
        req.setStatus("Pending");

        when(customerRequestRepository.findById(52)).thenReturn(Optional.of(req));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> roomChangeService.approveRoomChangeRequest(52, 102, "staff@hotel.com"));
        assertEquals("Yêu cầu này không phải là loại chuyển phòng (RoomChange).", ex.getMessage());
    }
}


