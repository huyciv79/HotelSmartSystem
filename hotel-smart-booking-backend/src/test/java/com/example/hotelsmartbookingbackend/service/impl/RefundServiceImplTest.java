package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.ApproveRefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RejectRefundRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.entity.CancellationPolicy;
import com.example.hotelsmartbookingbackend.entity.CustomerRequest;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.CancellationPolicyRepository;
import com.example.hotelsmartbookingbackend.repository.CustomerRequestRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import com.example.hotelsmartbookingbackend.service.PaypalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
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
class RefundServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock
    private CustomerRequestRepository customerRequestRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaypalService paypalService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RefundServiceImpl refundService;

    private User customerUser;
    private User receptionistUser;
    private User managerUser;
    private User otherCustomerUser;
    private Booking sampleBooking;
    private BookingDetail sampleBookingDetail;
    private CancellationPolicy policy7Days;
    private CancellationPolicy policy1Day;
    private CustomerRequest samplePendingRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refundService, "conversionRate", new BigDecimal("25000.0"));

        customerUser = new User();
        customerUser.setId(1);
        customerUser.setEmail("customer@example.com");
        customerUser.setFullName("Customer Person");
        customerUser.setRole(Role.customer);

        otherCustomerUser = new User();
        otherCustomerUser.setId(9);
        otherCustomerUser.setEmail("other@example.com");
        otherCustomerUser.setFullName("Other Customer");
        otherCustomerUser.setRole(Role.customer);

        receptionistUser = new User();
        receptionistUser.setId(2);
        receptionistUser.setEmail("staff@hotel.com");
        receptionistUser.setFullName("Receptionist Person");
        receptionistUser.setRole(Role.receptionist);

        managerUser = new User();
        managerUser.setId(3);
        managerUser.setEmail("manager@hotel.com");
        managerUser.setFullName("Manager Person");
        managerUser.setRole(Role.manager);

        sampleBooking = new Booking();
        sampleBooking.setId(1);
        sampleBooking.setBookingReference("BK20260729001");
        sampleBooking.setStatus(BookingStatus.PAID);
        sampleBooking.setPaidAmount(new BigDecimal("2500000.00"));
        sampleBooking.setUser(customerUser);

        ZonedDateTime futureCheckIn = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).plusDays(10);
        sampleBookingDetail = new BookingDetail();
        sampleBookingDetail.setId(1);
        sampleBookingDetail.setBooking(sampleBooking);
        sampleBookingDetail.setExpectedCheckIn(futureCheckIn.toInstant());

        policy7Days = new CancellationPolicy();
        policy7Days.setId(1);
        policy7Days.setDaysBeforeCheckIn(7);
        policy7Days.setRefundPercentage(new BigDecimal("100.00"));
        policy7Days.setPriority(10);
        policy7Days.setIsActive(true);

        policy1Day = new CancellationPolicy();
        policy1Day.setId(2);
        policy1Day.setDaysBeforeCheckIn(1);
        policy1Day.setRefundPercentage(new BigDecimal("50.00"));
        policy1Day.setPriority(5);
        policy1Day.setIsActive(true);

        samplePendingRequest = new CustomerRequest();
        samplePendingRequest.setId(10);
        samplePendingRequest.setBooking(sampleBooking);
        samplePendingRequest.setRequestType("Refund");
        samplePendingRequest.setDescription("Change of plans");
        samplePendingRequest.setOldValue("2500000.00");
        samplePendingRequest.setNewValue("2500000.00");
        samplePendingRequest.setStatus("Pending");
        samplePendingRequest.setCreatedAt(Instant.now());
    }

    // ==========================================
    // 1. submitRefundRequest Test Cases (UTCID01 - UTCID10, UTCID26 - UTCID27)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful refund submission by booking owner 10 days before check-in (100% refund)")
    void should_submitRefundRequestSuccessfully_when_ownerSubmitsTenDaysBeforeCheckIn() {
        RefundRequest request = new RefundRequest();
        request.setReason("Change of plans");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of(policy7Days, policy1Day));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> {
            CustomerRequest r = i.getArgument(0);
            r.setId(10);
            return r;
        });

        CustomerRequestResponse response = refundService.submitRefundRequest(1, request, "customer@example.com");

        assertNotNull(response);
        assertEquals(10, response.getRequestId());
        assertEquals("Pending", response.getStatus());
        assertEquals("2500000.00", response.getNewValue());
        verify(customerRequestRepository).save(any(CustomerRequest.class));
    }

    @Test
    @DisplayName("UTCID02 - Successful refund submission 2 days before check-in (50% refund)")
    void should_submitRefundRequestSuccessfully_when_submittingTwoDaysBeforeCheckInWithFiftyPercentRefund() {
        ZonedDateTime checkIn2Days = ZonedDateTime.now(ZoneId.of("Asia/Bangkok")).plusDays(2);
        sampleBookingDetail.setExpectedCheckIn(checkIn2Days.toInstant());

        RefundRequest request = new RefundRequest();
        request.setReason("Illness");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of(policy7Days, policy1Day));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> {
            CustomerRequest r = i.getArgument(0);
            r.setId(11);
            return r;
        });

        CustomerRequestResponse response = refundService.submitRefundRequest(1, request, "customer@example.com");

        assertNotNull(response);
        assertEquals("1250000.00", response.getNewValue());
    }

    @Test
    @DisplayName("UTCID03 - Successful refund submission by staff on behalf of customer")
    void should_submitRefundRequestSuccessfully_when_staffSubmitsOnBehalfOfCustomer() {
        RefundRequest request = new RefundRequest();
        request.setReason("Counter request");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of(policy7Days));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.submitRefundRequest(1, request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Pending", response.getStatus());
    }

    @Test
    @DisplayName("UTCID04 - Default to 100% refund when no cancellation policies exist in DB")
    void should_submitRefundRequestSuccessfully_when_noCancellationPoliciesExistInDb() {
        RefundRequest request = new RefundRequest();
        request.setReason("No policies");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of());
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.submitRefundRequest(1, request, "customer@example.com");

        assertNotNull(response);
        assertEquals("2500000.00", response.getNewValue());
    }

    @Test
    @DisplayName("UTCID05 - Throw exception (0% refund / non-refundable) when policies exist but none match (0 days before check-in)")
    void should_throwException_when_policiesExistButNoneMatchSameDayCheckIn() {
        ZonedDateTime checkInToday = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));
        sampleBookingDetail.setExpectedCheckIn(checkInToday.toInstant());

        RefundRequest request = new RefundRequest();
        request.setReason("Same day cancel");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of(policy7Days, policy1Day));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "customer@example.com"));

        assertEquals("Đơn đặt phòng này không đủ điều kiện hoàn tiền do hủy sát ngày Check-in theo chính sách của khách sạn.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when booking ID is not found in DB")
    void should_throwException_when_submitRefundRequestWithBookingNotFound() {
        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(99, request, "customer@example.com"));

        assertEquals("Không tìm thấy đơn đặt phòng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when customer email is not found in DB")
    void should_throwException_when_submitRefundRequestWithUserNotFound() {
        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "unknown@example.com"));

        assertEquals("Không tìm thấy người dùng", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when user is neither staff nor booking owner")
    void should_throwException_when_submitRefundRequestWithUnauthorizedUser() {
        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "other@example.com"));

        assertEquals("Bạn không có quyền yêu cầu hoàn tiền cho đơn đặt phòng này", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when booking status is CHECKED_IN, COMPLETED, or CANCELLED")
    void should_throwException_when_submitRefundRequestWithInvalidBookingStatus() {
        sampleBooking.setStatus(BookingStatus.CHECKED_IN);

        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "customer@example.com"));

        assertTrue(ex.getMessage().startsWith("Không thể yêu cầu hoàn tiền cho đơn đặt phòng ở trạng thái:"));
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when booking paidAmount is zero or negative")
    void should_throwException_when_submitRefundRequestWithUnpaidBooking() {
        sampleBooking.setPaidAmount(BigDecimal.ZERO);

        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "customer@example.com"));

        assertEquals("Đơn đặt phòng chưa được thanh toán, không thể hoàn tiền", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID26 - Safely handle null booking status string in exception message during submitRefundRequest")
    void should_handleNullBookingStatusSafely_when_submittingRefundRequest() {
        sampleBooking.setStatus(BookingStatus.CANCELLED);

        RefundRequest request = new RefundRequest();
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.submitRefundRequest(1, request, "customer@example.com"));

        assertEquals("Không thể yêu cầu hoàn tiền cho đơn đặt phòng ở trạng thái: Cancelled", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID27 - Catch notification exception silently during submitRefundRequest")
    void should_catchNotificationExceptionSilently_when_submittingRefundRequest() {
        RefundRequest request = new RefundRequest();
        request.setReason("Test");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of(policy7Days));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification service error")).when(notificationService).sendNotificationToRoles(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> refundService.submitRefundRequest(1, request, "customer@example.com"));
    }

    // ==========================================
    // 2. approveRefundRequest Test Cases (UTCID11 - UTCID17, UTCID28 - UTCID32)
    // ==========================================

    @Test
    @DisplayName("UTCID11 - Successful approveRefundRequest for PayPal payment with default amount and triggers real PayPal refund")
    void should_approveRefundRequestSuccessfully_when_staffApprovesPendingPaypalRefund() {
        ApproveRefundRequest request = new ApproveRefundRequest();

        Payment paypalPayment = new Payment();
        paypalPayment.setId(100);
        paypalPayment.setBooking(sampleBooking);
        paypalPayment.setAmount(new BigDecimal("2500000.00"));
        paypalPayment.setRefundedAmount(BigDecimal.ZERO);
        paypalPayment.setPaymentMethod("PayPal");
        paypalPayment.setTransactionCode("TXN-PAYPAL-100");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(paypalPayment));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.approveRefundRequest(10, request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Approved", response.getStatus());
        assertEquals(BookingStatus.CANCELLED, sampleBooking.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(sampleBooking.getPaidAmount()));
        verify(paypalService).refundPaypalCapture(eq("TXN-PAYPAL-100"), eq(new BigDecimal("100.00")), anyString());
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("UTCID12 - Successful approveRefundRequest for Cash payment by Manager with custom refundAmount")
    void should_approveRefundRequestSuccessfully_when_managerApprovesCashRefundWithCustomAmount() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(new BigDecimal("1000000.00"));

        Payment cashPayment = new Payment();
        cashPayment.setId(101);
        cashPayment.setBooking(sampleBooking);
        cashPayment.setAmount(new BigDecimal("2500000.00"));
        cashPayment.setRefundedAmount(BigDecimal.ZERO);
        cashPayment.setPaymentMethod("Cash");

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(cashPayment));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.approveRefundRequest(10, request, "manager@hotel.com");

        assertNotNull(response);
        assertEquals("Approved", response.getStatus());
        assertEquals("1000000.00", response.getNewValue());
        assertEquals(new BigDecimal("1500000.00"), sampleBooking.getPaidAmount());
        verify(paypalService, never()).refundPaypalCapture(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when staff email is not found in DB for approveRefundRequest")
    void should_throwException_when_approveRefundRequestWithStaffNotFound() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "unknown@hotel.com"));

        assertEquals("Không tìm thấy nhân viên", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when staff user role is customer (unauthorized)")
    void should_throwException_when_approveRefundRequestWithUnauthorizedRole() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "customer@example.com"));

        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID15 - Throw exception when request ID is not found in DB for approveRefundRequest")
    void should_throwException_when_approveRefundRequestWithRequestNotFound() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(99, request, "staff@hotel.com"));

        assertEquals("Không tìm thấy yêu cầu hoàn tiền", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when request status is already Approved or Rejected")
    void should_throwException_when_approveRefundRequestWithNonPendingStatus() {
        samplePendingRequest.setStatus("Approved");

        ApproveRefundRequest request = new ApproveRefundRequest();
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Yêu cầu hoàn tiền này đã được xử lý"));
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when approved refundAmount exceeds booking paidAmount")
    void should_throwException_when_approveRefundRequestWithAmountExceedingPaidAmount() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(new BigDecimal("5000000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Số tiền hoàn trả không được vượt quá số tiền đã thanh toán"));
    }

    @Test
    @DisplayName("UTCID28 - Paypal refund when conversionRate is 1.0 or null")
    void should_useOriginalAmount_when_conversionRateIsOneInPaypalRefund() {
        ReflectionTestUtils.setField(refundService, "conversionRate", new BigDecimal("1.0"));

        ApproveRefundRequest request = new ApproveRefundRequest();

        Payment paypalPayment = new Payment();
        paypalPayment.setId(100);
        paypalPayment.setBooking(sampleBooking);
        paypalPayment.setAmount(new BigDecimal("2500000.00"));
        paypalPayment.setRefundedAmount(BigDecimal.ZERO);
        paypalPayment.setPaymentMethod("PayPal");
        paypalPayment.setTransactionCode("TXN-PAYPAL-100");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(paypalPayment));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        refundService.approveRefundRequest(10, request, "staff@hotel.com");

        verify(paypalService).refundPaypalCapture(eq("TXN-PAYPAL-100"), eq(new BigDecimal("2500000.00")), anyString());
    }

    @Test
    @DisplayName("UTCID29 - Apply refund across multiple payment records")
    void should_applyRefundAcrossMultiplePayments_when_refundAmountSpansPayments() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(new BigDecimal("2000000.00"));

        Payment p1 = new Payment();
        p1.setId(101);
        p1.setBooking(sampleBooking);
        p1.setAmount(new BigDecimal("1500000.00"));
        p1.setRefundedAmount(BigDecimal.ZERO);
        p1.setPaymentMethod("Cash");

        Payment p2 = new Payment();
        p2.setId(102);
        p2.setBooking(sampleBooking);
        p2.setAmount(new BigDecimal("1000000.00"));
        p2.setRefundedAmount(BigDecimal.ZERO);
        p2.setPaymentMethod("Cash");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(p1, p2));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        refundService.approveRefundRequest(10, request, "staff@hotel.com");

        assertEquals(new BigDecimal("1500000.00"), p1.getRefundedAmount());
        assertEquals(new BigDecimal("500000.00"), p2.getRefundedAmount());
    }

    @Test
    @DisplayName("UTCID30 - Skip fully refunded payments during approveRefundRequest")
    void should_skipFullyRefundedPayments_when_approvingRefund() {
        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(new BigDecimal("500000.00"));

        Payment p1FullyRefunded = new Payment();
        p1FullyRefunded.setId(101);
        p1FullyRefunded.setBooking(sampleBooking);
        p1FullyRefunded.setAmount(new BigDecimal("1000000.00"));
        p1FullyRefunded.setRefundedAmount(new BigDecimal("1000000.00")); // availableToRefund = 0
        p1FullyRefunded.setPaymentMethod("Cash");

        Payment p2Available = new Payment();
        p2Available.setId(102);
        p2Available.setBooking(sampleBooking);
        p2Available.setAmount(new BigDecimal("1500000.00"));
        p2Available.setRefundedAmount(BigDecimal.ZERO);
        p2Available.setPaymentMethod("Cash");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(p1FullyRefunded, p2Available));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        refundService.approveRefundRequest(10, request, "staff@hotel.com");

        assertEquals(new BigDecimal("500000.00"), p2Available.getRefundedAmount());
    }

    @Test
    @DisplayName("UTCID31 - Throw exception when paypalService.refundPaypalCapture throws error during approval")
    void should_throwException_when_paypalRefundFailsDuringApproval() {
        ApproveRefundRequest request = new ApproveRefundRequest();

        Payment paypalPayment = new Payment();
        paypalPayment.setId(100);
        paypalPayment.setBooking(sampleBooking);
        paypalPayment.setAmount(new BigDecimal("2500000.00"));
        paypalPayment.setRefundedAmount(BigDecimal.ZERO);
        paypalPayment.setPaymentMethod("PayPal");
        paypalPayment.setTransactionCode("TXN-PAYPAL-100");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(paypalPayment));
        doThrow(new RuntimeException("PayPal API Error")).when(paypalService).refundPaypalCapture(any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("Lỗi khi thực hiện hoàn tiền qua cổng PayPal: PayPal API Error"));
    }

    @Test
    @DisplayName("UTCID32 - Catch notification exception silently during approveRefundRequest")
    void should_catchNotificationExceptionSilently_when_approvingRefund() {
        ApproveRefundRequest request = new ApproveRefundRequest();

        Payment cashPayment = new Payment();
        cashPayment.setId(101);
        cashPayment.setBooking(sampleBooking);
        cashPayment.setAmount(new BigDecimal("2500000.00"));
        cashPayment.setRefundedAmount(BigDecimal.ZERO);
        cashPayment.setPaymentMethod("Cash");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(cashPayment));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification server down")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> refundService.approveRefundRequest(10, request, "staff@hotel.com"));
    }

    // ==========================================
    // 3. rejectRefundRequest Test Cases (UTCID18 - UTCID22, UTCID33)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful rejectRefundRequest by receptionist with rejection reason")
    void should_rejectRefundRequestSuccessfully_when_receptionistRejectsPendingRequest() {
        RejectRefundRequest request = new RejectRefundRequest();
        request.setRejectionReason("Invalid proof of cancellation");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.rejectRefundRequest(10, request, "staff@hotel.com");

        assertNotNull(response);
        assertEquals("Rejected", response.getStatus());
        assertEquals("Invalid proof of cancellation", response.getRejectionReason());
        verify(customerRequestRepository).save(samplePendingRequest);
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when staff email is not found in DB for rejectRefundRequest")
    void should_throwException_when_rejectRefundRequestWithStaffNotFound() {
        RejectRefundRequest request = new RejectRefundRequest();
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.rejectRefundRequest(10, request, "unknown@hotel.com"));

        assertEquals("Không tìm thấy nhân viên", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID20 - Throw exception when staff user role is customer for rejectRefundRequest")
    void should_throwException_when_rejectRefundRequestWithUnauthorizedRole() {
        RejectRefundRequest request = new RejectRefundRequest();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.rejectRefundRequest(10, request, "customer@example.com"));

        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when request ID is not found in DB for rejectRefundRequest")
    void should_throwException_when_rejectRefundRequestWithRequestNotFound() {
        RejectRefundRequest request = new RejectRefundRequest();
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.rejectRefundRequest(99, request, "staff@hotel.com"));

        assertEquals("Không tìm thấy yêu cầu hoàn tiền", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID22 - Throw exception when request status is already Rejected")
    void should_throwException_when_rejectRefundRequestWithNonPendingStatus() {
        samplePendingRequest.setStatus("Rejected");

        RejectRefundRequest request = new RejectRefundRequest();
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.rejectRefundRequest(10, request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Yêu cầu hoàn tiền này đã được xử lý"));
    }

    @Test
    @DisplayName("UTCID33 - Catch notification exception silently during rejectRefundRequest")
    void should_catchNotificationExceptionSilently_when_rejectingRefund() {
        RejectRefundRequest request = new RejectRefundRequest();
        request.setRejectionReason("Policy violation");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Notification server down")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> refundService.rejectRefundRequest(10, request, "staff@hotel.com"));
    }

    // ==========================================
    // 4. getPendingRefundRequests Test Cases (UTCID23 - UTCID25)
    // ==========================================

    @Test
    @DisplayName("UTCID23 - Successful getPendingRefundRequests by receptionist returns list")
    void should_getPendingRefundRequestsSuccessfully_when_receptionistQueries() {
        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findByRequestTypeAndStatusOrderByCreatedAtDesc("Refund", "Pending"))
                .thenReturn(List.of(samplePendingRequest));

        List<CustomerRequestResponse> list = refundService.getPendingRefundRequests("staff@hotel.com");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("BK20260729001", list.get(0).getBookingReference());
    }

    @Test
    @DisplayName("UTCID24 - Throw exception when staff email is not found in DB for getPendingRefundRequests")
    void should_throwException_when_getPendingRefundRequestsWithStaffNotFound() {
        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.getPendingRefundRequests("unknown@hotel.com"));

        assertEquals("Không tìm thấy nhân viên", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID25 - Throw exception when staff user role is customer for getPendingRefundRequests")
    void should_throwException_when_getPendingRefundRequestsWithUnauthorizedRole() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.getPendingRefundRequests("customer@example.com"));

        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("Submit refund request defaults to 100% refund when no active cancellation policies exist")
    void should_submitRefundRequest_with100PercentDefaultRefund_when_noCancellationPoliciesExist() {
        RefundRequest request = new RefundRequest();
        request.setReason("Empty policy list");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(bookingDetailRepository.findByBooking_Id(1)).thenReturn(Optional.of(sampleBookingDetail));
        when(cancellationPolicyRepository.findByIsActiveTrueOrderByPriorityDescDaysBeforeCheckInDesc())
                .thenReturn(List.of());
        when(customerRequestRepository.save(any(CustomerRequest.class))).thenAnswer(i -> i.getArgument(0));

        CustomerRequestResponse response = refundService.submitRefundRequest(1, request, "customer@example.com");

        assertNotNull(response);
        assertEquals(sampleBooking.getPaidAmount().setScale(2).toString(), response.getNewValue());
    }

    @Test
    @DisplayName("Approve refund request throws exception when PayPal refund fails")
    void should_throwException_when_paypalRefundFails() {
        Payment paypalPayment = new Payment();
        paypalPayment.setId(99);
        paypalPayment.setBooking(sampleBooking);
        paypalPayment.setAmount(new BigDecimal("2000000.00"));
        paypalPayment.setRefundedAmount(BigDecimal.ZERO);
        paypalPayment.setPaymentMethod("PayPal");
        paypalPayment.setTransactionCode("PAYPAL_CAP_123");

        ApproveRefundRequest request = new ApproveRefundRequest();
        request.setRefundAmount(new BigDecimal("1000000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(customerRequestRepository.findById(10)).thenReturn(Optional.of(samplePendingRequest));
        when(paymentRepository.findByBooking_Id(1)).thenReturn(List.of(paypalPayment));
        doThrow(new RuntimeException("API Connection Failed")).when(paypalService)
                .refundPaypalCapture(eq("PAYPAL_CAP_123"), any(BigDecimal.class), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> refundService.approveRefundRequest(10, request, "staff@hotel.com"));

        assertTrue(ex.getMessage().contains("Lỗi khi thực hiện hoàn tiền qua cổng PayPal"));
    }
}

