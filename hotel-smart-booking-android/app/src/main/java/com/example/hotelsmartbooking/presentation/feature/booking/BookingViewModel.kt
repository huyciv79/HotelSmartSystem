package com.example.hotelsmartbooking.presentation.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.BookingRepository
import com.example.hotelsmartbooking.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    data class CreatedSuccess(val booking: BookingResponse) : BookingUiState()
    data class HistorySuccess(val bookings: List<BookingHistoryResponse>) : BookingUiState()
    data class DetailSuccess(val booking: BookingResponse) : BookingUiState()
    data class QrTokenSuccess(val tokenResponse: QrTokenResponse) : BookingUiState()
    data class ActionSuccess(val message: String) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun createBooking(
        roomTypeId: Long,
        checkInDate: String,
        checkOutDate: String,
        adults: Int,
        children: Int,
        checkInMethod: String,
        specialRequests: String
    ) {
        viewModelScope.launch {
            bookingRepository.createBooking(
                CreateBookingRequest(
                    roomTypeId = roomTypeId,
                    checkInDate = checkInDate,
                    checkOutDate = checkOutDate,
                    adults = adults,
                    children = children,
                    checkInMethod = checkInMethod,
                    specialRequests = specialRequests
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.CreatedSuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun loadBookingHistory() {
        viewModelScope.launch {
            bookingRepository.getBookingHistory().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.HistorySuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun loadBookingDetail(bookingId: Long) {
        viewModelScope.launch {
            bookingRepository.getBookingDetail(bookingId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.DetailSuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun generateQrToken(bookingId: Long) {
        viewModelScope.launch {
            bookingRepository.generateQrToken(bookingId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.QrTokenSuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun cancelBooking(bookingId: Long, reason: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId, reason).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.ActionSuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun requestExtension(bookingId: Long, newDate: String, note: String) {
        viewModelScope.launch {
            bookingRepository.requestExtension(ExtensionRequest(bookingId, newDate, note)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = BookingUiState.Loading
                    is Resource.Success -> _uiState.value = BookingUiState.ActionSuccess(resource.data)
                    is Resource.Error -> _uiState.value = BookingUiState.Error(resource.message)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = BookingUiState.Idle
    }
}
