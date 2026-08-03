package com.example.hotelsmartbooking.core.network.api

import com.example.hotelsmartbooking.domain.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface HotelApiService {

    // ── Authentication ────────────────────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<String>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<String>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<String>>

    @POST("auth/verify-forgot-otp")
    suspend fun verifyForgotOtp(@Body request: VerifyForgotOtpRequest): Response<ApiResponse<Map<String, String>>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<String>>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<String>>

    // ── User Profile ──────────────────────────────────────────────────────────
    @GET("users/profile")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserProfile>>

    // ── Room Types & Rooms ───────────────────────────────────────────────────
    @GET("room-types")
    suspend fun getRoomTypes(): Response<ApiResponse<List<RoomTypeSummary>>>

    @GET("room-types/{id}")
    suspend fun getRoomTypeDetail(@Path("id") id: Long): Response<ApiResponse<RoomTypeDetail>>

    @POST("room-types/filter")
    suspend fun filterRoomTypes(@Body criteria: RoomFilterCriteria): Response<ApiResponse<List<RoomTypeSummary>>>

    // ── Feedbacks / Reviews ──────────────────────────────────────────────────
    @GET("feedbacks")
    suspend fun getFeedbacks(@Query("roomTypeId") roomTypeId: Long? = null): Response<ApiResponse<List<FeedbackItem>>>

    @POST("feedbacks")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<ApiResponse<FeedbackItem>>

    // ── Bookings ──────────────────────────────────────────────────────────────
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<ApiResponse<BookingResponse>>

    @GET("bookings/{id}")
    suspend fun getBookingDetail(@Path("id") id: Long): Response<ApiResponse<BookingResponse>>

    @GET("bookings/history")
    suspend fun getBookingHistory(): Response<ApiResponse<List<BookingHistoryResponse>>>

    @DELETE("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") id: Long,
        @Body body: Map<String, String> = mapOf("cancellationReason" to "Khách hàng hủy trên app")
    ): Response<ApiResponse<String>>

    @GET("bookings/{id}/invoice")
    suspend fun getInvoice(@Path("id") id: Long): Response<ApiResponse<InvoiceResponse>>

    @GET("bookings/{id}/statement/pdf")
    suspend fun getStatementPdf(@Path("id") id: Long): Response<ResponseBody>

    // ── Stay Adjustments & Customer Requests ─────────────────────────────────
    @POST("stay-adjustments/extension")
    suspend fun requestExtension(@Body request: ExtensionRequest): Response<ApiResponse<String>>

    @POST("stay-adjustments/room-change")
    suspend fun requestRoomChange(@Body request: RoomChangeRequest): Response<ApiResponse<String>>

    @POST("stay-adjustments/early-checkout")
    suspend fun requestEarlyCheckout(@Body request: EarlyCheckoutRequest): Response<ApiResponse<String>>

    @POST("bookings/{bookingId}/refund-request")
    suspend fun requestRefund(
        @Path("bookingId") bookingId: Long,
        @Body request: RefundRequest
    ): Response<ApiResponse<String>>

    // ── Check-In & QR & eKYC ─────────────────────────────────────────────────
    @POST("checkin/{bookingId}/qr-token")
    suspend fun generateQrToken(@Path("bookingId") bookingId: Long): Response<ApiResponse<QrTokenResponse>>

    @POST("checkin/qr")
    suspend fun checkInWithQr(@Body request: QrCheckInRequest): Response<ApiResponse<String>>

    @GET("v1/ekyc/status")
    suspend fun getEkycStatus(): Response<ApiResponse<EkycStatusResponse>>

    @Multipart
    @POST("v1/ekyc/verify")
    suspend fun submitEkyc(
        @Part frontImage: MultipartBody.Part,
        @Part backImage: MultipartBody.Part,
        @Part selfieImage: MultipartBody.Part,
        @Part leftImage: MultipartBody.Part,
        @Part rightImage: MultipartBody.Part,
        @Part upImage: MultipartBody.Part,
        @Part downImage: MultipartBody.Part
    ): Response<ApiResponse<EkycStatusResponse>>

    @Multipart
    @POST("v1/ekyc/face/validate-frame")
    suspend fun validateFaceFrame(
        @Part frameImage: MultipartBody.Part,
        @Part("step") step: RequestBody,
        @Part referenceImage: MultipartBody.Part? = null,
        @Part oppositeImage: MultipartBody.Part? = null
    ): Response<ApiResponse<AiFaceFrameValidationResponse>>

    // ── Payments ─────────────────────────────────────────────────────────────
    @POST("payments/paypal/create-order")
    suspend fun createPaypalOrder(@Body request: PaymentPaypalCreateOrderRequest): Response<ApiResponse<PaymentPaypalOrderResponse>>

    @POST("payments/paypal/capture-order")
    suspend fun capturePaypalOrder(@Body request: PaymentPaypalCaptureRequest): Response<ApiResponse<String>>

    @POST("payments/vnpay/create-payment-url")
    suspend fun createVnPayUrl(@Body request: PaymentVnPayRequest): Response<ApiResponse<PaymentVnPayResponse>>

    // ── Notifications ────────────────────────────────────────────────────────
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationResponse>>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<Map<String, Int>>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<ApiResponse<String>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<String>>
}
