package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomTypeFilterCriteria;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeDetailResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomTypeSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface RoomTypeService {

    PageResponse<RoomTypeSummaryResponse> getRoomTypeList(RoomTypeFilterCriteria criteria, Pageable pageable);

    RoomTypeDetailResponse getRoomTypeDetail(Integer id);
}
