package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CaptureOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.BankTransferRequest;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.service.PaymentService;
import com.example.hotelsmartbookingbackend.service.PaypalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaypalService paypalService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String ORDER_BOOKING_PREFIX = "paypal:order_booking:";

    @Value("${paypal.conversion-rate:25000.0}")
    private BigDecimal conversionRate;

    @Override
    @Transactional
    public PaypalOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + request.getBookingId()));

        if ("Paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already fully paid");
        }
        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking has been cancelled");
        }

        BigDecimal remainingBalance = booking.getFinalamount().subtract(booking.getPaidamount());
        BigDecimal chargeAmount = request.getAmount();

        String option = request.getPaymentOption();
        if (option == null) {
            option = "FULL";
        }

        if (chargeAmount == null) {
            if ("DEPOSIT".equalsIgnoreCase(option)) {
                // Deposit 30% of final amount
                chargeAmount = booking.getFinalamount().multiply(new BigDecimal("0.30")).setScale(2,
                        RoundingMode.HALF_UP);
            } else {
                chargeAmount = remainingBalance;
            }
        } else {
            if (chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Payment amount must be greater than zero");
            }
            if (chargeAmount.compareTo(remainingBalance) > 0) {
                throw new RuntimeException("Payment amount exceeds remaining balance of " + remainingBalance);
            }
        }

        // Apply conversion rate if configured (e.g., convert VND to USD)
        BigDecimal paypalAmount = chargeAmount;
        if (conversionRate != null && conversionRate.compareTo(BigDecimal.ONE) > 0) {
            paypalAmount = chargeAmount.divide(conversionRate, 2, RoundingMode.HALF_UP);
            log.info("Converted local amount {} to {} USD (Conversion Rate: {})", chargeAmount, paypalAmount,
                    conversionRate);
        }

        PaypalOrderResponse paypalOrder = paypalService.createPaypalOrder(booking.getId(), paypalAmount,
                idempotencyKey);

        // Store the mapping of PayPal Order ID -> Booking ID, Original Charge Amount,
        // and Option in Redis for 24h
        String redisKey = ORDER_BOOKING_PREFIX + paypalOrder.getPaypalOrderId();
        redisTemplate.opsForValue().set(redisKey, booking.getId() + ":" + chargeAmount + ":" + option,
                Duration.ofDays(1));

        return paypalOrder;
    }

    @Override
    @Transactional
    public PaypalCaptureResponse captureOrder(CaptureOrderRequest request, String idempotencyKey) {
        String paypalOrderId = request.getPaypalOrderId();

        // 1. Fetch booking reference and charge amount from Redis
        String redisKey = ORDER_BOOKING_PREFIX + paypalOrderId;
        String cachedValue = (String) redisTemplate.opsForValue().get(redisKey);

        Integer bookingId = null;
        BigDecimal expectedChargeAmount = null;
        String paymentOption = "FULL";

        if (cachedValue != null) {
            String[] parts = cachedValue.split(":");
            bookingId = Integer.parseInt(parts[0]);
            expectedChargeAmount = new BigDecimal(parts[1]);
            if (parts.length > 2) {
                paymentOption = parts[2];
            }
        }

        // 2. Call PayPal to capture the order
        PaypalCaptureResponse captureResponse = paypalService.capturePaypalOrder(paypalOrderId, idempotencyKey);

        if (!"COMPLETED".equalsIgnoreCase(captureResponse.getStatus())) {
            throw new RuntimeException("PayPal order capture status is not COMPLETED: " + captureResponse.getStatus());
        }

        // 3. Check if we have the booking associated
        if (bookingId == null) {
            log.warn("Redis mapping for PayPal Order ID {} not found. Cache may have expired.", paypalOrderId);
            throw new RuntimeException("Could not associate PayPal order with a booking. Cache may have expired.");
        }

        final Integer finalBookingId = bookingId;
        Booking booking = bookingRepository.findById(finalBookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found for ID: " + finalBookingId));

        if (expectedChargeAmount == null) {
            // Fallback: convert captured USD back using conversion rate
            if (conversionRate != null && conversionRate.compareTo(BigDecimal.ONE) > 0) {
                expectedChargeAmount = captureResponse.getAmount().multiply(conversionRate).setScale(2,
                        RoundingMode.HALF_UP);
            } else {
                expectedChargeAmount = captureResponse.getAmount();
            }
        }

        // 4. Create Payment Record in Database
        Payment payment = new Payment();
        payment.setBookingid(booking);
        payment.setAmount(expectedChargeAmount);
        payment.setPaymentmethod("PayPal");
        payment.setPaymenttype("DEPOSIT".equalsIgnoreCase(paymentOption) ? "Deposit" : "Booking Payment");
        payment.setTransactioncode(captureResponse.getTransactionId());
        payment.setStatus("Completed");
        payment.setRefundedamount(BigDecimal.ZERO);
        payment.setPaymentdate(Instant.now());
        payment.setNotes("PayPal Order ID: " + paypalOrderId + " | USD captured: " + captureResponse.getAmount());

        paymentRepository.save(payment);

        // 5. Update Booking paid amount and status
        booking.setPaidamount(booking.getPaidamount().add(expectedChargeAmount));
        if ("DEPOSIT".equalsIgnoreCase(paymentOption)) {
            booking.setDepositamount(expectedChargeAmount);
        }
        booking.setUpdatedat(Instant.now());

        if (booking.getPaidamount().compareTo(booking.getFinalamount()) >= 0) {
            booking.setStatus("Paid");
        } else if (booking.getPaidamount().compareTo(BigDecimal.ZERO) > 0) {
            booking.setStatus("Partially Paid");
        }

        bookingRepository.save(booking);

        // Clean up Redis mapping
        redisTemplate.delete(redisKey);

        return captureResponse;
    }

    @Override
    @Transactional
    public void processBankTransfer(BankTransferRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + request.getBookingId()));

        if ("Paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already fully paid");
        }
        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking has been cancelled");
        }

        BigDecimal remainingBalance = booking.getFinalamount().subtract(booking.getPaidamount());
        BigDecimal chargeAmount = request.getAmount();

        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be greater than zero");
        }
        if (chargeAmount.compareTo(remainingBalance) > 0) {
            throw new RuntimeException("Payment amount exceeds remaining balance of " + remainingBalance);
        }

        // 1. Create Payment Record in Database
        Payment payment = new Payment();
        payment.setBookingid(booking);
        payment.setAmount(chargeAmount);
        payment.setPaymentmethod("Bank Transfer");

        boolean isFirstPayment = booking.getPaidamount().compareTo(BigDecimal.ZERO) == 0;
        boolean isPartial = chargeAmount.compareTo(booking.getFinalamount()) < 0;

        payment.setPaymenttype(isFirstPayment && isPartial ? "Deposit" : "Booking Payment");
        payment.setTransactioncode(request.getTransactionCode() != null ? request.getTransactionCode()
                : "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus("Completed");
        payment.setRefundedamount(BigDecimal.ZERO);
        payment.setPaymentdate(Instant.now());
        payment.setNotes("Direct Bank Transfer payment. Approved by system.");

        paymentRepository.save(payment);

        // 2. Update Booking paid amount and status
        booking.setPaidamount(booking.getPaidamount().add(chargeAmount));
        if (isFirstPayment && isPartial) {
            booking.setDepositamount(chargeAmount);
        }
        booking.setUpdatedat(Instant.now());

        if (booking.getPaidamount().compareTo(booking.getFinalamount()) >= 0) {
            booking.setStatus("Paid");
        } else if (booking.getPaidamount().compareTo(BigDecimal.ZERO) > 0) {
            booking.setStatus("Partially Paid");
        }

        bookingRepository.save(booking);
    }
}
