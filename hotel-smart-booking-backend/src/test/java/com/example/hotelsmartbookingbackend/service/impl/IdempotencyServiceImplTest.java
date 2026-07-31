package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.IdempotentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==========================================
    // 1. startOperation Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Return true when startOperation acquires lock successfully in Redis")
    void should_returnTrue_when_startOperationAcquiresLockSuccessfully() {
        when(valueOperations.setIfAbsent(eq("idempotency:req-123"), any(IdempotentRecord.class), eq(Duration.ofDays(1))))
                .thenReturn(true);

        boolean result = idempotencyService.startOperation("req-123");

        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq("idempotency:req-123"), any(IdempotentRecord.class), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("UTCID02 - Return false when startOperation fails to acquire lock because key exists")
    void should_returnFalse_when_startOperationFailsToAcquireLock() {
        when(valueOperations.setIfAbsent(eq("idempotency:req-123"), any(IdempotentRecord.class), eq(Duration.ofDays(1))))
                .thenReturn(false);

        boolean result = idempotencyService.startOperation("req-123");

        assertFalse(result);
    }

    @Test
    @DisplayName("UTCID03 - Return false when startOperation setIfAbsent returns null")
    void should_returnFalse_when_startOperationSetIfAbsentReturnsNull() {
        when(valueOperations.setIfAbsent(eq("idempotency:req-123"), any(IdempotentRecord.class), eq(Duration.ofDays(1))))
                .thenReturn(null);

        boolean result = idempotencyService.startOperation("req-123");

        assertFalse(result);
    }

    // ==========================================
    // 2. completeOperation Test Cases (UTCID04)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful completeOperation writing SUCCESS record with 24h TTL to Redis")
    void should_completeOperationSuccessfully_when_savingRecordToRedis() {
        idempotencyService.completeOperation("req-123", 200, "Response Payload");

        verify(valueOperations).set(eq("idempotency:req-123"), any(IdempotentRecord.class), eq(Duration.ofDays(1)));
    }

    // ==========================================
    // 3. getRecord Test Cases (UTCID05 - UTCID10)
    // ==========================================

    @Test
    @DisplayName("UTCID05 - Successful getRecord when Redis stores direct IdempotentRecord instance")
    void should_getRecordSuccessfully_when_redisContainsIdempotentRecordInstance() {
        IdempotentRecord record = new IdempotentRecord("SUCCESS", 200, "Cached Payload");
        when(valueOperations.get("idempotency:req-123")).thenReturn(record);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getStatus());
        assertEquals(200, result.get().getResponseStatus());
        assertEquals("Cached Payload", result.get().getResponseBody());
    }

    @Test
    @DisplayName("UTCID06 - Successful getRecord when Redis stores Map with non-null responseStatus")
    void should_getRecordSuccessfully_when_redisContainsMapWithNonNullResponseStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "SUCCESS");
        map.put("responseStatus", 201);
        map.put("responseBody", "Map Payload");
        when(valueOperations.get("idempotency:req-123")).thenReturn(map);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getStatus());
        assertEquals(201, result.get().getResponseStatus());
        assertEquals("Map Payload", result.get().getResponseBody());
    }

    @Test
    @DisplayName("UTCID07 - Successful getRecord when Redis stores Map with null responseStatus")
    void should_getRecordSuccessfully_when_redisContainsMapWithNullResponseStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "PROCESSING");
        map.put("responseStatus", null);
        map.put("responseBody", null);
        when(valueOperations.get("idempotency:req-123")).thenReturn(map);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertTrue(result.isPresent());
        assertEquals("PROCESSING", result.get().getStatus());
        assertEquals(0, result.get().getResponseStatus());
        assertNull(result.get().getResponseBody());
    }

    @Test
    @DisplayName("UTCID08 - Return empty when map fallback throws exception during cast")
    void should_returnEmpty_when_redisMapConversionThrowsException() {
        Map<Object, Object> invalidMap = mock(Map.class);
        when(invalidMap.get("status")).thenThrow(new ClassCastException("Cast failure"));
        when(valueOperations.get("idempotency:req-123")).thenReturn(invalidMap);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("UTCID09 - Return empty when Redis stores an unrecognized object type")
    void should_returnEmpty_when_redisContainsUnrecognizedObjectType() {
        when(valueOperations.get("idempotency:req-123")).thenReturn("Unrecognized String");

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("UTCID10 - Return empty when Redis key does not exist")
    void should_returnEmpty_when_redisKeyDoesNotExist() {
        when(valueOperations.get("idempotency:req-123")).thenReturn(null);

        Optional<IdempotentRecord> result = idempotencyService.getRecord("req-123");

        assertFalse(result.isPresent());
    }

    // ==========================================
    // 4. removeKey Test Cases (UTCID11)
    // ==========================================

    @Test
    @DisplayName("UTCID11 - Successful removeKey deleting Redis key")
    void should_removeKeySuccessfully_when_deletingRedisKey() {
        idempotencyService.removeKey("req-123");

        verify(redisTemplate).delete("idempotency:req-123");
    }
}
