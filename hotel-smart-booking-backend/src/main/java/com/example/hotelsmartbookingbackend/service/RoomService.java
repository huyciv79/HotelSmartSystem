package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomSummaryDTO;
import org.springframework.data.domain.Pageable;

public interface RoomService {

    PageResponse<RoomSummaryDTO> getRoomList(RoomFilterCriteria criteria, Pageable pageable);

    RoomDetailDTO getRoomDetail(Integer id);

    RoomDetailDTO updateRoom(Integer id, UpdateRoomRequest request);
}

