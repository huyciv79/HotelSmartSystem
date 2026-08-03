package com.example.hotelsmartbooking.presentation.feature.hotel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.RoomRepository
import com.example.hotelsmartbooking.domain.model.RoomTypeDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoomDetailUiState {
    object Loading : RoomDetailUiState()
    data class Success(val roomDetail: RoomTypeDetail) : RoomDetailUiState()
    data class Error(val message: String) : RoomDetailUiState()
}

@HiltViewModel
class RoomTypeDetailViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoomDetailUiState>(RoomDetailUiState.Loading)
    val uiState: StateFlow<RoomDetailUiState> = _uiState.asStateFlow()

    fun loadRoomTypeDetail(roomTypeId: Long) {
        viewModelScope.launch {
            roomRepository.getRoomTypeDetail(roomTypeId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = RoomDetailUiState.Loading
                    is Resource.Success -> _uiState.value = RoomDetailUiState.Success(resource.data)
                    is Resource.Error -> _uiState.value = RoomDetailUiState.Error(resource.message)
                }
            }
        }
    }
}
