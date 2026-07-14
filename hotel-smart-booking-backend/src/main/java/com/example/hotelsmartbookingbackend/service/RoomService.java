package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import org.springframework.data.domain.Pageable;
import com.example.hotelsmartbookingbackend.dto.response.RoomStatusResponse;
import java.util.List;

public interface RoomService {

    PageResponse<RoomSummaryDTO> getRoomList(RoomFilterCriteria criteria, Pageable pageable);

    RoomDetailDTO getRoomDetail(Integer id);

    RoomDetailDTO updateRoom(Integer id, UpdateRoomRequest request);

    RoomStatusResponse getRoomStatus(Integer roomId);

    List<RoomStatusResponse> getAllRoomStatuses();

    RoomStatusResponse updateRoomStatus(Integer roomId, UpdateRoomRequest request);
}

