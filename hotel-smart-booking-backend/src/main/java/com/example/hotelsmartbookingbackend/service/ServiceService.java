package com.example.hotelsmartbookingbackend.service;
import com.example.hotelsmartbookingbackend.dto.request.CreateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.response.ServiceResponse;
import java.util.List;

public interface ServiceService {List<ServiceResponse> getAllServices();

    ServiceResponse getServiceById(Integer id);

    ServiceResponse createService(CreateServiceRequest request);

    ServiceResponse updateService(Integer id, UpdateServiceRequest request);
    void deleteService(Integer id);
}
