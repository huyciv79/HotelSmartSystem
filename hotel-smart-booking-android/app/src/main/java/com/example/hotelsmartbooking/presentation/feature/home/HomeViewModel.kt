package com.example.hotelsmartbooking.presentation.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.RoomRepository
import com.example.hotelsmartbooking.domain.model.RoomTypeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoomListUiState {
    object Loading : RoomListUiState()
    data class Success(val rooms: List<RoomTypeSummary>) : RoomListUiState()
    data class Error(val message: String) : RoomListUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoomListUiState>(RoomListUiState.Loading)
    val uiState: StateFlow<RoomListUiState> = _uiState.asStateFlow()

    init {
        loadFeaturedRooms()
    }

    fun loadFeaturedRooms() {
        viewModelScope.launch {
            roomRepository.getRoomTypes().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = RoomListUiState.Loading
                    is Resource.Success -> _uiState.value = RoomListUiState.Success(resource.data)
                    is Resource.Error -> _uiState.value = RoomListUiState.Error(resource.message)
                }
            }
        }
    }

    fun toggleFavorite(room: RoomTypeSummary) {
        viewModelScope.launch {
            roomRepository.toggleFavorite(room)
        }
    }
}
