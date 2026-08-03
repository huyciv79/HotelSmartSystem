package com.example.hotelsmartbooking.data.repository

import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.core.network.api.HotelApiService
import com.example.hotelsmartbooking.data.local.datastore.UserPreferences
import com.example.hotelsmartbooking.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun login(request: LoginRequest): Flow<Resource<LoginResponse>>
    suspend fun register(request: RegisterRequest): Flow<Resource<String>>
    suspend fun verifyOtp(request: VerifyOtpRequest): Flow<Resource<String>>
    suspend fun forgotPassword(request: ForgotPasswordRequest): Flow<Resource<String>>
    suspend fun verifyForgotOtp(request: VerifyForgotOtpRequest): Flow<Resource<String>>
    suspend fun resetPassword(request: ResetPasswordRequest): Flow<Resource<String>>
    suspend fun changePassword(request: ChangePasswordRequest): Flow<Resource<String>>
    suspend fun getProfile(): Flow<Resource<UserProfile>>
    suspend fun logout()
    val userProfileFlow: Flow<UserProfile?>
    val isLoggedInFlow: Flow<Boolean>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: HotelApiService,
    private val userPreferences: UserPreferences,
    private val gson: Gson
) : AuthRepository {

    override val isLoggedInFlow: Flow<Boolean> = userPreferences.accessToken.map { token ->
        !token.isNullOrBlank()
    }

    override val userProfileFlow: Flow<UserProfile?> = userPreferences.userJson.map { json ->
        if (json.isNullOrEmpty()) null else try { gson.fromJson(json, UserProfile::class.java) } catch (e: Exception) { null }
    }

    override suspend fun login(request: LoginRequest): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val apiRes = response.body()!!
                val loginData = apiRes.data
                if (loginData != null) {
                    userPreferences.saveAuthTokens(loginData.token)
                    userPreferences.saveUserJson(gson.toJson(loginData.user))
                    emit(Resource.Success(loginData))
                } else {
                    emit(Resource.Error(apiRes.message.ifBlank { "Đăng nhập thất bại" }))
                }
            } else {
                emit(Resource.Error("Email hoặc mật khẩu không chính xác (${response.code()})"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối máy chủ", e))
        }
    }

    override suspend fun register(request: RegisterRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Đăng ký thất bại. Email hoặc SĐT đã tồn tại."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi hệ thống", e))
        }
    }

    override suspend fun verifyOtp(request: VerifyOtpRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.verifyOtp(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Mã OTP không hợp lệ hoặc đã hết hạn"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.forgotPassword(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Không tìm thấy tài khoản với Email này"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun verifyForgotOtp(request: VerifyForgotOtpRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.verifyForgotOtp(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val resetToken = response.body()!!.data!!["resetToken"] ?: ""
                emit(Resource.Success(resetToken))
            } else {
                emit(Resource.Error("Mã OTP không đúng hoặc hết hạn"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.resetPassword(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Đặt lại mật khẩu không thành công"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.changePassword(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.message))
            } else {
                emit(Resource.Error("Mật khẩu hiện tại không chính xác"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun getProfile(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body()?.data != null) {
                val profile = response.body()!!.data!!
                userPreferences.saveUserJson(gson.toJson(profile))
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error("Không thể tải thông tin cá nhân"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối", e))
        }
    }

    override suspend fun logout() {
        userPreferences.clearAuth()
    }
}
