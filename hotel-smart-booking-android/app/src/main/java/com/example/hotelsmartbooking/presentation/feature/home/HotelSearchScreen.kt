package com.example.hotelsmartbooking.presentation.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hotelsmartbooking.domain.model.RoomFilterCriteria
import com.example.hotelsmartbooking.presentation.components.EmptyStateView
import com.example.hotelsmartbooking.presentation.components.ErrorStateView
import com.example.hotelsmartbooking.presentation.components.RoomCardShimmer

@Composable
fun HotelSearchScreen(
    viewModel: HomeViewModel,
    onNavigateToRoomDetail: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Header Bar
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tìm kiếm loại phòng",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Nhập tên phòng, tiện nghi...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { /* Open Filter Sheet */ }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Bộ lọc")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                when (val state = uiState) {
                    is RoomListUiState.Loading -> {
                        items(4) {
                            RoomCardShimmer()
                        }
                    }
                    is RoomListUiState.Error -> {
                        item {
                            ErrorStateView(
                                message = state.message,
                                onRetry = { viewModel.loadFeaturedRooms() }
                            )
                        }
                    }
                    is RoomListUiState.Success -> {
                        val filteredRooms = state.rooms.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredRooms.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "Không tìm thấy phòng phù hợp",
                                    message = "Thử thay đổi từ khóa hoặc điều chỉnh bộ lọc."
                                )
                            }
                        } else {
                            items(filteredRooms) { room ->
                                RoomCardItem(
                                    room = room,
                                    onClick = { onNavigateToRoomDetail(room.id) },
                                    onFavoriteClick = { viewModel.toggleFavorite(room) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
