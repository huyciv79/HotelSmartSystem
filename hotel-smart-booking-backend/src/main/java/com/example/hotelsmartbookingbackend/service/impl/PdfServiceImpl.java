package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.PdfService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final BookingService bookingService;

    @Override
    public byte[] generateInvoicePdf(Integer bookingId, String actorEmail) {
        InvoiceResponse invoice = bookingService.getInvoiceDetails(bookingId, actorEmail);
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font mainTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(43, 60, 80));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(43, 60, 80));
            Font regularBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // Document Header
            Paragraph title = new Paragraph("ELYSIAN HOTEL - INVOICE STATEMENT", mainTitleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Info Section (Hotel vs Guest Details)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            // Left: Hotel info
            PdfPCell hotelCell = new PdfPCell();
            hotelCell.setBorder(PdfPCell.NO_BORDER);
            hotelCell.addElement(new Paragraph("Elysian Kanther Hotel", subTitleFont));
            hotelCell.addElement(new Paragraph("123 Vo Nguyen Giap St, Danang, Vietnam", regularFont));
            hotelCell.addElement(new Paragraph("Phone: +84 236 3999 999", regularFont));
            hotelCell.addElement(new Paragraph("Email: contact@elysiankanther.com", regularFont));
            infoTable.addCell(hotelCell);

            // Right: Invoice & Guest info
            PdfPCell guestCell = new PdfPCell();
            guestCell.setBorder(PdfPCell.NO_BORDER);
            guestCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            guestCell.addElement(new Paragraph("Invoice No: " + invoice.getBookingReference(), subTitleFont));
            guestCell.addElement(new Paragraph("Booking Type: " + invoice.getBookingType(), regularFont));
            guestCell.addElement(new Paragraph("Guest: " + invoice.getCustomerName(), regularFont));
            guestCell.addElement(new Paragraph("Email: " + invoice.getCustomerEmail(), regularFont));
            guestCell.addElement(new Paragraph("Phone: " + invoice.getCustomerPhone(), regularFont));
            infoTable.addCell(guestCell);

            document.add(infoTable);

            // Stay Details Table
            Paragraph stayTitle = new Paragraph("STAY INFORMATION", sectionTitleFont);
            stayTitle.setSpacingAfter(8);
            document.add(stayTitle);

            PdfPTable stayTable = new PdfPTable(4);
            stayTable.setWidthPercentage(100);
            stayTable.setSpacingAfter(20);

            addTableHeader(stayTable, "Check-in Date", regularBoldFont);
            addTableHeader(stayTable, "Check-out Date", regularBoldFont);
            addTableHeader(stayTable, "Room Type", regularBoldFont);
            addTableHeader(stayTable, "Assigned Room(s)", regularBoldFont);

            stayTable.addCell(new PdfPCell(new Phrase(invoice.getCheckInDate().format(displayFormatter), regularFont)));
            stayTable.addCell(new PdfPCell(new Phrase(invoice.getCheckOutDate().format(displayFormatter), regularFont)));
            stayTable.addCell(new PdfPCell(new Phrase(invoice.getRoomTypeName() + " (x" + invoice.getQuantity() + ")", regularFont)));
            String assignedText = String.join(", ", invoice.getAssignedRooms());
            if (assignedText.isBlank()) {
                assignedText = "Pending Assignment";
            }
            stayTable.addCell(new PdfPCell(new Phrase(assignedText, regularFont)));

            document.add(stayTable);

            // Invoice Breakdown Table
            Paragraph breakdownTitle = new Paragraph("CHARGES BREAKDOWN", sectionTitleFont);
            breakdownTitle.setSpacingAfter(8);
            document.add(breakdownTitle);

            PdfPTable chargesTable = new PdfPTable(4);
            chargesTable.setWidthPercentage(100);
            chargesTable.setSpacingAfter(20);
            chargesTable.setWidths(new float[]{4.0f, 1.0f, 1.5f, 1.5f});

            addTableHeader(chargesTable, "Description", regularBoldFont);
            addTableHeader(chargesTable, "Qty", regularBoldFont);
            addTableHeader(chargesTable, "Unit Price", regularBoldFont);
            addTableHeader(chargesTable, "Total Price", regularBoldFont);

            // Room Charges
            chargesTable.addCell(new PdfPCell(new Phrase("Room rate: " + invoice.getRoomTypeName() + " (" + invoice.getNights() + " nights)", regularFont)));
            chargesTable.addCell(new PdfPCell(new Phrase(String.valueOf(invoice.getQuantity() * invoice.getNights()), regularFont)));
            chargesTable.addCell(new PdfPCell(new Phrase(formatCurrency(invoice.getRoomRate()), regularFont)));
            chargesTable.addCell(new PdfPCell(new Phrase(formatCurrency(invoice.getRoomTotal()), regularFont)));

            // Service Charges
            if (invoice.getServices() != null && !invoice.getServices().isEmpty()) {
                for (InvoiceResponse.ServiceChargeItem service : invoice.getServices()) {
                    chargesTable.addCell(new PdfPCell(new Phrase(service.getServiceName(), regularFont)));
                    chargesTable.addCell(new PdfPCell(new Phrase(String.valueOf(service.getQuantity()), regularFont)));
                    chargesTable.addCell(new PdfPCell(new Phrase(formatCurrency(service.getUnitPrice()), regularFont)));
                    chargesTable.addCell(new PdfPCell(new Phrase(formatCurrency(service.getTotalPrice()), regularFont)));
                }
            }

            // Subtotal & Tax/Discount Summary
            addSummaryRow(chargesTable, "Subtotal Room Total:", formatCurrency(invoice.getRoomTotal()), regularBoldFont);
            if (invoice.getServiceTotal() != null && invoice.getServiceTotal().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(chargesTable, "Subtotal Services:", formatCurrency(invoice.getServiceTotal()), regularBoldFont);
            }
            addSummaryRow(chargesTable, "VAT / Tax (10%):", formatCurrency(invoice.getTaxAmount()), regularBoldFont);
            if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(chargesTable, "Discounts:", "-" + formatCurrency(invoice.getDiscountAmount()), regularBoldFont);
            }
            addSummaryRow(chargesTable, "Grand Total:", formatCurrency(invoice.getFinalAmount()), regularBoldFont);
            addSummaryRow(chargesTable, "Paid Amount:", formatCurrency(invoice.getPaidAmount()), regularBoldFont);
            addSummaryRow(chargesTable, "Balance Due:", formatCurrency(invoice.getDueAmount()), regularBoldFont);

            document.add(chargesTable);

            // Payments Summary
            if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
                Paragraph paymentsTitle = new Paragraph("PAYMENT HISTORY", sectionTitleFont);
                paymentsTitle.setSpacingAfter(8);
                document.add(paymentsTitle);

                PdfPTable paymentsTable = new PdfPTable(5);
                paymentsTable.setWidthPercentage(100);
                paymentsTable.setSpacingAfter(20);
                paymentsTable.setWidths(new float[]{2.0f, 1.5f, 1.5f, 2.5f, 1.5f});

                addTableHeader(paymentsTable, "Date", regularBoldFont);
                addTableHeader(paymentsTable, "Method", regularBoldFont);
                addTableHeader(paymentsTable, "Type", regularBoldFont);
                addTableHeader(paymentsTable, "Transaction Code", regularBoldFont);
                addTableHeader(paymentsTable, "Amount", regularBoldFont);

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("Asia/Bangkok"));

                for (InvoiceResponse.PaymentItem payment : invoice.getPayments()) {
                    paymentsTable.addCell(new PdfPCell(new Phrase(timeFormatter.format(payment.getPaymentDate()), regularFont)));
                    paymentsTable.addCell(new PdfPCell(new Phrase(payment.getPaymentMethod(), regularFont)));
                    paymentsTable.addCell(new PdfPCell(new Phrase(payment.getPaymentType(), regularFont)));
                    paymentsTable.addCell(new PdfPCell(new Phrase(payment.getTransactionCode(), regularFont)));
                    paymentsTable.addCell(new PdfPCell(new Phrase(formatCurrency(payment.getAmount()), regularFont)));
                }

                document.add(paymentsTable);
            }

            // Footer / Thank You Note
            Paragraph footer = new Paragraph("\nThank you for choosing Elysian Kanther Hotel. We hope you enjoyed your stay!\nFor any billing inquiries, please email contact@elysiankanther.com.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            log.error("Error generating invoice PDF statement: ", e);
            throw new RuntimeException("Lỗi khi kết xuất hóa đơn PDF", e);
        }

        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell(new Phrase(headerTitle, font));
        header.setBackgroundColor(new Color(240, 243, 246));
        header.setHorizontalAlignment(Element.ALIGN_LEFT);
        header.setPadding(6);
        table.addCell(header);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setColspan(3);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(new Color(250, 251, 252));
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, font));
        valCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valCell.setPadding(6);
        valCell.setBackgroundColor(new Color(250, 251, 252));
        table.addCell(valCell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
    }
}
