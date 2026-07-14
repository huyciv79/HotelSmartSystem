package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;

import java.util.List;

public interface RoomChangeService {


    RoomChangeResponse changeRoom(RoomChangeRequest request, String staffEmail);


    CustomerRequestResponse submitRoomChangeRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest request, String customerEmail);


    CustomerRequestResponse approveRoomChangeRequest(
            Integer requestId, Integer newRoomId, String staffEmail);

    CustomerRequestResponse rejectRoomChangeRequest(
            Integer requestId, String rejectionReason, String staffEmail);

    List<CustomerRequestResponse> getPendingRoomChangeRequests(
            String staffEmail);

    CustomerRequestResponse submitStayExtensionRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest request, String customerEmail);

    CustomerRequestResponse approveStayExtensionRequest(
            Integer requestId, String staffEmail);

    CustomerRequestResponse rejectStayExtensionRequest(
            Integer requestId, String rejectionReason, String staffEmail);


    List<CustomerRequestResponse> getPendingStayExtensionRequests(
            String staffEmail);

    CustomerRequestResponse submitEarlyCheckOutRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest request, String customerEmail);


    CustomerRequestResponse approveEarlyCheckOutRequest(
            Integer requestId, String staffEmail);


    CustomerRequestResponse rejectEarlyCheckOutRequest(
            Integer requestId, String rejectionReason, String staffEmail);


    List<CustomerRequestResponse> getPendingEarlyCheckOutRequests(
            String staffEmail);
}
