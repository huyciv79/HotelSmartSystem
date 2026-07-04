package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.ServiceResponse;
import com.example.hotelsmartbookingbackend.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices() {

        List<ServiceResponse> services = serviceService.getAllServices();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy danh sách dịch vụ thành công",
                        services
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(
            @PathVariable Integer id) {

        ServiceResponse service = serviceService.getServiceById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lấy chi tiết dịch vụ thành công",
                        service
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request) {

        ServiceResponse created = serviceService.createService(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Tạo dịch vụ thành công",
                        created
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateServiceRequest request) {

        ServiceResponse updated = serviceService.updateService(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật dịch vụ thành công",
                        updated
                )
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteService(
            @PathVariable Integer id) {

        serviceService.deleteService(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Xóa dịch vụ thành công",
                        null
                )
        );
    }
}
