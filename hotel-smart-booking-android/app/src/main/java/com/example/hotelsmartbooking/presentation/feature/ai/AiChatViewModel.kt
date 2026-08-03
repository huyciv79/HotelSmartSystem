package com.example.hotelsmartbooking.presentation.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotelsmartbooking.core.common.Resource
import com.example.hotelsmartbooking.data.repository.AiRepository
import com.example.hotelsmartbooking.domain.model.BookingStateSchema
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(sender = "ai", text = "Xin chào! Tôi là Trợ lý AI Elysian. Tôi có thể giúp bạn tìm phòng, xem thông tin resort hoặc hướng dẫn đặt phòng. Bạn cần hỗ trợ gì ạ?")
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentState = MutableStateFlow(BookingStateSchema())
    val currentState: StateFlow<BookingStateSchema> = _currentState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = "user", text = userText)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            aiRepository.chat(userText, _currentState.value).collect { resource ->
                _isLoading.value = false
                when (resource) {
                    is Resource.Success -> {
                        val responseData = resource.data
                        _currentState.value = responseData.state
                        val aiMsg = ChatMessage(sender = "ai", text = responseData.response)
                        _messages.value = _messages.value + aiMsg
                    }
                    is Resource.Error -> {
                        val errorMsg = ChatMessage(sender = "ai", text = resource.message)
                        _messages.value = _messages.value + errorMsg
                    }
                    else -> {}
                }
            }
        }
    }
}
