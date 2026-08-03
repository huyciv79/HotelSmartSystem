package com.example.hotelsmartbooking.presentation.feature.hotel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hotelsmartbooking.domain.model.RoomTypeDetail
import com.example.hotelsmartbooking.presentation.components.ElysianTopAppBar
import com.example.hotelsmartbooking.presentation.components.ErrorStateView
import com.example.hotelsmartbooking.presentation.components.RoomCardShimmer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RoomTypeDetailScreen(
    roomTypeId: Long,
    viewModel: RoomTypeDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    LaunchedEffect(roomTypeId) {
        viewModel.loadRoomTypeDetail(roomTypeId)
    }

    Scaffold(
        topBar = {
            ElysianTopAppBar(
                title = "Chi tiết hạng phòng",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (uiState is RoomDetailUiState.Success) {
                val room = (uiState as RoomDetailUiState.Success).roomDetail
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Giá mỗi đêm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(room.dynamicPrice ?: room.basePrice),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { onNavigateToCheckout(room.id) },
                            modifier = Modifier
                                .height(48.dp)
                                .width(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("ĐẶT PHÒNG", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is RoomDetailUiState.Loading -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RoomCardShimmer()
                    }
                }
                is RoomDetailUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetry = { viewModel.loadRoomTypeDetail(roomTypeId) }
                    )
                }
                is RoomDetailUiState.Success -> {
                    val room = state.roomDetail
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Image Gallery Slider
                        item {
                            val sampleImages = if (room.images.isNotEmpty()) room.images else listOf(
                                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=800",
                                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=800"
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            ) {
                                items(sampleImages) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = room.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(320.dp)
                                            .fillMaxHeight()
                                            .padding(end = 8.dp)
                                    )
                                }
                            }
                        }

                        // Room Info Section
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sức chứa: ${room.adultCapacity} người lớn, ${room.childCapacity} trẻ em (Tối đa ${room.totalCapacity} khách)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Mô tả phòng",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = room.description ?: "Trải nghiệm không gian sang trọng, hiện đại đầy đủ tiện nghi tiêu chuẩn 5 sao.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Tiện ích & Dịch vụ đi kèm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val defaultAmenities = listOf("Wifi tốc độ cao", "Điều hòa 2 chiều", "Minibar", "Bồn tắm cao cấp", "Smart TV 55 inch", "Bữa sáng miễn phí")
                                val amenitiesList = if (room.amenities.isNotEmpty()) room.amenities else defaultAmenities

                                amenitiesList.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        pair.forEach { amenity ->
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = amenity,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
