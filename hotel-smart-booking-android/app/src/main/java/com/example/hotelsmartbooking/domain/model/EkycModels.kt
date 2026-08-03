package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class EkycStatusResponse(
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("idCardNumber") val idCardNumber: String? = null,
    @SerializedName("frontImageUrl") val frontImageUrl: String? = null,
    @SerializedName("backImageUrl") val backImageUrl: String? = null,
    @SerializedName("selfieImageUrl") val selfieImageUrl: String? = null,
    @SerializedName("verificationDate") val verificationDate: String? = null
)

data class AiFaceFrameValidationResponse(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("step") val step: String,
    @SerializedName("message") val message: String,
    @SerializedName("confidence") val confidence: Float = 0f
)

data class AiFaceReadinessResponse(
    @SerializedName("ready") val ready: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("faceCount") val faceCount: Int = 0
)

data class PaymentPaypalCreateOrderRequest(
    @SerializedName("bookingId") val bookingId: Long
)

data class PaymentPaypalOrderResponse(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("approvalUrl") val approvalUrl: String
)

data class PaymentPaypalCaptureRequest(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("bookingId") val bookingId: Long
)

data class PaymentVnPayRequest(
    @SerializedName("bookingId") val bookingId: Long,
    @SerializedName("bankCode") val bankCode: String? = null
)

data class PaymentVnPayResponse(
    @SerializedName("paymentUrl") val paymentUrl: String
)
