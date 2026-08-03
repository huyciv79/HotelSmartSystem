package com.example.hotelsmartbooking.presentation.feature.ekyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.EkycRepository
import com.example.hotelsmartbooking.domain.model.EkycStatusResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class EkycUiState {
    object Idle : EkycUiState()
    object Loading : EkycUiState()
    data class StatusSuccess(val status: EkycStatusResponse) : EkycUiState()
    data class SubmitSuccess(val message: String) : EkycUiState()
    data class Error(val message: String) : EkycUiState()
}

@HiltViewModel
class EkycViewModel @Inject constructor(
    private val ekycRepository: EkycRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EkycUiState>(EkycUiState.Idle)
    val uiState: StateFlow<EkycUiState> = _uiState.asStateFlow()

    fun loadStatus() {
        viewModelScope.launch {
            ekycRepository.getEkycStatus().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = EkycUiState.Loading
                    is Resource.Success -> _uiState.value = EkycUiState.StatusSuccess(resource.data)
                    is Resource.Error -> _uiState.value = EkycUiState.Error(resource.message)
                }
            }
        }
    }

    fun submitEkyc(
        frontFile: File,
        backFile: File,
        selfieFile: File,
        leftFile: File,
        rightFile: File,
        upFile: File,
        downFile: File
    ) {
        viewModelScope.launch {
            ekycRepository.submitEkyc(frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = EkycUiState.Loading
                    is Resource.Success -> _uiState.value = EkycUiState.SubmitSuccess("Xác thực eKYC thành công!")
                    is Resource.Error -> _uiState.value = EkycUiState.Error(resource.message)
                }
            }
        }
    }
}
