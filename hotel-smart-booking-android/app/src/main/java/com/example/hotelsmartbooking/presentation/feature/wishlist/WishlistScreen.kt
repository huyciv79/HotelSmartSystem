package com.example.hotelsmartbooking.presentation.feature.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hotelsmartbooking.data.local.db.entity.RoomTypeEntity
import com.example.hotelsmartbooking.domain.model.RoomTypeSummary
import com.example.hotelsmartbooking.presentation.components.ElysianTopAppBar
import com.example.hotelsmartbooking.presentation.components.EmptyStateView
import com.example.hotelsmartbooking.presentation.feature.home.HomeViewModel
import com.example.hotelsmartbooking.presentation.feature.home.RoomCardItem

@Composable
fun WishlistScreen(
    viewModel: HomeViewModel,
    onNavigateToRoomDetail: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            ElysianTopAppBar(title = "Danh sách yêu thích")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            EmptyStateView(
                title = "Chưa có phòng yêu thích",
                message = "Bấm biểu tượng trái tim ở các phòng bạn quan tâm để lưu lại tại đây."
            )
        }
    }
}
