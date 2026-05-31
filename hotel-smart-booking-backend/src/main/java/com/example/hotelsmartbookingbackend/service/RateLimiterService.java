package com.example.hotelsmartbookingbackend.service;

public interface RateLimiterService {

    boolean isRateLimited(String key);

    void incrementAttempt(String key);

    void resetAttempt(String key);
}
