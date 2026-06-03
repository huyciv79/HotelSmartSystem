package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailDTO;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryDTO;
import org.springframework.data.domain.Pageable;

public interface RoomTypeService {

    PageResponse<RoomTypeSummaryDTO> getRoomTypeList(RoomTypeFilterCriteria criteria, Pageable pageable);

    RoomTypeDetailDTO getRoomTypeDetail(Integer id);
}
