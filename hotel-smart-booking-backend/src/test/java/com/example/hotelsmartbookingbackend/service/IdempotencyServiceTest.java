package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.IdempotentRecord;
import com.example.hotelsmartbookingbackend.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testStartOperation_NewKey() {
        when(valueOperations.setIfAbsent(eq("idempotency:key-1"), any(IdempotentRecord.class), any(Duration.class)))
                .thenReturn(true);

        boolean result = idempotencyService.startOperation("key-1");
        assertTrue(result);
    }

    @Test
    void testStartOperation_DuplicateKey() {
        when(valueOperations.setIfAbsent(eq("idempotency:key-1"), any(IdempotentRecord.class), any(Duration.class)))
                .thenReturn(false);

        boolean result = idempotencyService.startOperation("key-1");
        assertFalse(result);
    }

    @Test
    void testCompleteOperation() {
        idempotencyService.completeOperation("key-1", 200, "ResponseData");
        verify(valueOperations).set(eq("idempotency:key-1"), any(IdempotentRecord.class), any(Duration.class));
    }

    @Test
    void testGetRecord_TypedRecord() {
        IdempotentRecord record = new IdempotentRecord("SUCCESS", 200, "Data");
        when(valueOperations.get("idempotency:key-1")).thenReturn(record);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("key-1");
        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getStatus());
        assertEquals(200, result.get().getResponseStatus());
        assertEquals("Data", result.get().getResponseBody());
    }

    @Test
    void testGetRecord_LinkedHashMapFallback() {
        Map<String, Object> mockMap = new HashMap<>();
        mockMap.put("status", "SUCCESS");
        mockMap.put("responseStatus", 200);
        mockMap.put("responseBody", "Data");

        when(valueOperations.get("idempotency:key-1")).thenReturn(mockMap);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("key-1");
        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getStatus());
        assertEquals(200, result.get().getResponseStatus());
        assertEquals("Data", result.get().getResponseBody());
    }

    @Test
    void testRemoveKey() {
        idempotencyService.removeKey("key-1");
        verify(redisTemplate).delete("idempotency:key-1");
    }
}
