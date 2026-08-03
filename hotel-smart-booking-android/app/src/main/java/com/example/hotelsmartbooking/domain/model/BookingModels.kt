package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class CreateBookingRequest(
    @SerializedName("roomTypeId") val roomTypeId: Long,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("adults") val adults: Int = 1,
    @SerializedName("children") val children: Int = 0,
    @SerializedName("checkInMethod") val checkInMethod: String = "Manual",
    @SerializedName("specialRequests") val specialRequests: String = "",
    @SerializedName("voucherCode") val voucherCode: String? = null
)

data class BookingResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bookingReference") val bookingReference: String,
    @SerializedName("roomTypeName") val roomTypeName: String,
    @SerializedName("roomTypeId") val roomTypeId: Long,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("checkInMethod") val checkInMethod: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("paidAmount") val paidAmount: Double = 0.0,
    @SerializedName("specialRequests") val specialRequests: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class BookingHistoryResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bookingReference") val bookingReference: String,
    @SerializedName("roomTypeName") val roomTypeName: String,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("paidAmount") val paidAmount: Double,
    @SerializedName("thumbnail") val thumbnail: String? = null
)

data class QrTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expiresAt") val expiresAt: String
)

data class QrCheckInRequest(
    @SerializedName("token") val token: String
)

data class InvoiceResponse(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("bookingReference") val bookingReference: String,
    @SerializedName("guestName") val guestName: String,
    @SerializedName("roomTypeName") val roomTypeName: String,
    @SerializedName("roomNumber") val roomNumber: String? = null,
    @SerializedName("checkInDate") val checkInDate: String,
    @SerializedName("checkOutDate") val checkOutDate: String,
    @SerializedName("roomTotal") val roomTotal: Double,
    @SerializedName("serviceTotal") val serviceTotal: Double = 0.0,
    @SerializedName("taxAmount") val taxAmount: Double = 0.0,
    @SerializedName("discountAmount") val discountAmount: Double = 0.0,
    @SerializedName("finalAmount") val finalAmount: Double,
    @SerializedName("paidAmount") val paidAmount: Double,
    @SerializedName("dueAmount") val dueAmount: Double = finalAmount - paidAmount,
    @SerializedName("services") val services: List<BookingServiceItem> = emptyList()
)

data class BookingServiceItem(
    @SerializedName("id") val id: Long,
    @SerializedName("serviceName") val serviceName: String,
    @SerializedName("unitPrice") val unitPrice: Double,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("totalPrice") val totalPrice: Double
)

data class ExtensionRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("newCheckOutDate") val newCheckOutDate: String,
    @SerializedName("note") val note: String = ""
)

data class RoomChangeRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("targetRoomTypeId") val targetRoomTypeId: Long,
    @SerializedName("reason") val reason: String = ""
)

data class EarlyCheckoutRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("newCheckOutDate") val newCheckOutDate: String,
    @SerializedName("reason") val reason: String = ""
)

data class RefundRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("reason") val reason: String
)
