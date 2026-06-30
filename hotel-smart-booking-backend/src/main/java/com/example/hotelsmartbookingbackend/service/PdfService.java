package com.example.hotelsmartbookingbackend.service;

public interface PdfService {
    byte[] generateInvoicePdf(Integer bookingId, String actorEmail);
}
