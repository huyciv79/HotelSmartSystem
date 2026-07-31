package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateServiceRequest;
import com.example.hotelsmartbookingbackend.dto.response.ServiceResponse;
import com.example.hotelsmartbookingbackend.entity.Service;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private Service sampleService;

    @BeforeEach
    void setUp() {
        sampleService = new Service();
        sampleService.setId(1);
        sampleService.setName("Laundry Service");
        sampleService.setDescription("Washing and ironing");
        sampleService.setPrice(new BigDecimal("50000.00"));
        sampleService.setUnit("Item");
        sampleService.setIsActive(true);
        sampleService.setIsDeleted(false);
        sampleService.setCreatedAt(Instant.now());
        sampleService.setUpdatedAt(Instant.now());
    }

    // ==========================================
    // 1. getAllServices Test Cases (UTCID01 - UTCID02)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful getAllServices returning list of non-deleted services")
    void should_getAllServicesSuccessfully_when_activeServicesExist() {
        when(serviceRepository.findByIsDeletedFalse()).thenReturn(List.of(sampleService));

        List<ServiceResponse> responses = serviceService.getAllServices();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Laundry Service", responses.get(0).getName());
        assertEquals(new BigDecimal("50000.00"), responses.get(0).getPrice());
        assertTrue(responses.get(0).getIsactive());
    }

    @Test
    @DisplayName("UTCID02 - Successful getAllServices returning empty list when no active services exist")
    void should_getEmptyServiceListSuccessfully_when_noActiveServicesExist() {
        when(serviceRepository.findByIsDeletedFalse()).thenReturn(List.of());

        List<ServiceResponse> responses = serviceService.getAllServices();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // ==========================================
    // 2. getServiceById Test Cases (UTCID03 - UTCID04)
    // ==========================================

    @Test
    @DisplayName("UTCID03 - Successful getServiceById when active service exists")
    void should_getServiceByIdSuccessfully_when_serviceIdExistsAndNotDeleted() {
        when(serviceRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleService));

        ServiceResponse response = serviceService.getServiceById(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Laundry Service", response.getName());
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when getServiceById with non-existing or deleted service ID")
    void should_throwException_when_getServiceByIdWithServiceNotFoundOrDeleted() {
        when(serviceRepository.findByIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceService.getServiceById(99));

        assertEquals("Không tìm thấy dịch vụ với mã: 99", ex.getMessage());
    }

    // ==========================================
    // 3. createService Test Cases (UTCID05, UTCID10)
    // ==========================================

    @Test
    @DisplayName("UTCID05 - Successful createService with valid request and isActive true")
    void should_createServiceSuccessfully_when_validRequestProvided() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Airport Shuttle");
        request.setDescription("Pickup and dropoff");
        request.setPrice(new BigDecimal("200000.00"));
        request.setUnit("Trip");
        request.setIsactive(true);

        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> {
            Service s = i.getArgument(0);
            s.setId(2);
            return s;
        });

        ServiceResponse response = serviceService.createService(request);

        assertNotNull(response);
        assertEquals(2, response.getId());
        assertEquals("Airport Shuttle", response.getName());
        assertEquals(new BigDecimal("200000.00"), response.getPrice());
        assertTrue(response.getIsactive());
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    @DisplayName("UTCID10 - Successful createService when isActive is set to false")
    void should_createServiceWithIsActiveFalse_when_isactiveIsFalseInRequest() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Spa Bath");
        request.setDescription("Temporarily disabled service");
        request.setPrice(new BigDecimal("300000.00"));
        request.setUnit("Session");
        request.setIsactive(false);

        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> {
            Service s = i.getArgument(0);
            s.setId(3);
            return s;
        });

        ServiceResponse response = serviceService.createService(request);

        assertNotNull(response);
        assertFalse(response.getIsactive());
    }

    // ==========================================
    // 4. updateService Test Cases (UTCID06 - UTCID07, UTCID11)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Successful updateService when service exists and is active")
    void should_updateServiceSuccessfully_when_serviceIdExistsAndNotDeleted() {
        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setName("Express Laundry");
        request.setDescription("Same day washing");
        request.setPrice(new BigDecimal("75000.00"));
        request.setUnit("Item");
        request.setIsactive(true);

        when(serviceRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleService));
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));

        ServiceResponse response = serviceService.updateService(1, request);

        assertNotNull(response);
        assertEquals("Express Laundry", response.getName());
        assertEquals(new BigDecimal("75000.00"), response.getPrice());
        verify(serviceRepository).save(sampleService);
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when updateService with non-existing or deleted service ID")
    void should_throwException_when_updateServiceWithServiceNotFoundOrDeleted() {
        UpdateServiceRequest request = new UpdateServiceRequest();
        when(serviceRepository.findByIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceService.updateService(99, request));

        assertEquals("Không tìm thấy dịch vụ với mã: 99", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Successful updateService when isActive is set to false")
    void should_updateServiceWithIsActiveFalse_when_isactiveIsFalseInRequest() {
        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setName("Laundry Service");
        request.setDescription("Disabled service");
        request.setPrice(new BigDecimal("50000.00"));
        request.setUnit("Item");
        request.setIsactive(false);

        when(serviceRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleService));
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));

        ServiceResponse response = serviceService.updateService(1, request);

        assertNotNull(response);
        assertFalse(response.getIsactive());
    }

    // ==========================================
    // 5. deleteService Test Cases (UTCID08 - UTCID09)
    // ==========================================

    @Test
    @DisplayName("UTCID08 - Successful deleteService performing soft-delete by setting isDeleted to true")
    void should_deleteServiceSuccessfully_when_serviceIdExistsAndNotDeleted() {
        when(serviceRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleService));

        serviceService.deleteService(1);

        assertTrue(sampleService.getIsDeleted());
        verify(serviceRepository).save(sampleService);
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when deleteService with non-existing or deleted service ID")
    void should_throwException_when_deleteServiceWithServiceNotFoundOrDeleted() {
        when(serviceRepository.findByIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceService.deleteService(99));

        assertEquals("Không tìm thấy dịch vụ với mã: 99", ex.getMessage());
    }
}
