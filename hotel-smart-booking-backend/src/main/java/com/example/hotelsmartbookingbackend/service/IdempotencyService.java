package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.IdempotentRecord;
import java.util.Optional;

public interface IdempotencyService {
    boolean startOperation(String key);
    void completeOperation(String key, int responseStatus, Object responseBody);
    Optional<IdempotentRecord> getRecord(String key);
    void removeKey(String key);
}
