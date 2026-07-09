package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.ApproveRefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RejectRefundRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import com.example.hotelsmartbookingbackend.entity.Cancellationpolicy;
import com.example.hotelsmartbookingbackend.entity.Customerrequest;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingdetailRepository;
import com.example.hotelsmartbookingbackend.repository.CancellationpolicyRepository;
import com.example.hotelsmartbookingbackend.repository.CustomerrequestRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final BookingRepository bookingRepository;
    private final BookingdetailRepository bookingdetailRepository;
    private final CancellationpolicyRepository cancellationpolicyRepository;
    private final CustomerrequestRepository customerrequestRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final com.example.hotelsmartbookingbackend.service.PaypalService paypalService;
    private final com.example.hotelsmartbookingbackend.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${paypal.conversion-rate:25000.0}")
    private BigDecimal conversionRate;

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    @Override
    @Transactional
    public CustomerRequestResponse submitRefundRequest(Integer bookingId, RefundRequest request, String customerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Allow staff/managers or the booking owner
        boolean isStaff = "receptionist".equalsIgnoreCase(customer.getRole().name())
                || "manager".equalsIgnoreCase(customer.getRole().name());

        if (!isStaff && !customer.getId().equals(booking.getUserid().getId())) {
            throw new RuntimeException("Bạn không có quyền yêu cầu hoàn tiền cho đơn đặt phòng này");
        }

        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CHECKED_IN || status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            throw new RuntimeException("Không thể yêu cầu hoàn tiền cho đơn đặt phòng ở trạng thái: " + (status != null ? status.getValue() : "null"));
        }

        if (booking.getPaidamount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Đơn đặt phòng chưa được thanh toán, không thể hoàn tiền");
        }

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        // Calculate days before expected check-in
        LocalDate checkInDate = detail.getExpectedcheckin().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        long daysBetween = ChronoUnit.DAYS.between(today, checkInDate);

        // Calculate estimated refund using policies
        List<Cancellationpolicy> policies = cancellationpolicyRepository.findByIsactiveTrueOrderByPriorityDescDaysbeforecheckinDesc();
        BigDecimal refundPercentage = BigDecimal.ZERO;
        Cancellationpolicy matchedPolicy = null;

        for (Cancellationpolicy policy : policies) {
            if (daysBetween >= policy.getDaysbeforecheckin()) {
                matchedPolicy = policy;
                refundPercentage = policy.getRefundpercentage();
                break;
            }
        }

        if (matchedPolicy == null) {
            if (policies.isEmpty()) {
                refundPercentage = new BigDecimal("100.00"); // default to full refund if no policy is defined
            } else {
                refundPercentage = BigDecimal.ZERO; // non-refundable if none matched
            }
        }

        BigDecimal paidAmount = booking.getPaidamount();
        BigDecimal estimatedRefund = paidAmount.multiply(refundPercentage.divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

        Customerrequest customerRequest = new Customerrequest();
        customerRequest.setBookingid(booking);
        customerRequest.setRequesttype("Refund");
        customerRequest.setDescription(request.getReason());
        customerRequest.setOldvalue(paidAmount.toString());
        customerRequest.setNewvalue(estimatedRefund.toString());
        customerRequest.setStatus("Pending");
        customerRequest.setCreatedat(Instant.now());

        Customerrequest savedRequest = customerrequestRepository.save(customerRequest);

        try {
            String refundMsg = String.format("Khách hàng %s đã yêu cầu hoàn tiền cho đơn đặt phòng %s. Lý do: %s. Số tiền dự kiến: %,.0f VND.", 
                    customer.getFullname(), booking.getBookingreference(), request.getReason(), estimatedRefund);
            notificationService.sendNotificationToRoles(
                    List.of(com.example.hotelsmartbookingbackend.enums.Role.receptionist, com.example.hotelsmartbookingbackend.enums.Role.manager), 
                    "Yêu cầu hoàn tiền mới", 
                    refundMsg, 
                    "Refund", 
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send refund request notification: ", e);
        }

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional
    public CustomerRequestResponse approveRefundRequest(Integer requestId, ApproveRefundRequest request, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu hoàn tiền này đã được xử lý (trạng thái: " + req.getStatus() + ")");
        }

        BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount()
                : new BigDecimal(req.getNewvalue());

        Booking booking = req.getBookingid();
        if (refundAmount.compareTo(booking.getPaidamount()) > 0) {
            throw new RuntimeException("Số tiền hoàn trả không được vượt quá số tiền đã thanh toán: " + booking.getPaidamount());
        }

        // Apply refund to payments
        List<Payment> payments = paymentRepository.findByBookingid_Id(booking.getId());
        BigDecimal remainingRefund = refundAmount;

        for (Payment payment : payments) {
            if (remainingRefund.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal availableToRefund = payment.getAmount().subtract(payment.getRefundedamount());
            if (availableToRefund.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal refundToApply = remainingRefund.min(availableToRefund);
                
                // If payment method is PayPal, trigger real sandbox refund
                if ("PayPal".equalsIgnoreCase(payment.getPaymentmethod()) && payment.getTransactioncode() != null) {
                    try {
                        BigDecimal usdAmount = refundToApply;
                        if (conversionRate != null && conversionRate.compareTo(BigDecimal.ONE) > 0) {
                            usdAmount = refundToApply.divide(conversionRate, 2, RoundingMode.HALF_UP);
                        }
                        log.info("Initiating PayPal Sandbox refund for transaction {}: {} VND ({} USD)", 
                                payment.getTransactioncode(), refundToApply, usdAmount);
                        paypalService.refundPaypalCapture(payment.getTransactioncode(), usdAmount, java.util.UUID.randomUUID().toString());
                    } catch (Exception e) {
                        log.error("Failed to execute PayPal Refund for capture {}: ", payment.getTransactioncode(), e);
                        throw new RuntimeException("Lỗi khi thực hiện hoàn tiền qua cổng PayPal: " + e.getMessage());
                    }
                }
                
                payment.setRefundedamount(payment.getRefundedamount().add(refundToApply));
                paymentRepository.save(payment);
                remainingRefund = remainingRefund.subtract(refundToApply);
            }
        }

        // Update booking
        booking.setPaidamount(booking.getPaidamount().subtract(refundAmount));
        booking.setStatus(BookingStatus.CANCELLED); // Mark as Cancelled because of refund
        booking.setCancelledat(Instant.now());
        booking.setCancelledby(staff);
        booking.setCancellationreason("Refund Approved: " + req.getDescription());
        booking.setUpdatedat(Instant.now());
        bookingRepository.save(booking);

        // Update request
        req.setStatus("Approved");
        req.setResolvedat(Instant.now());
        req.setNewvalue(refundAmount.toString()); // save actual approved refund amount
        Customerrequest savedRequest = customerrequestRepository.save(req);

        try {
            String approveMsg = String.format("Yêu cầu hoàn tiền của bạn cho đơn đặt phòng %s đã được phê duyệt. Số tiền hoàn trả: %,.0f VND.", 
                    booking.getBookingreference(), refundAmount);
            notificationService.sendNotification(booking.getUserid(), "Hoàn tiền được phê duyệt", approveMsg, "Refund", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send refund approved notification: ", e);
        }

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional
    public CustomerRequestResponse rejectRefundRequest(Integer requestId, RejectRefundRequest request, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu hoàn tiền này đã được xử lý (trạng thái: " + req.getStatus() + ")");
        }

        // Update request
        req.setStatus("Rejected");
        req.setResolvedat(Instant.now());
        req.setRejectionreason(request.getRejectionReason());
        Customerrequest savedRequest = customerrequestRepository.save(req);

        try {
            String rejectMsg = String.format("Yêu cầu hoàn tiền của bạn cho đơn đặt phòng %s đã bị từ chối. Lý do: %s.", 
                    req.getBookingid().getBookingreference(), request.getRejectionReason());
            notificationService.sendNotification(req.getBookingid().getUserid(), "Hoàn tiền bị từ chối", rejectMsg, "Refund", req.getBookingid().getId());
        } catch (Exception e) {
            log.error("Failed to send refund rejected notification: ", e);
        }

        return mapToResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestResponse> getPendingRefundRequests(String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        return customerrequestRepository.findByRequesttypeAndStatusOrderByCreatedatDesc("Refund", "Pending")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CustomerRequestResponse mapToResponse(Customerrequest req) {
        return CustomerRequestResponse.builder()
                .requestId(req.getId())
                .bookingId(req.getBookingid().getId())
                .bookingReference(req.getBookingid().getBookingreference())
                .requestType(req.getRequesttype())
                .description(req.getDescription())
                .oldValue(req.getOldvalue())
                .newValue(req.getNewvalue())
                .status(req.getStatus())
                .resolvedAt(req.getResolvedat())
                .rejectionReason(req.getRejectionreason())
                .createdAt(req.getCreatedat())
                .build();
    }
}
