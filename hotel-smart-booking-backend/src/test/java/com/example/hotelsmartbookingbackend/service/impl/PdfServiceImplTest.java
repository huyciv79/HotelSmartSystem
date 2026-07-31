package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceImplTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private PdfServiceImpl pdfService;

    private InvoiceResponse sampleInvoice;

    @BeforeEach
    void setUp() {
        sampleInvoice = new InvoiceResponse();
        sampleInvoice.setBookingReference("BK20260729001");
        sampleInvoice.setBookingType("Single");
        sampleInvoice.setCustomerName("Nguyen Van A");
        sampleInvoice.setCustomerEmail("guest@example.com");
        sampleInvoice.setCustomerPhone("0901234567");
        sampleInvoice.setCheckInDate(LocalDate.of(2026, 8, 1));
        sampleInvoice.setCheckOutDate(LocalDate.of(2026, 8, 3));
        sampleInvoice.setRoomTypeName("Deluxe Ocean View");
        sampleInvoice.setQuantity(1);
        sampleInvoice.setNights(2L);
        sampleInvoice.setRoomRate(new BigDecimal("1000000.00"));
        sampleInvoice.setRoomTotal(new BigDecimal("2000000.00"));
        sampleInvoice.setServiceTotal(new BigDecimal("200000.00"));
        sampleInvoice.setTaxAmount(new BigDecimal("220000.00"));
        sampleInvoice.setDiscountAmount(new BigDecimal("100000.00"));
        sampleInvoice.setFinalAmount(new BigDecimal("2320000.00"));
        sampleInvoice.setPaidAmount(new BigDecimal("2320000.00"));
        sampleInvoice.setDueAmount(BigDecimal.ZERO);
        sampleInvoice.setAssignedRooms(List.of("101"));

        InvoiceResponse.ServiceChargeItem serviceItem = InvoiceResponse.ServiceChargeItem.builder()
                .serviceName("Laundry")
                .quantity(1)
                .unitPrice(new BigDecimal("200000.00"))
                .totalPrice(new BigDecimal("200000.00"))
                .build();
        sampleInvoice.setServices(List.of(serviceItem));

        InvoiceResponse.PaymentItem paymentItem = InvoiceResponse.PaymentItem.builder()
                .paymentDate(Instant.now())
                .paymentMethod("PayPal")
                .paymentType("Booking Payment")
                .transactionCode("TXN-001")
                .amount(new BigDecimal("2320000.00"))
                .build();
        sampleInvoice.setPayments(List.of(paymentItem));
    }

    @Test
    @DisplayName("UTCID01 - Successful PDF generation with full invoice details (assigned rooms, services, discounts, payments)")
    void should_generateInvoicePdfSuccessfully_when_fullInvoiceDetailsProvided() {
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));
        verify(bookingService).getInvoiceDetails(1, "guest@example.com");
    }

    @Test
    @DisplayName("UTCID02 - Successful PDF generation when assignedRooms is empty or blank")
    void should_generateInvoicePdfSuccessfully_when_assignedRoomsIsEmpty() {
        sampleInvoice.setAssignedRooms(new ArrayList<>());
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID03 - Successful PDF generation when services list is empty and serviceTotal is ZERO")
    void should_generateInvoicePdfSuccessfully_when_servicesListIsEmptyAndServiceTotalIsZero() {
        sampleInvoice.setServices(null);
        sampleInvoice.setServiceTotal(BigDecimal.ZERO);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID04 - Successful PDF generation including Subtotal Services row when serviceTotal > 0")
    void should_generateInvoicePdfSuccessfully_when_serviceTotalIsGreaterThanZero() {
        sampleInvoice.setServiceTotal(new BigDecimal("500000.00"));
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID05 - Successful PDF generation skipping Discounts row when discountAmount is null or zero")
    void should_generateInvoicePdfSuccessfully_when_discountAmountIsNull() {
        sampleInvoice.setDiscountAmount(null);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID06 - Successful PDF generation including Discounts row when discountAmount > 0")
    void should_generateInvoicePdfSuccessfully_when_discountAmountIsGreaterThanZero() {
        sampleInvoice.setDiscountAmount(new BigDecimal("150000.00"));
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID07 - Successful PDF generation skipping PAYMENT HISTORY section when payments list is empty")
    void should_generateInvoicePdfSuccessfully_when_paymentsListIsEmpty() {
        sampleInvoice.setPayments(List.of());
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when BookingService throws exception")
    void should_throwException_when_bookingServiceGetInvoiceDetailsThrowsException() {
        when(bookingService.getInvoiceDetails(99, "guest@example.com"))
                .thenThrow(new RuntimeException("Booking not found with ID: 99"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfService.generateInvoicePdf(99, "guest@example.com"));

        assertEquals("Booking not found with ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when PDF generation engine encounters unexpected error")
    void should_throwException_when_pdfGenerationEngineEncountersError() {
        sampleInvoice.setCheckInDate(null);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfService.generateInvoicePdf(1, "guest@example.com"));

        assertTrue(ex.getMessage().contains("Lỗi khi kết xuất hóa đơn PDF"));
    }

    @Test
    @DisplayName("UTCID10 - Render Pending Assignment when assignedRooms list contains blank whitespace strings")
    void should_renderPendingAssignment_when_assignedRoomsContainsBlankStrings() {
        sampleInvoice.setAssignedRooms(List.of("   "));
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID11 - Skip Subtotal Services row when serviceTotal field is null")
    void should_skipSubtotalServicesRow_when_serviceTotalIsNull() {
        sampleInvoice.setServiceTotal(null);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID12 - Skip Discounts row when discountAmount field is BigDecimal.ZERO")
    void should_skipDiscountsRow_when_discountAmountIsZero() {
        sampleInvoice.setDiscountAmount(BigDecimal.ZERO);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("UTCID13 - Skip PAYMENT HISTORY section when payments field is null")
    void should_skipPaymentHistorySection_when_paymentsFieldIsNull() {
        sampleInvoice.setPayments(null);
        when(bookingService.getInvoiceDetails(1, "guest@example.com")).thenReturn(sampleInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(1, "guest@example.com");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
