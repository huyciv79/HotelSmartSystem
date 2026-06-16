package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CreateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.request.UpdateRoomTypeRequest;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryDTO;
import org.springframework.data.domain.Pageable;

public interface RoomTypeService {

    PageResponse<RoomTypeSummaryDTO> getRoomTypeList(RoomTypeFilterCriteria criteria, Pageable pageable);

    RoomTypeDetailDTO getRoomTypeDetail(Integer id);

    RoomTypeDetailDTO createRoomType(CreateRoomTypeRequest request);

    RoomTypeDetailDTO updateRoomType(Integer id, UpdateRoomTypeRequest request);

    void deleteRoomType(Integer id);
}
