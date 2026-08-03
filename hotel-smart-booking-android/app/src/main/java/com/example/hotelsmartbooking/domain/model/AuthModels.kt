package com.example.hotelsmartbooking.domain.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserProfile
)

data class UserProfile(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("fullName") val fullName: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("idCardNumber") val idCardNumber: String = "",
    @SerializedName("role") val role: String = "customer",
    @SerializedName("loyaltyPoints") val loyaltyPoints: Int = 0,
    @SerializedName("membershipTier") val membershipTier: String = "Silver",
    @SerializedName("ekycVerified") val ekycVerified: Boolean = false,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class RegisterRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("idCardNumber") val idCardNumber: String,
    @SerializedName("password") val password: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class VerifyForgotOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ResetPasswordRequest(
    @SerializedName("resetToken") val resetToken: String,
    @SerializedName("newPassword") val newPassword: String
)

data class ChangePasswordRequest(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)
