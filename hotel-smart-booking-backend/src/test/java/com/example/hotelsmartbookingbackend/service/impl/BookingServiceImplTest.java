package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.AddServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceVerificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.QrTokenResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.EkycProfile;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingServiceRepository;
import com.example.hotelsmartbookingbackend.repository.CustomerRequestRepository;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceEmbeddingRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.EmailService;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import com.example.hotelsmartbookingbackend.specification.BookingSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private BookingRoomAccessRepository bookingRoomAccessRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EkycProfileRepository ekycProfileRepository;
    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;
    @Mock
    private AesEncryptionService aesEncryptionService;
    @Mock
    private SupabaseStorageService supabaseStorageService;
    @Mock
    private WebClient webClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingServiceRepository bookingServiceRepository;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CustomerRequestRepository customerRequestRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    private User customerUser;
    private User staffUser;
    private User managerUser;
    private RoomType defaultRoomType;
    private Room defaultRoom;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(1);
        customerUser.setEmail("john@example.com");
        customerUser.setFullName("John Doe");
        customerUser.setRole(Role.customer);

        staffUser = new User();
        staffUser.setId(2);
        staffUser.setEmail("staff@hotel.com");
        staffUser.setFullName("Staff Member");
        staffUser.setRole(Role.receptionist);

        managerUser = new User();
        managerUser.setId(3);
        managerUser.setEmail("manager@hotel.com");
        managerUser.setFullName("Manager Person");
        managerUser.setRole(Role.manager);

        defaultRoomType = new RoomType();
        defaultRoomType.setId(1);
        defaultRoomType.setName("Deluxe Suite");
        defaultRoomType.setStatus("Active");
        defaultRoomType.setAdultCapacity(2);
        defaultRoomType.setChildCapacity(1);
        defaultRoomType.setBasePrice(new BigDecimal("1000000.00"));

        defaultRoom = new Room();
        defaultRoom.setId(101);
        defaultRoom.setRoomNumber("101");
        defaultRoom.setFloorNumber(1);
        defaultRoom.setStatus("Available");
        defaultRoom.setRoomType(defaultRoomType);
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(HOTEL_ZONE).toInstant();
    }

    // ==========================================
    // 1. createBooking Test Cases (UTCID01 - UTCID09, UTCID58)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful createBooking for online customer")
    void should_createBookingSuccessfully_when_validRequest() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(3));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(0);
        request.setCheckInMethod("Manual");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(eq(1), any(), any(), anyList(), anyList())).thenReturn(0L);

        Booking savedBooking = new Booking();
        savedBooking.setId(10);
        savedBooking.setBookingReference("BK202607280001");
        savedBooking.setCheckInMethod("Manual");
        savedBooking.setStatus(BookingStatus.CONFIRMED);
        savedBooking.setUser(customerUser);
        savedBooking.setTotalAmount(new BigDecimal("2000000.00"));
        savedBooking.setTaxAmount(new BigDecimal("200000.00"));
        savedBooking.setFinalAmount(new BigDecimal("2200000.00"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, "john@example.com");

        assertNotNull(response);
        assertEquals(10, response.getBookingId());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingRepository).save(any(Booking.class));
        verify(bookingDetailRepository).save(any(BookingDetail.class));
        verify(notificationService).sendNotification(eq(customerUser), anyString(), anyString(), eq("Booking"), eq(10));
    }

    @Test
    @DisplayName("UTCID02 - Throw exception when checkInDate or checkOutDate is null")
    void should_throwException_when_createBookingWithNullCheckInOrCheckOutDate() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCheckInDate(null);
        request.setCheckOutDate(LocalDate.now(HOTEL_ZONE).plusDays(2));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Vui lòng chọn ngày nhận phòng và ngày trả phòng", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when checkInDate is in the past")
    void should_throwException_when_createBookingWithCheckInDateInPast() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCheckInDate(today.minusDays(1));
        request.setCheckOutDate(today.plusDays(1));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Ngày nhận phòng không được là ngày trong quá khứ", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when checkOutDate is on or before checkInDate")
    void should_throwException_when_createBookingWithCheckOutDateOnOrBeforeCheckInDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCheckInDate(today.plusDays(2));
        request.setCheckOutDate(today.plusDays(2));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Ngày trả phòng phải sau ngày nhận phòng", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when customer email is not found")
    void should_throwException_when_createBookingWithCustomerNotFound() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "unknown@example.com"));
        assertEquals("Không tìm thấy khách hàng", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when FaceID method is selected without eKYC registration")
    void should_throwException_when_createBookingWithFaceIdSelectedWithoutEkyc() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);
        request.setCheckInMethod("FaceID");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Bạn phải hoàn thành đăng ký eKYC và khuôn mặt trước khi chọn check-in bằng FaceID", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when RoomType status is inactive")
    void should_throwException_when_createBookingWithInactiveRoomType() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);

        RoomType inactiveRoomType = new RoomType();
        inactiveRoomType.setId(1);
        inactiveRoomType.setStatus("Inactive");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(inactiveRoomType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Loại phòng hiện không hoạt động", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when guest count exceeds room capacity")
    void should_throwException_when_createBookingWithGuestsExceedingCapacity() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));
        request.setNumberOfAdults(5); // Capacity is 2
        request.setNumberOfChildren(0);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Số người lớn vượt quá sức chứa của loại phòng", ex.getMessage());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when available room inventory is insufficient")
    void should_throwException_when_createBookingWithInsufficientAvailableRooms() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(0);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(2L);
        when(bookingDetailRepository.sumBookedQuantity(eq(1), any(), any(), anyList(), anyList())).thenReturn(2L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertTrue(ex.getMessage().contains("Không đủ phòng trống vào ngày"));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("UTCID58 - Successful createBooking even if notificationService throws exception")
    void should_createBookingSuccessfully_when_notificationServiceThrowsException() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(3));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(0);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingDetailRepository.sumBookedQuantity(eq(1), any(), any(), anyList(), anyList())).thenReturn(0L);

        Booking savedBooking = new Booking();
        savedBooking.setId(10);
        savedBooking.setStatus(BookingStatus.CONFIRMED);
        savedBooking.setUser(customerUser);
        savedBooking.setTotalAmount(new BigDecimal("2000000.00"));
        savedBooking.setFinalAmount(new BigDecimal("2200000.00"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        doThrow(new RuntimeException("Notification service down")).when(notificationService)
                .sendNotification(any(), anyString(), anyString(), anyString(), any());

        BookingResponse response = bookingService.createBooking(request, "john@example.com");

        assertNotNull(response);
        assertEquals(10, response.getBookingId());
        verify(bookingRepository).save(any(Booking.class));
    }

    // ==========================================
    // 2. createGroupBooking Test Cases (UTCID10 - UTCID12)
    // ==========================================

    @Test
    @DisplayName("UTCID10 - Successful createGroupBooking for quantity >= 2")
    void should_createGroupBookingSuccessfully_when_validGroupRequest() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateGroupBookingRequest request = new CreateGroupBookingRequest();
        request.setRoomTypeId(1);
        request.setQuantity(3);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(3));
        request.setNumberOfAdults(4);
        request.setNumberOfChildren(0);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(10L);
        when(bookingDetailRepository.sumBookedQuantity(eq(1), any(), any(), anyList(), anyList())).thenReturn(0L);

        Booking savedBooking = new Booking();
        savedBooking.setId(20);
        savedBooking.setBookingType("Group");
        savedBooking.setStatus(BookingStatus.CONFIRMED);
        savedBooking.setUser(customerUser);
        savedBooking.setTotalAmount(new BigDecimal("6000000.00"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createGroupBooking(request, "john@example.com");

        assertNotNull(response);
        assertEquals(20, response.getBookingId());
        assertEquals("Group", response.getBookingType());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when group booking quantity is less than 2")
    void should_throwException_when_createGroupBookingWithQuantityLessThan2() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateGroupBookingRequest request = new CreateGroupBookingRequest();
        request.setQuantity(1);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createGroupBooking(request, "john@example.com"));
        assertEquals("Số lượng phòng cho đặt nhóm phải ít nhất là 2", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when group booking quantity is zero or negative")
    void should_throwException_when_createGroupBookingWithQuantityZeroOrNegative() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        CreateGroupBookingRequest request = new CreateGroupBookingRequest();
        request.setQuantity(0);
        request.setCheckInDate(today.plusDays(1));
        request.setCheckOutDate(today.plusDays(2));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createGroupBooking(request, "john@example.com"));
        assertEquals("Số lượng phòng phải lớn hơn 0", ex.getMessage());
    }

    // ==========================================
    // 3. getBookingHistory & Details Test Cases (UTCID13 - UTCID16)
    // ==========================================

    @Test
    @DisplayName("UTCID13 - Successful getBookingHistory for existing customer")
    void should_getBookingHistorySuccessfully_when_validCustomerEmail() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK001");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE)));
        detail.setExpectedCheckOut(toInstant(LocalDate.now(HOTEL_ZONE).plusDays(2)));

        when(bookingDetailRepository.findBookingHistory("john@example.com")).thenReturn(List.of(detail));

        List<BookingHistoryResponse> history = bookingService.getBookingHistory("john@example.com");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("BK001", history.get(0).getBookingNumber());
        verify(bookingDetailRepository).findBookingHistory("john@example.com");
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when getting booking history for non-existent customer")
    void should_throwException_when_getBookingHistoryWithCustomerNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.getBookingHistory("unknown@example.com"));
        assertEquals("Không tìm thấy người dùng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID15 - Successful getBookingDetail for booking owner")
    void should_getBookingDetailSuccessfully_when_validBookingIdAndCustomerEmail() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK001");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE)));
        detail.setExpectedCheckOut(toInstant(LocalDate.now(HOTEL_ZONE).plusDays(2)));

        when(bookingDetailRepository.findBookingDetail(1, "john@example.com")).thenReturn(Optional.of(detail));

        BookingResponse response = bookingService.getBookingDetail(1, "john@example.com");

        assertNotNull(response);
        assertEquals(1, response.getBookingId());
        verify(bookingDetailRepository).findBookingDetail(1, "john@example.com");
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when booking detail is not found or not owned by customer")
    void should_throwException_when_getBookingDetailWithBookingNotFoundOrNotOwned() {
        when(bookingDetailRepository.findBookingDetail(99, "john@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.getBookingDetail(99, "john@example.com"));
        assertEquals("Không tìm thấy đặt phòng hoặc bạn không có quyền xem đặt phòng này", ex.getMessage());
    }

    // ==========================================
    // 4. performCheckIn & Helper Branches (UTCID17 - UTCID20, UTCID59 - UTCID61, UTCID66 - UTCID68)
    // ==========================================

    @Test
    @DisplayName("UTCID17 - Successful performCheckIn by receptionist staff")
    void should_performCheckInSuccessfully_when_validStaffAndConfirmedBooking() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("BK001");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of(defaultRoom));

        BookingResponse response = bookingService.performCheckIn(1, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals("Occupied", defaultRoom.getStatus());
        verify(roomRepository).saveAll(anyList());
        verify(webSocketService).broadcastRoomStatus(101, "101", "Occupied");
    }

    @Test
    @DisplayName("UTCID18 - Throw exception when check-in is attempted by user with Customer role")
    void should_throwException_when_performCheckInWithCustomerRole() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "john@example.com"));
        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when check-in is attempted on CANCELLED booking")
    void should_throwException_when_performCheckInWithInvalidBookingStatus() {
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertEquals("Đơn đặt phòng không ở trạng thái có thể nhận phòng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID20 - Throw exception when physical available rooms are insufficient at check-in")
    void should_throwException_when_performCheckInWithInsufficientPhysicalRooms() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("Không đủ phòng sẵn sàng để nhận phòng"));
    }

    @Test
    @DisplayName("UTCID59 - Throw exception when physical available rooms are insufficient at check-in (before check-in date window)")
    void should_throwException_when_validateCheckInDateWindowBeforeCheckInDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.plusDays(2)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(4)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("Không đủ phòng sẵn sàng để nhận phòng"));
    }

    @Test
    @DisplayName("UTCID61 - Throw exception when physical available rooms are insufficient at check-in (on/after check-out date)")
    void should_throwException_when_validateCheckInDateWindowOnOrAfterCheckOutDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(5)));
        detail.setExpectedCheckOut(toInstant(today));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertTrue(ex.getMessage().contains("Không đủ phòng sẵn sàng để nhận phòng"));
    }

    @Test
    @DisplayName("UTCID66 - Throw exception in completeCheckIn if booking already has room accesses")
    void should_throwException_when_completeCheckInWithAlreadyAssignedRoomAccesses() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        BookingRoomAccess existingAccess = new BookingRoomAccess();
        existingAccess.setRoom(defaultRoom);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(existingAccess));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertEquals("Booking này đã được cấp quyền truy cập phòng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID67 - Throw exception in completeCheckIn if assigned room type mismatch")
    void should_throwException_when_completeCheckInWithAssignedRoomTypeMismatch() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        RoomType otherRoomType = new RoomType();
        otherRoomType.setId(99);

        Room assignedRoom = new Room();
        assignedRoom.setId(202);
        assignedRoom.setRoomType(otherRoomType);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType); // ID = 1
        detail.setRoom(assignedRoom); // Assigned room has RoomType ID = 99
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertEquals("Phòng được gán không thuộc loại phòng đã đặt", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID68 - Throw exception in completeCheckIn if assigned room is not Available")
    void should_throwException_when_completeCheckInWithAssignedRoomNotAvailable() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        Room assignedRoom = new Room();
        assignedRoom.setId(101);
        assignedRoom.setRoomType(defaultRoomType);
        assignedRoom.setStatus("Occupied"); // Not available

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setRoom(assignedRoom);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performCheckIn(1, "staff@hotel.com"));
        assertEquals("Phòng được gán hiện chưa sẵn sàng để nhận phòng", ex.getMessage());
    }

    // ==========================================
    // 5. performFaceCheckIn Test Cases (UTCID21 - UTCID25, UTCID69 - UTCID72)
    // ==========================================

    @Test
    @DisplayName("UTCID21 - Successful FaceID check-in by Manager with verified AI response")
    void should_performFaceCheckInSuccessfully_when_managerRoleAndFaceVerificationMatches() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "selfie.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile ch1 = new MockMultipartFile("challengeImage", "ch1.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile ch2 = new MockMultipartFile("challengeImage2", "ch2.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile ch3 = new MockMultipartFile("challengeImage3", "ch3.jpg", "image/jpeg", new byte[]{1, 2});

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(1)).thenReturn(Optional.of("embedding_data_string"));

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceVerificationResponse aiResponse = new AiFaceVerificationResponse();
        aiResponse.setLivenessPassed(true);
        aiResponse.setActiveLivenessPassed(true);
        aiResponse.setVerified(true);
        aiResponse.setMatched(true);

        when(responseSpec.bodyToMono(AiFaceVerificationResponse.class)).thenReturn(Mono.just(aiResponse));
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of(defaultRoom));

        BookingResponse response = bookingService.performFaceCheckIn(1, selfie, ch1, ch2, ch3, "left", "manager@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
    }

    @Test
    @DisplayName("UTCID69 - Throw exception when challenge direction is invalid")
    void should_throwException_when_performFaceCheckInWithInvalidChallengeDirection() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "up", "manager@hotel.com"));
        assertEquals("Hướng thử thách FaceID không hợp lệ", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID70 - Throw exception when registered embedding text is missing for customer")
    void should_throwException_when_performFaceCheckInWithMissingRegisteredEmbedding() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertEquals("Không tìm thấy dữ liệu khuôn mặt đã đăng ký", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID71 - Handle WebClientResponseException in callFaceVerificationService")
    void should_handleWebClientResponseException_when_callingFaceVerificationService() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(1)).thenReturn(Optional.of("embedding_text"));

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);

        WebClientResponseException webEx = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);
        when(headersSpec.retrieve()).thenThrow(webEx);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertTrue(ex.getMessage().contains("Face API lỗi"));
    }

    // ==========================================
    // 6. generateQrCheckInToken & performQrCheckIn (UTCID26 - UTCID32, UTCID73 - UTCID74)
    // ==========================================

    @Test
    @DisplayName("UTCID26 - Successful generateQrCheckInToken when no active token exists")
    void should_generateQrCheckInTokenSuccessfully_when_eligibleCustomerNoActiveToken() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR Code");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingDetailRepository.findBookingDetail(1, "john@example.com")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(bookingDetailRepository.existsByQrCodeValue(anyString())).thenReturn(false);

        QrTokenResponse response = bookingService.generateQrCheckInToken(1, "john@example.com");

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(emailService).sendQrCheckInEmail(eq("john@example.com"), any(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("UTCID74 - Safely handle EmailService exception during QR token email sending")
    void should_sendQrCheckInEmailSafely_when_emailServiceThrowsException() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR Code");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingDetailRepository.findBookingDetail(1, "john@example.com")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(bookingDetailRepository.existsByQrCodeValue(anyString())).thenReturn(false);

        doThrow(new RuntimeException("Mail server down")).when(emailService)
                .sendQrCheckInEmail(any(), any(), any(), any(), any(), any(), any(), any());

        QrTokenResponse response = bookingService.generateQrCheckInToken(1, "john@example.com");

        assertNotNull(response);
        assertNotNull(response.getToken());
    }

    // ==========================================
    // 7. performCheckOut Test Cases (UTCID36 - UTCID39, UTCID62 - UTCID65)
    // ==========================================

    @Test
    @DisplayName("UTCID62 - Apply 100% late check-out penalty when late > 6 hours")
    void should_performCheckOutWith100PercentPenalty_when_lateMoreThan6Hours() {
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUser(customerUser);
        booking.setTotalAmount(new BigDecimal("2000000.00"));
        booking.setTaxAmount(new BigDecimal("200000.00"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("3300000.00"));
        booking.setPaidAmount(new BigDecimal("3300000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setPriceAtBooking(new BigDecimal("1000000.00"));
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE).minusDays(2)));
        detail.setExpectedCheckOut(Instant.now().minus(Duration.ofHours(7))); // 7 hours late => 100% penalty

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));

        BookingResponse response = bookingService.performCheckOut(1, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getSpecialRequests().contains("Trễ trên 6 giờ"));
    }

    @Test
    @DisplayName("UTCID63 - Apply 30% late check-out penalty when late <= 3 hours")
    void should_performCheckOutWith30PercentPenalty_when_lateLessThan3Hours() {
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUser(customerUser);
        booking.setTotalAmount(new BigDecimal("2000000.00"));
        booking.setTaxAmount(new BigDecimal("200000.00"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("2530000.00"));
        booking.setPaidAmount(new BigDecimal("2530000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setPriceAtBooking(new BigDecimal("1000000.00"));
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE).minusDays(2)));
        detail.setExpectedCheckOut(Instant.now().minus(Duration.ofMinutes(100)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));

        BookingResponse response = bookingService.performCheckOut(1, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getSpecialRequests().contains("Trễ dưới 3 giờ"));
    }

    @Test
    @DisplayName("UTCID64 - Skip late check-out penalty if specialRequests already contains penalty marker")
    void should_skipLateCheckOutPenalty_when_alreadyChargedPenaltyInSpecialRequests() {
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUser(customerUser);
        booking.setSpecialRequests("[Late Check-out Penalty]");
        booking.setFinalAmount(new BigDecimal("2200000.00"));
        booking.setPaidAmount(new BigDecimal("2200000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE).minusDays(2)));
        detail.setExpectedCheckOut(Instant.now().minus(Duration.ofHours(2)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));

        BookingResponse response = bookingService.performCheckOut(1, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
    }

    // ==========================================
    // 8. createWalkInBooking Test Cases (UTCID42 - UTCID43, UTCID75 - UTCID77)
    // ==========================================

    @Test
    @DisplayName("UTCID75 - Walk-in booking with full payment sets status to CHECKED_IN via instant check-in")
    void should_createWalkInBookingWithFullPayment_when_paidAmountEqualsFinalAmount() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        WalkInBookingRequest request = new WalkInBookingRequest();
        request.setCustomerEmail("fullwalkin@example.com");
        request.setCustomerFullname("Full Paid Guest");
        request.setCustomerPhonenumber("0988888888");
        request.setCustomerIdCardNumber("123456789099");
        request.setRoomTypeId(1);
        request.setQuantity(1);
        request.setCheckInDate(today);
        request.setCheckOutDate(today.plusDays(1));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);
        request.setPaidAmount(new BigDecimal("1100000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(userRepository.findByEmail("fullwalkin@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByIdCardNumber("123456789099")).thenReturn(false);
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);

        User newCustomer = new User();
        newCustomer.setId(5);
        newCustomer.setEmail("fullwalkin@example.com");
        newCustomer.setRole(Role.customer);

        when(userRepository.save(any(User.class))).thenReturn(newCustomer);

        Booking booking = new Booking();
        booking.setId(30);
        booking.setBookingType("Walk-in");
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setFinalAmount(new BigDecimal("1100000.00"));
        booking.setUser(newCustomer);

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of(defaultRoom));

        BookingResponse response = bookingService.createWalkInBooking(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
    }

    @Test
    @DisplayName("UTCID76 - Throw exception when existing customer email is associated with a different CCCD")
    void should_throwException_when_createWalkInBookingWithExistingCustomerMismatchedIdCard() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        WalkInBookingRequest request = new WalkInBookingRequest();
        request.setCustomerEmail("john@example.com");
        request.setCustomerIdCardNumber("999999999999");
        request.setCheckInDate(today);
        request.setCheckOutDate(today.plusDays(1));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);

        User existingCustomer = new User();
        existingCustomer.setEmail("john@example.com");
        existingCustomer.setIdCardNumber("123456789012");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingCustomer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createWalkInBooking(request, "staff@hotel.com"));
        assertEquals("Email khach hang da gan voi so CCCD khac", ex.getMessage());
    }

    // ==========================================
    // 9. getInvoiceDetails & addServiceToBooking (UTCID44 - UTCID48, UTCID78 - UTCID79)
    // ==========================================

    @Test
    @DisplayName("UTCID78 - Successful getInvoiceDetails when actor is Staff receptionist")
    void should_getInvoiceDetailsSuccessfully_when_actorIsStaffReceptionist() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setUser(customerUser);
        booking.setTaxAmount(new BigDecimal("100.00"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1100.00"));
        booking.setPaidAmount(new BigDecimal("1100.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setPriceAtBooking(new BigDecimal("1000.00"));
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE)));
        detail.setExpectedCheckOut(toInstant(LocalDate.now(HOTEL_ZONE).plusDays(1)));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));

        InvoiceResponse invoice = bookingService.getInvoiceDetails(1, "staff@hotel.com");

        assertNotNull(invoice);
        assertEquals(1, invoice.getBookingId());
    }

    @Test
    @DisplayName("UTCID79 - Successful addServiceToBooking when request note is provided")
    void should_addServiceToBookingWithNote_when_requestNoteIsProvided() {
        AddServiceRequest request = new AddServiceRequest();
        request.setServiceId(2);
        request.setQuantity(1);
        request.setNote("Extra towel");

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setServiceChargeAmount(BigDecimal.ZERO);
        booking.setTaxAmount(new BigDecimal("100.00"));
        booking.setFinalAmount(new BigDecimal("1100.00"));
        booking.setPaidAmount(new BigDecimal("1100.00"));

        com.example.hotelsmartbookingbackend.entity.Service service = new com.example.hotelsmartbookingbackend.entity.Service();
        service.setId(2);
        service.setName("Laundry");
        service.setIsActive(true);
        service.setPrice(new BigDecimal("50000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(serviceRepository.findById(2)).thenReturn(Optional.of(service));

        bookingService.addServiceToBooking(1, request, "staff@hotel.com");

        verify(bookingServiceRepository).save(any());
        verify(bookingRepository).save(booking);
    }

    // ==========================================
    // 10. checkFaceReadiness & eKYC Mapping (UTCID33 - UTCID35, UTCID82 - UTCID85)
    // ==========================================

    @Test
    @DisplayName("UTCID82 - Throw exception when selfieImage content type is invalid")
    void should_throwException_when_checkFaceReadinessWithInvalidImageContentType() {
        MockMultipartFile pdfFile = new MockMultipartFile("selfieImage", "document.pdf", "application/pdf", new byte[]{1, 2});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(pdfFile, "manager@hotel.com"));
        assertEquals("Ảnh khuôn mặt chỉ hỗ trợ JPG, PNG hoặc WebP", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID85 - Successfully map eKYC profile fields with AES decryption and ID masking")
    void should_mapEkycIdentitySummary_withDecryptedAndMaskedFields() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        EkycProfile profile = new EkycProfile();
        profile.setStatus("Verified");
        profile.setIdCardNumber("enc_123456789012");
        profile.setFullName("enc_John Doe");

        when(bookingDetailRepository.findBookingDetail(1, "john@example.com")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(customerUser)).thenReturn(Optional.of(profile));
        when(aesEncryptionService.decrypt("enc_123456789012")).thenReturn("123456789012");
        when(aesEncryptionService.decrypt("enc_John Doe")).thenReturn("John Doe");

        BookingResponse response = bookingService.getBookingDetail(1, "john@example.com");

        assertNotNull(response);
        assertNotNull(response.getEkycIdentity());
        assertEquals("12*******012", response.getEkycIdentity().getIdNumber());
    }

    // ==========================================
    // Additional Branch Coverage Tests
    // ==========================================

    @Test
    @DisplayName("Create booking with null check-in method defaults to Manual")
    void should_createBookingSuccessfully_when_checkInMethodIsNull() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(LocalDate.now(HOTEL_ZONE).plusDays(1));
        request.setCheckOutDate(LocalDate.now(HOTEL_ZONE).plusDays(3));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(0);
        request.setCheckInMethod(null);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(roomTypeRepository.findByIdForUpdate(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, "john@example.com");

        assertNotNull(response);
        assertEquals("Manual", response.getCheckInMethod());
    }

    @Test
    @DisplayName("Create booking with FaceID throws exception when eKYC is verified but face embedding is missing")
    void should_throwException_when_createBookingFaceIdWithEkycVerifiedButNoFaceEmbedding() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(LocalDate.now(HOTEL_ZONE).plusDays(1));
        request.setCheckOutDate(LocalDate.now(HOTEL_ZONE).plusDays(3));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(0);
        request.setCheckInMethod("FaceID");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(customerUser.getId())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertTrue(ex.getMessage().contains("hoàn thành đăng ký eKYC"));
    }

    @Test
    @DisplayName("Validate guest counts throws exception when adults count is 0 or negative")
    void should_throwException_when_numberOfAdultsIsZeroOrNegative() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(LocalDate.now(HOTEL_ZONE).plusDays(1));
        request.setCheckOutDate(LocalDate.now(HOTEL_ZONE).plusDays(3));
        request.setNumberOfAdults(0);
        request.setNumberOfChildren(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Số người lớn phải lớn hơn 0", ex.getMessage());
    }

    @Test
    @DisplayName("Validate guest counts throws exception when children count is negative")
    void should_throwException_when_numberOfChildrenIsNegative() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomTypeId(1);
        request.setCheckInDate(LocalDate.now(HOTEL_ZONE).plusDays(1));
        request.setCheckOutDate(LocalDate.now(HOTEL_ZONE).plusDays(3));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(-1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, "john@example.com"));
        assertEquals("Số trẻ em không được âm", ex.getMessage());
    }

    @Test
    @DisplayName("Cancel booking by Customer > 48 hours before check-in gives 100% refund")
    void should_cancelBooking_byCustomerMoreThan48Hours_shouldRefund100Percent() {
        LocalDate checkInDate = LocalDate.now(HOTEL_ZONE).plusDays(5);
        Booking booking = new Booking();
        booking.setId(10);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);
        booking.setPaidAmount(new BigDecimal("1000000.00"));
        booking.setFinalAmount(new BigDecimal("1000000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setExpectedCheckIn(toInstant(checkInDate));
        detail.setExpectedCheckOut(toInstant(checkInDate.plusDays(2)));

        CancelBookingRequest cancelReq = new CancelBookingRequest();
        cancelReq.setCancellationReason("Change of plans for vacation");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(10)).thenReturn(Optional.of(detail));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(10, cancelReq, "john@example.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    @DisplayName("Cancel booking by Customer between 24 and 48 hours before check-in gives 50% refund")
    void should_cancelBooking_byCustomerBetween24And48Hours_shouldRefund50Percent() {
        LocalDate checkInDate = LocalDate.now(HOTEL_ZONE).plusDays(2);
        Booking booking = new Booking();
        booking.setId(11);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);
        booking.setPaidAmount(new BigDecimal("1000000.00"));
        booking.setFinalAmount(new BigDecimal("1000000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setExpectedCheckIn(toInstant(checkInDate));
        detail.setExpectedCheckOut(toInstant(checkInDate.plusDays(2)));

        CancelBookingRequest cancelReq = new CancelBookingRequest();
        cancelReq.setCancellationReason("Emergency reason for cancelling");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingRepository.findById(11)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(11)).thenReturn(Optional.of(detail));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(11, cancelReq, "john@example.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    @DisplayName("Cancel booking throws exception if booking status is already CANCELLED")
    void should_throwException_when_cancelBookingWithAlreadyCancelledStatus() {
        Booking booking = new Booking();
        booking.setId(12);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUser(customerUser);

        CancelBookingRequest cancelReq = new CancelBookingRequest();
        cancelReq.setCancellationReason("Test cancellation reason");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingRepository.findById(12)).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking(12, cancelReq, "john@example.com"));
        assertEquals("Đơn đặt phòng này đã được hủy trước đó", ex.getMessage());
    }

    @Test
    @DisplayName("Get bookings with full filter parameters covers all specification branches")
    void should_getBookings_withFullFilterParameters_coversSpecificationBranches() {
        BookingFilter filter = new BookingFilter();
        filter.setBookingReference("BK2026");
        filter.setStatus(BookingStatus.CONFIRMED);
        filter.setBookingType("Online");
        filter.setCheckInDateFrom(LocalDate.now(HOTEL_ZONE));
        filter.setCheckInDateTo(LocalDate.now(HOTEL_ZONE).plusDays(7));
        filter.setCheckOutDateFrom(LocalDate.now(HOTEL_ZONE).plusDays(1));
        filter.setCheckOutDateTo(LocalDate.now(HOTEL_ZONE).plusDays(8));
        filter.setPage(0);
        filter.setPageSize(10);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);

        when(bookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        PageResponse<BookingHistoryResponse> response = bookingService.filterBookings(filter);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("Filter bookings with DESC sort direction and null page/pageSize defaults")
    void should_filterBookingsSuccessfully_when_sortDirectionDESCAndNullDefaults() {
        BookingFilter filter = new BookingFilter();
        filter.setSortDirection("DESC");
        filter.setPage(null);
        filter.setPageSize(null);
        filter.setSortBy(null);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK_FILTER_DESC");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE)));
        detail.setExpectedCheckOut(toInstant(LocalDate.now(HOTEL_ZONE).plusDays(1)));

        BookingRoomAccess access = new BookingRoomAccess();
        access.setBooking(booking);
        access.setRoom(defaultRoom);

        when(bookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingDetailRepository.findByBooking_IdIn(List.of(1))).thenReturn(List.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdIn(List.of(1))).thenReturn(List.of(access));

        PageResponse<BookingHistoryResponse> response = bookingService.filterBookings(filter);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("BK_FILTER_DESC", response.getContent().get(0).getBookingNumber());
    }

    @Test
    @DisplayName("Filter bookings returns empty PageResponse when no bookings match criteria")
    void should_filterBookingsSuccessfully_when_emptyResult() {
        BookingFilter filter = new BookingFilter();
        filter.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(bookingDetailRepository.findByBooking_IdIn(List.of())).thenReturn(List.of());
        when(bookingRoomAccessRepository.findByBooking_IdIn(List.of())).thenReturn(List.of());

        PageResponse<BookingHistoryResponse> response = bookingService.filterBookings(filter);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    @DisplayName("Check face readiness with empty file throws exception")
    void should_throwException_when_checkFaceReadinessWithEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("selfieImage", "empty.jpg", "image/jpeg", new byte[0]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(emptyFile, "manager@hotel.com"));
        assertEquals("Vui lòng chụp hoặc tải lên ảnh khuôn mặt", ex.getMessage());
    }

    @Test
    @DisplayName("Perform face check-in throws exception when liveness check fails")
    void should_throwException_when_performFaceCheckInLivenessFails() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(1)).thenReturn(Optional.of("embedding_data"));

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceVerificationResponse aiResponse = new AiFaceVerificationResponse();
        aiResponse.setLivenessPassed(false); // Liveness failed!
        aiResponse.setMessage("Thất bại xác thực khuôn mặt");
        aiResponse.setVerified(false);

        when(responseSpec.bodyToMono(AiFaceVerificationResponse.class)).thenReturn(Mono.just(aiResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertTrue(ex.getMessage().contains("Thất bại xác thực khuôn mặt"));
    }

    @Test
    @DisplayName("Perform face check-in throws exception when face verification mismatch occurs")
    void should_throwException_when_performFaceCheckInFaceMismatch() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(faceEmbeddingRepository.findEmbeddingTextByUserId(1)).thenReturn(Optional.of("embedding_data"));

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceVerificationResponse aiResponse = new AiFaceVerificationResponse();
        aiResponse.setLivenessPassed(true);
        aiResponse.setVerified(false); // Mismatch!
        aiResponse.setMatched(false);

        when(responseSpec.bodyToMono(AiFaceVerificationResponse.class)).thenReturn(Mono.just(aiResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertTrue(ex.getMessage().contains("khuôn mặt hiện tại không khớp"));
    }

    @Test
    @DisplayName("Perform face check-in throws exception when actor is non-manager staff")
    void should_throwException_when_performFaceCheckInNonManagerRole() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser)); // Receptionist role

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "staff@hotel.com"));
        assertEquals("Chỉ Manager mới được check-in bằng FaceID", ex.getMessage());
    }

    @Test
    @DisplayName("Perform face check-in throws exception when booking check-in method is not FaceID")
    void should_throwException_when_performFaceCheckInWrongCheckInMethod() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("Manual"); // Not FaceID!
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertEquals("Đơn đặt phòng này không sử dụng phương thức FaceID", ex.getMessage());
    }

    @Test
    @DisplayName("Perform face check-in throws exception when customer eKYC is not verified")
    void should_throwException_when_performFaceCheckInEkycNotVerified() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("FaceID");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performFaceCheckIn(1, selfie, selfie, selfie, selfie, "left", "manager@hotel.com"));
        assertEquals("Bạn chưa hoàn thành đăng ký eKYC nên chưa thể check-in bằng FaceID", ex.getMessage());
    }

    @Test
    @DisplayName("Get all bookings for staff successfully when actor is Manager")
    void should_getAllBookingsForStaff_when_actorIsManager() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK100");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(Instant.now());

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(2);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("BK100", response.get(0).getBookingNumber());
    }

    @Test
    @DisplayName("Get all bookings for staff successfully when actor is Receptionist")
    void should_getAllBookingsForStaff_when_actorIsReceptionist() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK101");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(1);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("staff@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("BK101", response.get(0).getBookingNumber());
    }

    @Test
    @DisplayName("Get all bookings for staff throws exception when actor is Customer")
    void should_throwException_when_getAllBookingsForStaffWithCustomerRole() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.getAllBookingsForStaff("customer@example.com"));
        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("Get all bookings for staff throws exception when user is not found")
    void should_throwException_when_getAllBookingsForStaffWithUserNotFound() {
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.getAllBookingsForStaff("unknown@hotel.com"));
        assertEquals("Không tìm thấy người dùng", ex.getMessage());
    }

    @Test
    @DisplayName("IsRoomAccessUsable returns true when booking is CHECKED_IN and key is Active and not expired")
    void should_includeRoomPassword_when_isRoomAccessUsableIsTrue() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Instant now = Instant.now();

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK200");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCreatedAt(now);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(2);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        BookingRoomAccess access = new BookingRoomAccess();
        access.setBooking(booking);
        access.setRoom(defaultRoom);
        access.setRoomKeyAccess("KEY123");
        access.setRoomKeyStatus("Active");
        access.setRoomKeyExpiredAt(now.plusSeconds(3600));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(access));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getRoomAccesses());
        assertEquals("KEY123", response.get(0).getRoomAccesses().get(0).getRoomPassword());
    }

    @Test
    @DisplayName("IsRoomAccessUsable returns false when room key is Expired")
    void should_maskRoomPassword_when_isRoomAccessUsableIsFalse() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Instant now = Instant.now();

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK201");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCreatedAt(now);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(2);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        BookingRoomAccess access = new BookingRoomAccess();
        access.setBooking(booking);
        access.setRoom(defaultRoom);
        access.setRoomKeyAccess("KEY123");
        access.setRoomKeyStatus("Expired"); // Expired!
        access.setRoomKeyExpiredAt(now.minusSeconds(100));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of(access));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getRoomAccesses());
        assertNull(response.get(0).getRoomAccesses().get(0).getRoomPassword());
    }

    @Test
    @DisplayName("IsFaceIdMethod returns true when checkInMethod is Face Recognition")
    void should_includeEkycIdentity_when_isFaceIdMethodIsTrue() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Instant now = Instant.now();

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK300");
        booking.setUser(customerUser);
        booking.setCheckInMethod("Face Recognition"); // isFaceIdMethod == true
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        EkycProfile profile = new EkycProfile();
        profile.setId(10);
        profile.setUser(customerUser);
        profile.setStatus("VERIFIED");

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(customerUser)).thenReturn(Optional.of(profile));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getEkycIdentity());
    }

    @Test
    @DisplayName("SignedUrlOrNull returns signed URL when Supabase storage service succeeds")
    void should_returnSignedUrl_when_signedUrlOrNullSuccessful() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK302");
        booking.setUser(customerUser);
        booking.setCheckInMethod("FaceID");
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        EkycProfile profile = new EkycProfile();
        profile.setId(10);
        profile.setUser(customerUser);
        profile.setStatus("VERIFIED");
        profile.setFrontImage("ekyc/front.jpg");

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(customerUser)).thenReturn(Optional.of(profile));
        when(supabaseStorageService.getSignedUrl("ekyc/front.jpg")).thenReturn("https://supabase.co/signed/front.jpg");

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getEkycIdentity());
        assertEquals("https://supabase.co/signed/front.jpg", response.get(0).getEkycIdentity().getFrontImage());
    }

    @Test
    @DisplayName("SignedUrlOrNull returns null when Supabase storage service throws exception")
    void should_returnNull_when_signedUrlOrNullThrowsException() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK303");
        booking.setUser(customerUser);
        booking.setCheckInMethod("FaceID");
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        EkycProfile profile = new EkycProfile();
        profile.setId(10);
        profile.setUser(customerUser);
        profile.setStatus("VERIFIED");
        profile.setFrontImage("ekyc/corrupted.jpg");

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(customerUser)).thenReturn(Optional.of(profile));
        when(supabaseStorageService.getSignedUrl("ekyc/corrupted.jpg")).thenThrow(new RuntimeException("Storage unavailable"));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getEkycIdentity());
        assertNull(response.get(0).getEkycIdentity().getFrontImage());
    }

    @Test
    @DisplayName("IsFaceIdMethod returns false when checkInMethod is Manual")
    void should_excludeEkycIdentity_when_isFaceIdMethodIsFalse() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK301");
        booking.setUser(customerUser);
        booking.setCheckInMethod("Manual"); // isFaceIdMethod == false
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));

        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff("manager@hotel.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertNull(response.get(0).getEkycIdentity());
    }

    // ==========================================
    // Additional Edge-Case & Full Coverage Tests
    // ==========================================

    @Test
    @DisplayName("Create walk-in booking throws exception when actor is not staff")
    void should_throwException_when_createWalkInBookingWithNonStaffRole() {
        WalkInBookingRequest request = new WalkInBookingRequest();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createWalkInBooking(request, "john@example.com"));
        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("Create walk-in booking throws exception when room type is inactive")
    void should_throwException_when_createWalkInBookingWithInactiveRoomType() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        WalkInBookingRequest request = new WalkInBookingRequest();
        request.setCustomerEmail("newguest@example.com");
        request.setCustomerFullname("New Guest");
        request.setCustomerPhonenumber("0912345678");
        request.setCustomerIdCardNumber("111222333444");
        request.setRoomTypeId(1);
        request.setQuantity(1);
        request.setCheckInDate(today);
        request.setCheckOutDate(today.plusDays(1));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);

        RoomType inactiveRoomType = new RoomType();
        inactiveRoomType.setId(1);
        inactiveRoomType.setStatus("Inactive");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(userRepository.findByEmail("newguest@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByIdCardNumber("111222333444")).thenReturn(false);
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(inactiveRoomType));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createWalkInBooking(request, "staff@hotel.com"));
        assertEquals("Loại phòng hiện không hoạt động", ex.getMessage());
    }

    @Test
    @DisplayName("Create walk-in booking sets PARTIALLY_PAID status when paid amount is less than final amount")
    void should_createWalkInBookingWithPartiallyPaidStatus_when_paidAmountLessThanFinalAmount() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        WalkInBookingRequest request = new WalkInBookingRequest();
        request.setCustomerEmail("partial@example.com");
        request.setCustomerFullname("Partial Guest");
        request.setCustomerPhonenumber("0911111111");
        request.setCustomerIdCardNumber("123123123123");
        request.setRoomTypeId(1);
        request.setQuantity(1);
        request.setCheckInDate(today);
        request.setCheckOutDate(today.plusDays(1));
        request.setNumberOfAdults(1);
        request.setNumberOfChildren(0);
        request.setPaidAmount(new BigDecimal("500000.00")); // Final amount is 1,100,000

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(userRepository.findByEmail("partial@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByIdCardNumber("123123123123")).thenReturn(false);
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(defaultRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(1, "Available")).thenReturn(5L);

        User newCustomer = new User();
        newCustomer.setId(6);
        newCustomer.setEmail("partial@example.com");
        newCustomer.setRole(Role.customer);

        when(userRepository.save(any(User.class))).thenReturn(newCustomer);

        Booking booking = new Booking();
        booking.setId(31);
        booking.setBookingType("Walk-in");
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setFinalAmount(new BigDecimal("1100000.00"));
        booking.setUser(newCustomer);

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of(defaultRoom));

        BookingResponse response = bookingService.createWalkInBooking(request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
    }

    @Test
    @DisplayName("Update booking throws exception when booking is already COMPLETED")
    void should_throwException_when_updateBookingWithCompletedStatus() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.COMPLETED);

        UpdateBookingRequest request = new UpdateBookingRequest();

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(1, request, "staff@hotel.com"));
        assertEquals("Không thể cập nhật đơn đặt phòng đã hủy hoặc đã hoàn thành", ex.getMessage());
    }

    @Test
    @DisplayName("Update booking throws exception when new check-in date is after check-out date")
    void should_throwException_when_updateBookingWithInvalidCheckInCheckOutDates() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setCheckInDate(today.plusDays(5).toString());
        request.setCheckOutDate(today.plusDays(3).toString()); // Check-in after Check-out!

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(1, request, "staff@hotel.com"));
        assertEquals("Ngày nhận phòng phải trước ngày trả phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Update booking successfully when changing room type, dates, quantity, adults, children, discount and special requests")
    void should_updateBookingSuccessfully_when_allFieldsProvided() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate newCheckIn = today.plusDays(1);
        LocalDate newCheckOut = today.plusDays(3);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1100000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(1);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));
        detail.setPriceAtBooking(new BigDecimal("500000.00"));

        RoomType newRoomType = new RoomType();
        newRoomType.setId(2);
        newRoomType.setName("Executive Suite");
        newRoomType.setBasePrice(new BigDecimal("800000.00"));
        newRoomType.setStatus("Active");
        newRoomType.setAdultCapacity(4);
        newRoomType.setChildCapacity(2);

        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setCheckInDate(newCheckIn.toString());
        request.setCheckOutDate(newCheckOut.toString());
        request.setRoomTypeId(2);
        request.setQuantity(2);
        request.setNumberOfAdults(3);
        request.setNumberOfChildren(1);
        request.setSpecialRequests("High floor, quiet room");
        request.setDiscountAmount(new BigDecimal("100000.00"));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(detail));
        when(roomTypeRepository.findByIdForUpdate(2)).thenReturn(Optional.of(newRoomType));
        when(roomRepository.countByRoomType_IdAndStatus(eq(2), anyString())).thenReturn(10L);
        when(bookingDetailRepository.sumBookedQuantity(eq(2), any(), any(), anyList(), anyList())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingDetailRepository.save(any(BookingDetail.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.updateBooking(1, request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("High floor, quiet room", booking.getSpecialRequests());
        assertEquals(new BigDecimal("100000.00"), booking.getDiscountAmount());
        assertEquals(2, detail.getQuantity());
        assertEquals(3, detail.getNumberOfAdults());
        assertEquals(1, detail.getNumberOfChildren());
    }

    @Test
    @DisplayName("Update booking successfully when only updating special requests and discount amount")
    void should_updateBookingSuccessfully_when_noDatesOrRoomTypeChanged() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(customerUser);
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setNumberOfAdults(2);
        detail.setNumberOfChildren(0);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));
        detail.setPriceAtBooking(new BigDecimal("500000.00"));

        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setSpecialRequests("Late check-in requested");
        request.setDiscountAmount(new BigDecimal("50000.00"));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(detail));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingDetailRepository.save(any(BookingDetail.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.updateBooking(1, request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Late check-in requested", booking.getSpecialRequests());
        assertEquals(new BigDecimal("50000.00"), booking.getDiscountAmount());
    }

    @Test
    @DisplayName("Update booking throws exception when new room type is not found")
    void should_throwException_when_updateBookingWithNotFoundRoomType() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setRoomTypeId(999);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.of(detail));
        when(roomTypeRepository.findByIdForUpdate(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(1, request, "staff@hotel.com"));
        assertEquals("Không tìm thấy loại phòng mới", ex.getMessage());
    }

    @Test
    @DisplayName("Update booking throws exception when booking ID is not found")
    void should_throwException_when_updateBookingWithBookingNotFound() {
        UpdateBookingRequest request = new UpdateBookingRequest();
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(999, request, "staff@hotel.com"));
        assertEquals("Không tìm thấy đơn đặt phòng với ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Update booking throws exception when booking detail is not found")
    void should_throwException_when_updateBookingDetailNotFound() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);

        UpdateBookingRequest request = new UpdateBookingRequest();

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(1, request, "staff@hotel.com"));
        assertEquals("Không tìm thấy chi tiết đặt phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Add service to booking throws exception when actor is not staff")
    void should_throwException_when_addServiceToBookingWithNonStaffRole() {
        AddServiceRequest request = new AddServiceRequest();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.addServiceToBooking(1, request, "john@example.com"));
        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("Add service to booking throws exception when booking is CANCELLED")
    void should_throwException_when_addServiceToBookingWithCancelledStatus() {
        AddServiceRequest request = new AddServiceRequest();
        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(1);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(cancelledBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.addServiceToBooking(1, request, "staff@hotel.com"));
        assertEquals("Không thể thêm dịch vụ cho đơn đặt phòng ở trạng thái này", ex.getMessage());
    }

    @Test
    @DisplayName("Add service to booking throws exception when service is inactive")
    void should_throwException_when_addServiceToBookingWithInactiveService() {
        AddServiceRequest request = new AddServiceRequest();
        request.setServiceId(2);
        request.setQuantity(1);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);

        com.example.hotelsmartbookingbackend.entity.Service inactiveService = new com.example.hotelsmartbookingbackend.entity.Service();
        inactiveService.setId(2);
        inactiveService.setIsActive(false);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(serviceRepository.findById(2)).thenReturn(Optional.of(inactiveService));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.addServiceToBooking(1, request, "staff@hotel.com"));
        assertEquals("Dịch vụ hiện không hoạt động", ex.getMessage());
    }

    @Test
    @DisplayName("Add service to paid booking updates status to PARTIALLY_PAID")
    void should_addServiceToBooking_andChangeStatusToPartiallyPaid_when_bookingWasPaid() {
        AddServiceRequest request = new AddServiceRequest();
        request.setServiceId(2);
        request.setQuantity(1);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.PAID);
        booking.setServiceChargeAmount(BigDecimal.ZERO);
        booking.setTaxAmount(new BigDecimal("100.00"));
        booking.setFinalAmount(new BigDecimal("1100.00"));
        booking.setPaidAmount(new BigDecimal("1100.00")); // Paid in full

        com.example.hotelsmartbookingbackend.entity.Service service = new com.example.hotelsmartbookingbackend.entity.Service();
        service.setId(2);
        service.setName("Mini Bar");
        service.setIsActive(true);
        service.setPrice(new BigDecimal("100000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(serviceRepository.findById(2)).thenReturn(Optional.of(service));

        bookingService.addServiceToBooking(1, request, "staff@hotel.com");

        assertEquals(BookingStatus.PARTIALLY_PAID, booking.getStatus());
    }

    @Test
    @DisplayName("Get invoice details throws exception when customer is not booking owner")
    void should_throwException_when_getInvoiceDetailsWithNonOwnerCustomer() {
        User otherCustomer = new User();
        otherCustomer.setId(99);
        otherCustomer.setEmail("other@example.com");
        otherCustomer.setRole(Role.customer);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setUser(customerUser); // ID = 1 (different from otherCustomer ID = 99)

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.getInvoiceDetails(1, "other@example.com"));
        assertEquals("Bạn không có quyền xem hóa đơn này", ex.getMessage());
    }

    @Test
    @DisplayName("Staff cancel booking throws exception when booking is COMPLETED")
    void should_throwException_when_staffCancelBookingWithCompletedStatus() {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Customer requested");

        Booking completedBooking = new Booking();
        completedBooking.setId(1);
        completedBooking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(completedBooking));
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking(1, request, "staff@hotel.com"));
        assertEquals("Không thể hủy đơn đặt phòng đã trả phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Customer cancel booking throws exception when user is not booking owner")
    void should_throwException_when_customerCancelBookingWithNonOwnerUser() {
        CancelBookingRequest request = new CancelBookingRequest();

        User otherCustomer = new User();
        otherCustomer.setId(88);
        otherCustomer.setEmail("stranger@example.com");

        Booking booking = new Booking();
        booking.setId(1);
        booking.setUser(customerUser); // ID = 1 (different from stranger ID = 88)

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(otherCustomer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.customerCancelBooking(1, request, "stranger@example.com"));
        assertEquals("Bạn không có quyền hủy đơn đặt phòng này", ex.getMessage());
    }

    @Test
    @DisplayName("Customer cancel booking throws exception when booking is CHECKED_IN")
    void should_throwException_when_customerCancelBookingWithCheckedInStatus() {
        CancelBookingRequest request = new CancelBookingRequest();

        Booking checkedInBooking = new Booking();
        checkedInBooking.setId(1);
        checkedInBooking.setUser(customerUser);
        checkedInBooking.setStatus(BookingStatus.CHECKED_IN);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(checkedInBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.customerCancelBooking(1, request, "customer@example.com"));
        assertTrue(ex.getMessage().startsWith("Không thể hủy đơn đặt phòng ở trạng thái:"));
    }

    @Test
    @DisplayName("Customer cancel booking successfully with explicit cancellation reason")
    void should_customerCancelBookingSuccessfully_when_validRequestWithExplicitReason() {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Personal reasons");

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK123456");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setStatus("Active");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingDetailRepository.save(any(BookingDetail.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.customerCancelBooking(1, request, "customer@example.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Personal reasons", booking.getCancellationReason());
        assertEquals("Cancelled", detail.getStatus());
        assertEquals(customerUser, booking.getCancelledBy());
    }

    @Test
    @DisplayName("Customer cancel booking successfully with default reason when request or reason is null")
    void should_customerCancelBookingSuccessfully_when_requestOrReasonIsNull() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK123456");
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setStatus("Active");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.customerCancelBooking(1, null, "customer@example.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Khách hàng tự hủy trực tuyến", booking.getCancellationReason());
    }

    @Test
    @DisplayName("Customer cancel booking throws exception when booking ID is not found")
    void should_throwException_when_customerCancelBookingWithBookingNotFound() {
        CancelBookingRequest request = new CancelBookingRequest();
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.customerCancelBooking(999, request, "customer@example.com"));
        assertEquals("Không tìm thấy đơn đặt phòng với ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Customer cancel booking throws exception when customer user is not found")
    void should_throwException_when_customerCancelBookingWithCustomerNotFound() {
        CancelBookingRequest request = new CancelBookingRequest();
        Booking booking = new Booking();
        booking.setId(1);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.customerCancelBooking(1, request, "unknown@example.com"));
        assertEquals("Không tìm thấy khách hàng", ex.getMessage());
    }

    @Test
    @DisplayName("Customer cancel booking throws exception when booking detail is not found")
    void should_throwException_when_customerCancelBookingWithDetailNotFound() {
        CancelBookingRequest request = new CancelBookingRequest();
        Booking booking = new Booking();
        booking.setId(1);
        booking.setUser(customerUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.customerCancelBooking(1, request, "customer@example.com"));
        assertEquals("Không tìm thấy chi tiết đơn đặt phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token reuses existing active token when not expired")
    void should_returnExistingQrToken_when_activeTokenAlreadyExists() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Instant now = Instant.now();

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));
        detail.setQrCodeValue("ACTIVE_QR_TOKEN_999");
        detail.setQrCodeGeneratedAt(now.minusSeconds(60));
        detail.setQrCodeExpiredAt(now.plusSeconds(3600)); // Active for 1 hour

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);

        QrTokenResponse response = bookingService.generateQrCheckInToken(1, "customer@example.com");

        assertNotNull(response);
        assertEquals("ACTIVE_QR_TOKEN_999", response.getToken());
    }

    @Test
    @DisplayName("Perform QR check-in throws exception when token is expired")
    void should_throwException_when_performQrCheckInWithExpiredToken() {
        Instant now = Instant.now();
        BookingDetail detail = new BookingDetail();
        detail.setQrCodeValue("EXPIRED_QR_TOKEN");
        detail.setQrCodeExpiredAt(now.minusSeconds(10)); // Expired 10 seconds ago

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingDetailRepository.findByQrCodeValueForUpdate("EXPIRED_QR_TOKEN")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performQrCheckIn("EXPIRED_QR_TOKEN", "staff@hotel.com"));
        assertEquals("Ma QR check-in da het hieu luc", ex.getMessage());
    }

    @Test
    @DisplayName("Perform QR check-in throws exception when QR token is blank or null")
    void should_throwException_when_performQrCheckInWithNullOrBlankToken() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performQrCheckIn("  ", "staff@hotel.com"));
        assertEquals("Ma QR check-in khong hop le", ex.getMessage());
    }

    @Test
    @DisplayName("Perform QR check-in throws exception when actor is an unauthorized stranger customer")
    void should_throwException_when_performQrCheckInWithUnauthorizedActor() {
        Instant now = Instant.now();
        User stranger = new User();
        stranger.setId(99);
        stranger.setEmail("stranger@example.com");
        stranger.setRole(Role.customer);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser); // ID = 1 (different from stranger ID = 99)

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(LocalDate.now(HOTEL_ZONE).minusDays(1)));
        detail.setExpectedCheckOut(toInstant(LocalDate.now(HOTEL_ZONE).plusDays(1)));
        detail.setQrCodeValue("VALID_QR_TOKEN");
        detail.setQrCodeExpiredAt(now.plusSeconds(3600));

        when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));
        when(bookingDetailRepository.findByQrCodeValueForUpdate("VALID_QR_TOKEN")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.performQrCheckIn("VALID_QR_TOKEN", "stranger@example.com"));
        assertEquals("Ban khong co quyen check-in booking nay bang QR Code", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token throws exception when customer eKYC is not verified")
    void should_throwException_when_generateQrTokenWithUnverifiedEkyc() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
        assertEquals("Ban phai hoan thanh dang ky eKYC truoc khi chon check-in bang QR Code", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token throws exception when booking is already checked-in")
    void should_throwException_when_generateQrTokenWithAlreadyCheckedInBooking() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));
        detail.setActualCheckIn(Instant.now()); // Already checked in!

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
        assertEquals("Booking nay da duoc check-in", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token throws exception when booking status is not eligible for check-in")
    void should_throwException_when_generateQrTokenWithIneligibleBookingStatus() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CANCELLED); // CANCELLED status is not eligible!
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
        assertEquals("Don dat phong khong o trang thai co the nhan phong", ex.getMessage());
    }

    @Test
    @DisplayName("Perform QR check-in successfully when actor is the booking owner customer")
    void should_performQrCheckInSuccessfully_when_actorIsBookingOwnerCustomer() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setBookingReference("BK_QR_001");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setExpectedCheckIn(toInstant(today.minusDays(1)));
        detail.setExpectedCheckOut(toInstant(today.plusDays(1)));
        detail.setQrCodeValue("VALID_QR_TOKEN_OWNER");
        detail.setQrCodeExpiredAt(now.plusSeconds(3600));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByQrCodeValueForUpdate("VALID_QR_TOKEN_OWNER")).thenReturn(Optional.of(detail));
        when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());
        when(roomRepository.findByRoomType_IdAndStatusOrderByRoomNumberAsc(1, "Available")).thenReturn(List.of(defaultRoom));

        BookingResponse response = bookingService.performQrCheckIn("VALID_QR_TOKEN_OWNER", "john@example.com");

        assertNotNull(response);
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertNotNull(detail.getActualCheckIn());
    }

    @Test
    @DisplayName("Perform check-out applies 50% penalty when late between 3 and 6 hours")
    void should_performCheckOutWith50PercentPenalty_when_lateBetween3And6Hours() {
        Instant now = Instant.now();
        Instant expectedCheckOut = now.minus(Duration.ofHours(4)); // Late by 4 hours (between 3 and 6h)

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUser(customerUser);
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setPaidAmount(new BigDecimal("1650000.00")); // Paid in full including late penalty (1.5M + 150k tax)
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setFinalAmount(new BigDecimal("1100000.00"));

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setQuantity(1);
        detail.setPriceAtBooking(new BigDecimal("1000000.00")); // 1 night = 1,000,000 -> 50% = 500,000
        detail.setExpectedCheckIn(now.minus(Duration.ofDays(1)));
        detail.setExpectedCheckOut(expectedCheckOut);
        detail.setRoom(defaultRoom);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(detail));
        when(bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(1)).thenReturn(List.of());

        BookingResponse response = bookingService.performCheckOut(1, "staff@hotel.com");

        assertNotNull(response);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getSpecialRequests().contains("50% (Trễ từ 3-6 giờ)"));
    }

    @Test
    @DisplayName("Check face readiness throws exception when actor is not manager")
    void should_throwException_when_checkFaceReadinessWithNonManagerRole() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "s.jpg", "image/jpeg", new byte[]{1, 2});
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(staffUser)); // Receptionist role

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(selfie, "staff@hotel.com"));
        assertEquals("Chỉ Manager mới được kiểm tra camera FaceID", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token throws exception when check-in date is in future")
    void should_throwException_when_generateQrTokenWithFutureCheckInDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.plusDays(2))); // 2 days in future
        detail.setExpectedCheckOut(toInstant(today.plusDays(4)));

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
        assertEquals("Chưa đến ngày nhận phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Generate QR token throws exception when check-out date has passed")
    void should_throwException_when_generateQrTokenWithExpiredCheckOutDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today.minusDays(5)));
        detail.setExpectedCheckOut(toInstant(today.minusDays(1))); // Check-out was yesterday

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
        assertEquals("Đơn đặt phòng đã quá thời gian nhận phòng", ex.getMessage());
    }

    @Test
    @DisplayName("Validate check-in date window on check-in date")
    void should_handleSameDayCheckInHourWindow_when_generateQrTokenOnCheckInDate() {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalTime nowTime = LocalTime.now(HOTEL_ZONE);

        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCheckInMethod("QR");
        booking.setUser(customerUser);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setRoomType(defaultRoomType);
        detail.setExpectedCheckIn(toInstant(today));
        detail.setExpectedCheckOut(toInstant(today.plusDays(2)));

        when(bookingDetailRepository.findBookingDetail(1, "customer@example.com")).thenReturn(Optional.of(detail));

        if (nowTime.isBefore(LocalTime.of(14, 0))) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> bookingService.generateQrCheckInToken(1, "customer@example.com"));
            assertEquals("Chưa đến giờ nhận phòng tiêu chuẩn (từ 14:00)", ex.getMessage());
        } else {
            when(ekycProfileRepository.existsVerifiedByUserid(customerUser)).thenReturn(true);
            when(bookingDetailRepository.existsByQrCodeValue(anyString())).thenReturn(false);
            QrTokenResponse response = bookingService.generateQrCheckInToken(1, "customer@example.com");
            assertNotNull(response);
        }
    }

    @Test
    @DisplayName("Check face readiness throws exception when image format is invalid")
    void should_throwException_when_checkFaceReadinessWithInvalidImageFormat() {
        MockMultipartFile pdfFile = new MockMultipartFile("selfieImage", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(pdfFile, "manager@hotel.com"));
        assertEquals("Ảnh khuôn mặt chỉ hỗ trợ JPG, PNG hoặc WebP", ex.getMessage());
    }

    @Test
    @DisplayName("Check face readiness successfully when valid selfie image and Manager role")
    void should_checkFaceReadinessSuccessfully_when_validSelfieAndManagerRole() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceReadinessResponse readinessResponse = new AiFaceReadinessResponse();
        readinessResponse.setReady(true);
        readinessResponse.setReason("Khuôn mặt sẵn sàng");

        when(responseSpec.bodyToMono(AiFaceReadinessResponse.class)).thenReturn(Mono.just(readinessResponse));

        AiFaceReadinessResponse result = bookingService.checkFaceReadiness(selfie, "manager@hotel.com");

        assertNotNull(result);
        assertTrue(result.getReady());
        assertEquals("Khuôn mặt sẵn sàng", result.getReason());
    }

    @Test
    @DisplayName("Check face readiness throws exception when WebClient returns null response")
    void should_throwException_when_checkFaceReadinessWebClientReturnsNull() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AiFaceReadinessResponse.class)).thenReturn(Mono.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(selfie, "manager@hotel.com"));
        assertEquals("Face readiness API trả về phản hồi rỗng", ex.getMessage());
    }

    @Test
    @DisplayName("Check face readiness throws exception when WebClient throws WebClientResponseException")
    void should_throwException_when_checkFaceReadinessWebClientThrowsResponseException() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new WebClientResponseException(500, "Internal Error", HttpHeaders.EMPTY, "AI service crash".getBytes(), null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(selfie, "manager@hotel.com"));
        assertTrue(ex.getMessage().startsWith("Face readiness API lỗi:"));
    }

    @Test
    @DisplayName("Check face readiness throws exception when actor user is not found")
    void should_throwException_when_checkFaceReadinessUserNotFound() {
        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.checkFaceReadiness(selfie, "unknown@hotel.com"));
        assertEquals("Không tìm thấy người dùng", ex.getMessage());
    }
}





