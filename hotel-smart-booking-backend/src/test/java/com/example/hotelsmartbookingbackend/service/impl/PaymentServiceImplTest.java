package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CaptureOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.ManualPaymentRequest;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaypalService paypalService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Booking sampleBooking;
    private User customerUser;
    private User receptionistUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "conversionRate", new BigDecimal("25000.0"));

        customerUser = new User();
        customerUser.setId(1);
        customerUser.setEmail("customer@example.com");
        customerUser.setFullName("Customer Person");
        customerUser.setRole(Role.customer);

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
        sampleBooking.setStatus(BookingStatus.CONFIRMED);
        sampleBooking.setFinalAmount(new BigDecimal("2500000.00"));
        sampleBooking.setPaidAmount(BigDecimal.ZERO);
        sampleBooking.setDepositAmount(BigDecimal.ZERO);
        sampleBooking.setUser(customerUser);
    }

    // ==========================================
    // 1. createOrder Test Cases (UTCID01 - UTCID09)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful createOrder for full payment option with currency conversion")
    void should_createOrderSuccessfully_when_fullPaymentOptionAndConversionRateConfigured() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setPaymentOption("FULL");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PaypalOrderResponse orderResponse = new PaypalOrderResponse();
        orderResponse.setPaypalOrderId("PAYPAL-ORD-001");
        when(paypalService.createPaypalOrder(eq(1), eq(new BigDecimal("100.00")), eq("ik-001")))
                .thenReturn(orderResponse);

        PaypalOrderResponse response = paymentService.createOrder(request, "ik-001");

        assertNotNull(response);
        assertEquals("PAYPAL-ORD-001", response.getPaypalOrderId());
        verify(valueOperations).set(eq("paypal:order_booking:PAYPAL-ORD-001"), eq("1:2500000.00:FULL"), any(Duration.class));
    }

    @Test
    @DisplayName("UTCID02 - Successful createOrder for 30% DEPOSIT payment option")
    void should_createOrderSuccessfully_when_depositOptionSelected() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setPaymentOption("DEPOSIT");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PaypalOrderResponse orderResponse = new PaypalOrderResponse();
        orderResponse.setPaypalOrderId("PAYPAL-ORD-002");
        when(paypalService.createPaypalOrder(eq(1), eq(new BigDecimal("30.00")), eq("ik-002")))
                .thenReturn(orderResponse);

        PaypalOrderResponse response = paymentService.createOrder(request, "ik-002");

        assertNotNull(response);
        assertEquals("PAYPAL-ORD-002", response.getPaypalOrderId());
        verify(valueOperations).set(eq("paypal:order_booking:PAYPAL-ORD-002"), eq("1:750000.00:DEPOSIT"), any(Duration.class));
    }

    @Test
    @DisplayName("UTCID03 - Successful createOrder with custom amount and no conversion (rate=1.0)")
    void should_createOrderSuccessfully_when_customAmountAndNoConversion() {
        ReflectionTestUtils.setField(paymentService, "conversionRate", new BigDecimal("1.0"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("500000.00"));
        request.setPaymentOption("CUSTOM");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PaypalOrderResponse orderResponse = new PaypalOrderResponse();
        orderResponse.setPaypalOrderId("PAYPAL-ORD-003");
        when(paypalService.createPaypalOrder(eq(1), eq(new BigDecimal("500000.00")), eq("ik-003")))
                .thenReturn(orderResponse);

        PaypalOrderResponse response = paymentService.createOrder(request, "ik-003");

        assertNotNull(response);
        verify(valueOperations).set(eq("paypal:order_booking:PAYPAL-ORD-003"), eq("1:500000.00:CUSTOM"), any(Duration.class));
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when booking is not found in DB")
    void should_throwException_when_createOrderWithBookingNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(99);

        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request, "ik-004"));

        assertEquals("Booking not found with ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when booking status is already PAID")
    void should_throwException_when_createOrderWithBookingAlreadyPaid() {
        sampleBooking.setStatus(BookingStatus.PAID);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request, "ik-005"));

        assertEquals("Booking is already fully paid", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when booking status is CANCELLED")
    void should_throwException_when_createOrderWithBookingCancelled() {
        sampleBooking.setStatus(BookingStatus.CANCELLED);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request, "ik-006"));

        assertEquals("Booking has been cancelled", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when custom charge amount is zero or negative")
    void should_throwException_when_createOrderWithZeroOrNegativeAmount() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("-100.00"));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request, "ik-007"));

        assertEquals("Payment amount must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when custom charge amount exceeds remaining balance")
    void should_throwException_when_createOrderWithAmountExceedingRemainingBalance() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("5000000.00"));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request, "ik-008"));

        assertTrue(ex.getMessage().startsWith("Payment amount exceeds remaining balance of 2500000.00"));
    }

    @Test
    @DisplayName("UTCID09 - Successful createOrder when conversion rate is null")
    void should_createOrderSuccessfully_when_conversionRateIsNull() {
        ReflectionTestUtils.setField(paymentService, "conversionRate", null);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("1000000.00"));
        request.setPaymentOption("CUSTOM");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PaypalOrderResponse orderResponse = new PaypalOrderResponse();
        orderResponse.setPaypalOrderId("PAYPAL-ORD-009");
        when(paypalService.createPaypalOrder(eq(1), eq(new BigDecimal("1000000.00")), eq("ik-009")))
                .thenReturn(orderResponse);

        PaypalOrderResponse response = paymentService.createOrder(request, "ik-009");

        assertNotNull(response);
        assertEquals("PAYPAL-ORD-009", response.getPaypalOrderId());
    }

    // ==========================================
    // 2. captureOrder Test Cases (UTCID10 - UTCID16, UTCID26 - UTCID29)
    // ==========================================

    @Test
    @DisplayName("UTCID10 - Successful captureOrder for FULL payment option and updates booking to PAID")
    void should_captureOrderSuccessfully_when_fullPaymentCompletedAndRedisCached() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-010");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-010")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-010");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-010", "ik-010")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        PaypalCaptureResponse response = paymentService.captureOrder(request, "ik-010");

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(BookingStatus.PAID, sampleBooking.getStatus());
        assertEquals(new BigDecimal("2500000.00"), sampleBooking.getPaidAmount());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(sampleBooking);
        verify(redisTemplate).delete("paypal:order_booking:PAYPAL-ORD-010");
    }

    @Test
    @DisplayName("UTCID11 - Successful captureOrder for DEPOSIT payment option and updates booking to PARTIALLY_PAID")
    void should_captureOrderSuccessfully_when_depositPaymentCompleted() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-011");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-011")).thenReturn("1:750000.00:DEPOSIT");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-011");
        captureResponse.setAmount(new BigDecimal("30.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-011", "ik-011")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        PaypalCaptureResponse response = paymentService.captureOrder(request, "ik-011");

        assertNotNull(response);
        assertEquals(BookingStatus.PARTIALLY_PAID, sampleBooking.getStatus());
        assertEquals(new BigDecimal("750000.00"), sampleBooking.getPaidAmount());
        assertEquals(new BigDecimal("750000.00"), sampleBooking.getDepositAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when PayPal capture response status is NOT COMPLETED")
    void should_throwException_when_captureOrderWithPaypalStatusNotCompleted() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-012");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-012")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("DECLINED");

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-012", "ik-012")).thenReturn(captureResponse);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.captureOrder(request, "ik-012"));

        assertEquals("PayPal order capture status is not COMPLETED: DECLINED", ex.getMessage());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when Redis mapping for PayPal order ID is missing/expired")
    void should_throwException_when_captureOrderWithRedisMappingExpired() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-999");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-999")).thenReturn(null);

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-999", "ik-013")).thenReturn(captureResponse);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.captureOrder(request, "ik-013"));

        assertEquals("Could not associate PayPal order with a booking. Cache may have expired.", ex.getMessage());
        verify(bookingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when booking ID extracted from Redis is not found in DB")
    void should_throwException_when_captureOrderWithBookingNotFoundInDb() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-014");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-014")).thenReturn("100:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-014", "ik-014")).thenReturn(captureResponse);
        when(bookingRepository.findById(100)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.captureOrder(request, "ik-014"));

        assertEquals("Booking not found for ID: 100", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID15 - Fallback conversion when expected charge amount in Redis value is null")
    void should_captureOrderSuccessfully_when_expectedChargeAmountInRedisIsNull() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-015");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-015")).thenReturn("1");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-015");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-015", "ik-015")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        PaypalCaptureResponse response = paymentService.captureOrder(request, "ik-015");

        assertNotNull(response);
        assertEquals(new BigDecimal("2500000.00"), sampleBooking.getPaidAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("UTCID16 - Preserve status when booking is already CHECKED_IN during capture")
    void should_captureOrderSuccessfully_when_bookingStatusIsCheckedIn() {
        sampleBooking.setStatus(BookingStatus.CHECKED_IN);

        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-016");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-016")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-016");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-016", "ik-016")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.captureOrder(request, "ik-016");

        assertEquals(BookingStatus.CHECKED_IN, sampleBooking.getStatus());
        assertEquals(new BigDecimal("2500000.00"), sampleBooking.getPaidAmount());
    }

    @Test
    @DisplayName("UTCID26 - Fallback expectedChargeAmount equals captureResponse amount when conversionRate is 1.0")
    void should_fallbackToCaptureAmount_when_conversionRateIsOneAndExpectedChargeAmountIsNull() {
        ReflectionTestUtils.setField(paymentService, "conversionRate", new BigDecimal("1.0"));

        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-026");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-026")).thenReturn("1:null");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-026");
        captureResponse.setAmount(new BigDecimal("500.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-026", "ik-026")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.captureOrder(request, "ik-026");

        assertEquals(new BigDecimal("500.00"), sampleBooking.getPaidAmount());
    }

    @Test
    @DisplayName("UTCID27 - Preserve status when booking is STAYING during capture")
    void should_preserveStayingStatus_when_capturingOrderForStayingBooking() {
        sampleBooking.setStatus(BookingStatus.STAYING);

        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-027");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-027")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-027");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-027", "ik-027")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.captureOrder(request, "ik-027");

        assertEquals(BookingStatus.STAYING, sampleBooking.getStatus());
    }

    @Test
    @DisplayName("UTCID28 - Preserve status when booking is COMPLETED during capture")
    void should_preserveCompletedStatus_when_capturingOrderForCompletedBooking() {
        sampleBooking.setStatus(BookingStatus.COMPLETED);

        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-028");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-028")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-028");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-028", "ik-028")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.captureOrder(request, "ik-028");

        assertEquals(BookingStatus.COMPLETED, sampleBooking.getStatus());
    }

    @Test
    @DisplayName("UTCID29 - Catch notification exception silently during captureOrder")
    void should_catchNotificationExceptionSilently_when_sendingCaptureNotification() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-029");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-029")).thenReturn("1:2500000.00:FULL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setTransactionId("TXN-PAYPAL-029");
        captureResponse.setAmount(new BigDecimal("100.00"));

        when(paypalService.capturePaypalOrder("PAYPAL-ORD-029", "ik-029")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        doThrow(new RuntimeException("Notification server down")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> paymentService.captureOrder(request, "ik-029"));

        verify(paymentRepository).save(any(Payment.class));
        verify(redisTemplate).delete("paypal:order_booking:PAYPAL-ORD-029");
    }

    // ==========================================
    // 3. processManualPayment Test Cases (UTCID17 - UTCID25, UTCID30 - UTCID32)
    // ==========================================

    @Test
    @DisplayName("UTCID17 - Successful processManualPayment for full cash payment by Receptionist staff")
    void should_processManualPaymentSuccessfully_when_receptionistFullCashPayment() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("2500000.00"));
        request.setPaymentMethod("Cash");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.processManualPayment(request, "staff@hotel.com");

        assertEquals(BookingStatus.PAID, sampleBooking.getStatus());
        assertEquals(new BigDecimal("2500000.00"), sampleBooking.getPaidAmount());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("UTCID18 - Successful processManualPayment for partial deposit bank transfer by Manager")
    void should_processManualPaymentSuccessfully_when_managerDepositBankTransfer() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("750000.00"));
        request.setPaymentMethod("Bank Transfer");

        when(userRepository.findByEmail("manager@hotel.com")).thenReturn(Optional.of(managerUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.processManualPayment(request, "manager@hotel.com");

        assertEquals(BookingStatus.PARTIALLY_PAID, sampleBooking.getStatus());
        assertEquals(new BigDecimal("750000.00"), sampleBooking.getPaidAmount());
        assertEquals(new BigDecimal("750000.00"), sampleBooking.getDepositAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when staff email is not found in DB")
    void should_throwException_when_processManualPaymentWithStaffNotFound() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("unknown@hotel.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "unknown@hotel.com"));

        assertEquals("Không tìm thấy nhân viên", ex.getMessage());
        verify(bookingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("UTCID20 - Throw exception when staff user role is customer (unauthorized)")
    void should_throwException_when_processManualPaymentWithUnauthorizedCustomerRole() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "customer@example.com"));

        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
        verify(bookingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when booking ID is not found for manual payment")
    void should_throwException_when_processManualPaymentWithBookingNotFound() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(99);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "staff@hotel.com"));

        assertEquals("Booking not found with ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID22 - Throw exception when booking status is already PAID for manual payment")
    void should_throwException_when_processManualPaymentWithBookingAlreadyPaid() {
        sampleBooking.setStatus(BookingStatus.PAID);

        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "staff@hotel.com"));

        assertEquals("Booking is already fully paid", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID23 - Throw exception when booking status is CANCELLED for manual payment")
    void should_throwException_when_processManualPaymentWithBookingCancelled() {
        sampleBooking.setStatus(BookingStatus.CANCELLED);

        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "staff@hotel.com"));

        assertEquals("Booking has been cancelled", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID24 - Throw exception when manual charge amount is null or zero/negative")
    void should_throwException_when_processManualPaymentWithInvalidAmount() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(BigDecimal.ZERO);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "staff@hotel.com"));

        assertEquals("Payment amount must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID25 - Throw exception when manual charge amount exceeds remaining balance")
    void should_throwException_when_processManualPaymentWithAmountExceedingBalance() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("5000000.00"));

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "staff@hotel.com"));

        assertTrue(ex.getMessage().startsWith("Payment amount exceeds remaining balance of 2500000.00"));
    }

    @Test
    @DisplayName("UTCID30 - Process manual payment with explicitly provided paymentType and notes")
    void should_useProvidedPaymentTypeAndNotes_when_explicitlyPassedInManualPayment() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("1000000.00"));
        request.setPaymentMethod("POS");
        request.setPaymentType("Service Charge");
        request.setNotes("Custom counter note");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.processManualPayment(request, "staff@hotel.com");

        verify(paymentRepository).save(argThat(p -> 
                "Service Charge".equals(p.getPaymentType()) &&
                "Custom counter note".equals(p.getNotes()) &&
                "POS".equals(p.getPaymentMethod())
        ));
    }

    @Test
    @DisplayName("UTCID31 - Process manual payment fallback default notes when notes field is null")
    void should_useDefaultNotes_when_notesFieldIsNullInManualPayment() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("1000000.00"));
        request.setPaymentMethod("Cash");
        request.setNotes(null);

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        paymentService.processManualPayment(request, "staff@hotel.com");

        verify(paymentRepository).save(argThat(p -> 
                p.getNotes().contains("Counter payment approved by Receptionist Person")
        ));
    }

    @Test
    @DisplayName("UTCID32 - Catch notification exception silently during processManualPayment")
    void should_catchNotificationExceptionSilently_when_sendingManualPaymentNotification() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("2500000.00"));
        request.setPaymentMethod("Cash");

        when(userRepository.findByEmail("staff@hotel.com")).thenReturn(Optional.of(receptionistUser));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));
        doThrow(new RuntimeException("Notification service down")).when(notificationService).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> paymentService.processManualPayment(request, "staff@hotel.com"));

        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("Throw exception when PayPal order capture status is not COMPLETED")
    void should_throwException_when_captureOrderResponseStatusIsNotCompleted() {
        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-FAIL");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("DECLINED");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-FAIL")).thenReturn("1:1000000.00:FULL");
        when(paypalService.capturePaypalOrder("PAYPAL-ORD-FAIL", "ik-fail")).thenReturn(captureResponse);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.captureOrder(request, "ik-fail"));

        assertEquals("PayPal order capture status is not COMPLETED: DECLINED", ex.getMessage());
    }

    @Test
    @DisplayName("Throw exception when user is not staff during processManualPayment")
    void should_throwException_when_processManualPaymentWithNonStaffUser() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBookingId(1);
        request.setAmount(new BigDecimal("500000.00"));

        User customerUser = new User();
        customerUser.setId(5);
        customerUser.setEmail("customer@example.com");
        customerUser.setRole(com.example.hotelsmartbookingbackend.enums.Role.customer);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.processManualPayment(request, "customer@example.com"));

        assertEquals("Bạn không có quyền thực hiện chức năng này", ex.getMessage());
    }

    @Test
    @DisplayName("Fallback to raw capture amount when expectedChargeAmount is null and conversionRate is null")
    void should_fallbackToCapturedUsdAmount_when_expectedChargeAmountIsNullAndConversionRateIsNull() {
        ReflectionTestUtils.setField(paymentService, "conversionRate", null);

        CaptureOrderRequest request = new CaptureOrderRequest();
        request.setPaypalOrderId("PAYPAL-ORD-NULL-AMT");

        PaypalCaptureResponse captureResponse = new PaypalCaptureResponse();
        captureResponse.setStatus("COMPLETED");
        captureResponse.setAmount(new BigDecimal("100.00")); // 100 USD
        captureResponse.setTransactionId("CAP-TRANS-100");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:order_booking:PAYPAL-ORD-NULL-AMT")).thenReturn("1:null:FULL");
        when(paypalService.capturePaypalOrder("PAYPAL-ORD-NULL-AMT", "ik-null-amt")).thenReturn(captureResponse);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(sampleBooking));

        PaypalCaptureResponse response = paymentService.captureOrder(request, "ik-null-amt");

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
    }
}

