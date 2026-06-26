package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.ApproveRefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RejectRefundRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;

import java.util.List;

public interface RefundService {
    CustomerRequestResponse submitRefundRequest(Integer bookingId, RefundRequest request, String customerEmail);
    CustomerRequestResponse approveRefundRequest(Integer requestId, ApproveRefundRequest request, String staffEmail);
    CustomerRequestResponse rejectRefundRequest(Integer requestId, RejectRefundRequest request, String staffEmail);
    List<CustomerRequestResponse> getPendingRefundRequests(String staffEmail);
}
