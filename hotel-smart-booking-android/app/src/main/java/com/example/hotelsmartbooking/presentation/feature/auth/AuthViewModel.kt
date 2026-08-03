package com.example.hotelsmartbooking.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.AuthRepository
import com.example.hotelsmartbooking.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn = authRepository.isLoggedInFlow
    val userProfile = authRepository.userProfileFlow

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Vui lòng nhập đầy đủ Email và Mật khẩu")
            return
        }
        viewModelScope.launch {
            authRepository.login(LoginRequest(email, password)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> _uiState.value = AuthUiState.Success("Đăng nhập thành công")
                    is Resource.Error -> _uiState.value = AuthUiState.Error(resource.message)
                }
            }
        }
    }

    fun register(fullName: String, email: String, phone: String, idCardNumber: String, password: String) {
        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || idCardNumber.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        viewModelScope.launch {
            authRepository.register(RegisterRequest(fullName, email, phone, idCardNumber, password)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> _uiState.value = AuthUiState.Success(resource.data)
                    is Resource.Error -> _uiState.value = AuthUiState.Error(resource.message)
                }
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        if (otp.length < 6) {
            _uiState.value = AuthUiState.Error("Mã OTP gồm 6 chữ số")
            return
        }
        viewModelScope.launch {
            authRepository.verifyOtp(VerifyOtpRequest(email, otp)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> _uiState.value = AuthUiState.Success("Xác thực OTP thành công. Vui lòng đăng nhập.")
                    is Resource.Error -> _uiState.value = AuthUiState.Error(resource.message)
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Vui lòng nhập Email tài khoản")
            return
        }
        viewModelScope.launch {
            authRepository.forgotPassword(ForgotPasswordRequest(email)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = AuthUiState.Loading
                    is Resource.Success -> _uiState.value = AuthUiState.Success(resource.data)
                    is Resource.Error -> _uiState.value = AuthUiState.Error(resource.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
