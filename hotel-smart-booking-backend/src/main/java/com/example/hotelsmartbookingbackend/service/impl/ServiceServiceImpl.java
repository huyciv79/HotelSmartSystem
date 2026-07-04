package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.response.ServiceResponse;
import com.example.hotelsmartbookingbackend.entity.Service;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import com.example.hotelsmartbookingbackend.service.ServiceService;
import lombok.RequiredArgsConstructor;


import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {

        return serviceRepository.findByIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Integer id) {

        Service service = serviceRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy dịch vụ với mã: " + id));

        return mapToResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {

        Service service = new Service();

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setUnit(request.getUnit());
        service.setIsactive(request.getIsactive());

        service.setCreatedat(Instant.now());
        service.setUpdatedat(Instant.now());

        Service saved = serviceRepository.save(service);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteService(Integer id) {

        Service service = serviceRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy dịch vụ với mã: " + id));

        service.setIsDeleted(true);
        service.setUpdatedat(Instant.now());

        serviceRepository.save(service);
    }
    @Override
    @Transactional
    public ServiceResponse updateService(Integer id,
                                         UpdateServiceRequest request) {

        Service service = serviceRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy dịch vụ với mã: " + id));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setUnit(request.getUnit());
        service.setIsactive(request.getIsactive());

        service.setUpdatedat(Instant.now());

        Service updated = serviceRepository.save(service);

        return mapToResponse(updated);
    }

    private ServiceResponse mapToResponse(Service service) {

        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .unit(service.getUnit())
                .isactive(service.getIsactive())
                .createdAt(service.getCreatedat())
                .updatedAt(service.getUpdatedat())
                .build();
    }

}