package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CaptureOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateOrderRequest;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

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

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(paymentService, "conversionRate", new BigDecimal("1.0"));
    }

    @Test
    void testCreateOrder_Success() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus("Pending");
        booking.setFinalamount(new BigDecimal("100.00"));
        booking.setPaidamount(BigDecimal.ZERO);

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        
        PaypalOrderResponse mockResponse = PaypalOrderResponse.builder()
                .paypalOrderId("PAY-12345")
                .approveUrl("https://paypal.com/approve")
                .status("CREATED")
                .build();
        when(paypalService.createPaypalOrder(anyInt(), any(BigDecimal.class), anyString())).thenReturn(mockResponse);

        CreateOrderRequest request = new CreateOrderRequest(1, new BigDecimal("100.00"));
        PaypalOrderResponse response = paymentService.createOrder(request, "idemp-key-1");

        assertNotNull(response);
        assertEquals("PAY-12345", response.getPaypalOrderId());
        assertEquals("https://paypal.com/approve", response.getApproveUrl());

        verify(bookingRepository).findById(1);
        verify(valueOperations).set(eq("paypal:order_booking:PAY-12345"), eq("1:100.00:FULL"), any());
    }

    @Test
    void testCreateOrder_AlreadyPaid() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus("Paid");
        booking.setFinalamount(new BigDecimal("100.00"));
        booking.setPaidamount(new BigDecimal("100.00"));

        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        CreateOrderRequest request = new CreateOrderRequest(1, new BigDecimal("100.00"));
        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createOrder(request, "idemp-key-2");
        });

        assertEquals("Booking is already fully paid", exception.getMessage());
    }

    @Test
    void testCaptureOrder_Success() {
        Booking booking = new Booking();
        booking.setId(1);
        booking.setStatus("Pending");
        booking.setFinalamount(new BigDecimal("150.00"));
        booking.setPaidamount(BigDecimal.ZERO);

        when(valueOperations.get("paypal:order_booking:PAY-12345")).thenReturn("1:150.00");
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        PaypalCaptureResponse mockCapture = PaypalCaptureResponse.builder()
                .paypalOrderId("PAY-12345")
                .transactionId("TXN-789")
                .status("COMPLETED")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .build();
        when(paypalService.capturePaypalOrder(anyString(), anyString())).thenReturn(mockCapture);

        CaptureOrderRequest request = new CaptureOrderRequest("PAY-12345");
        PaypalCaptureResponse response = paymentService.captureOrder(request, "idemp-key-3");

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("TXN-789", response.getTransactionId());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(new BigDecimal("150.00"), savedPayment.getAmount());
        assertEquals("PayPal", savedPayment.getPaymentmethod());
        assertEquals("Completed", savedPayment.getStatus());
        assertEquals(booking, savedPayment.getBookingid());

        verify(bookingRepository).save(booking);
        assertEquals("Paid", booking.getStatus());
        assertEquals(new BigDecimal("150.00"), booking.getPaidamount());
    }
}
