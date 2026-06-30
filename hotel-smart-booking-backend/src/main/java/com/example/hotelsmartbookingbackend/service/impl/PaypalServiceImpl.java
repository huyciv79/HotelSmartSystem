package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.config.PaypalConfig;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import com.example.hotelsmartbookingbackend.service.PaypalService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaypalServiceImpl implements PaypalService {

    private final PaypalConfig paypalConfig;
    private final RestClient paypalRestClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaypalServiceImpl.class);
    private static final String ACCESS_TOKEN_CACHE_KEY = "paypal:access_token";

    @Override
    public String getAccessToken() {
        log.info("getAccessToken() - Reading from Redis cache...");
        String cachedToken = null;
        try {
            cachedToken = (String) redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        } catch (Exception e) {
            log.error("getAccessToken() - Error querying Redis, proceeding without cache: ", e);
        }
        
        if (cachedToken != null) {
            log.info("getAccessToken() - Found cached token in Redis (length: {})", cachedToken.length());
            return cachedToken;
        }

        log.info("getAccessToken() - Fetching new PayPal access token from API...");
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (paypalConfig.getClientId() + ":" + paypalConfig.getClientSecret()).getBytes(StandardCharsets.UTF_8));

        try {
            log.info("getAccessToken() - Executing HTTP POST /v1/oauth2/token to PayPal...");
            Map<String, Object> response = paypalRestClient.post()
                    .uri("/v1/oauth2/token")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("getAccessToken() - Response received from PayPal: {}", response != null ? "Not Null" : "Null");
            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException("Failed to retrieve access token from PayPal: Empty response");
            }

            String token = (String) response.get("access_token");
            Number expiresIn = (Number) response.get("expires_in");
            long cacheExpiry = (expiresIn != null) ? expiresIn.longValue() : 3600L;

            log.info("getAccessToken() - Token fetched successfully (length: {}). Caching to Redis...", token.length());
            try {
                redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, Duration.ofSeconds(cacheExpiry - 60));
            } catch (Exception e) {
                log.error("getAccessToken() - Error writing token to Redis: ", e);
            }
            return token;
        } catch (Exception e) {
            log.error("Error fetching access token from PayPal: ", e);
            throw new RuntimeException("PayPal Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public PaypalOrderResponse createPaypalOrder(Integer bookingId, BigDecimal amount, String idempotencyKey) {
        log.info("createPaypalOrder() - Booking ID: {}, Amount: {}, Idempotency Key: {}", bookingId, amount, idempotencyKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("intent", "CAPTURE");

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", "USD");
        amountMap.put("value", amount.setScale(2, RoundingMode.HALF_UP).toString());

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("reference_id", "booking_" + bookingId);
        purchaseUnit.put("amount", amountMap);

        requestBody.put("purchase_units", Collections.singletonList(purchaseUnit));

        Map<String, Object> experienceContext = new HashMap<>();
        experienceContext.put("return_url", paypalConfig.getReturnUrl());
        experienceContext.put("cancel_url", paypalConfig.getCancelUrl());
        experienceContext.put("shipping_preference", "NO_SHIPPING");
        experienceContext.put("user_action", "PAY_NOW");
        experienceContext.put("locale", "vi-VN");

        Map<String, Object> paypalSource = new HashMap<>();
        paypalSource.put("experience_context", experienceContext);

        Map<String, Object> paymentSource = new HashMap<>();
        paymentSource.put("paypal", paypalSource);

        requestBody.put("payment_source", paymentSource);

        try {
            log.info("createPaypalOrder() - Retrieving access token...");
            String accessToken = getAccessToken();
            log.info("createPaypalOrder() - Access token obtained (length: {})", accessToken.length());
            
            log.info("createPaypalOrder() - Executing HTTP POST /v2/checkout/orders to PayPal...");
            Map<String, Object> response = paypalRestClient.post()
                    .uri("/v2/checkout/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("PayPal-Request-Id", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("createPaypalOrder() - Response received from PayPal order API: {}", response != null ? "Not Null" : "Null");
            if (response == null || !response.containsKey("id")) {
                throw new RuntimeException("Invalid response from PayPal Order Creation");
            }

            String orderId = (String) response.get("id");
            String status = (String) response.get("status");
            String approveUrl = "";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) response.get("links");
            if (links != null) {
                for (Map<String, Object> link : links) {
                    String rel = (String) link.get("rel");
                    if ("approve".equals(rel) || "payer-action".equals(rel)) {
                        approveUrl = (String) link.get("href");
                        break;
                    }
                }
            }

            log.info("createPaypalOrder() - Created order successfully. ID: {}, Approve URL: {}, Status: {}", orderId, approveUrl, status);
            return PaypalOrderResponse.builder()
                    .paypalOrderId(orderId)
                    .approveUrl(approveUrl)
                    .status(status)
                    .build();

        } catch (Exception e) {
            log.error("Error creating PayPal order: ", e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage());
        }
    }

    @Override
    public PaypalCaptureResponse capturePaypalOrder(String paypalOrderId, String idempotencyKey) {
        log.info("Capturing PayPal order: {}, Idempotency Key: {}", paypalOrderId, idempotencyKey);

        try {
            Map<String, Object> response = paypalRestClient.post()
                    .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .header("PayPal-Request-Id", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new HashMap<>())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                throw new RuntimeException("Empty response from PayPal Capture Order");
            }

            String status = (String) response.get("status");
            String transactionId = null;
            BigDecimal capturedAmount = BigDecimal.ZERO;
            String currency = "USD";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) response.get("purchase_units");
            if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                Map<String, Object> pu = purchaseUnits.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> payments = (Map<String, Object>) pu.get("payments");
                if (payments != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
                    if (captures != null && !captures.isEmpty()) {
                        Map<String, Object> capture = captures.get(0);
                        transactionId = (String) capture.get("id");
                        String capStatus = (String) capture.get("status");
                        if (capStatus != null) {
                            status = capStatus;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> amountObj = (Map<String, Object>) capture.get("amount");
                        if (amountObj != null) {
                            String val = (String) amountObj.get("value");
                            if (val != null) {
                                capturedAmount = new BigDecimal(val);
                            }
                            currency = (String) amountObj.get("currency_code");
                        }
                    }
                }
            }

            return PaypalCaptureResponse.builder()
                    .paypalOrderId(paypalOrderId)
                    .transactionId(transactionId)
                    .status(status)
                    .amount(capturedAmount)
                    .currency(currency)
                    .build();

        } catch (Exception e) {
            log.error("Error capturing PayPal order: ", e);
            throw new RuntimeException("Failed to capture PayPal order: " + e.getMessage());
        }
    }

    @Override
    public void refundPaypalCapture(String captureId, BigDecimal usdAmount, String idempotencyKey) {
        log.info("refundPaypalCapture() - Capture ID: {}, Amount: {} USD, Idempotency Key: {}", captureId, usdAmount, idempotencyKey);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", "USD");
        amountMap.put("value", usdAmount.setScale(2, RoundingMode.HALF_UP).toString());
        requestBody.put("amount", amountMap);

        try {
            log.info("refundPaypalCapture() - Executing HTTP POST /v2/payments/captures/{id}/refund to PayPal...", captureId);
            Map<String, Object> response = paypalRestClient.post()
                    .uri("/v2/payments/captures/{id}/refund", captureId)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .header("PayPal-Request-Id", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("refundPaypalCapture() - Response received from PayPal refund API: {}", response != null ? "Not Null" : "Null");
            if (response == null) {
                throw new RuntimeException("Empty response from PayPal Refund API");
            }

            String status = (String) response.get("status");
            String refundId = (String) response.get("id");
            log.info("refundPaypalCapture() - Refund successful. Status: {}, Refund ID: {}", status, refundId);

            if (!"COMPLETED".equalsIgnoreCase(status) && !"PENDING".equalsIgnoreCase(status)) {
                throw new RuntimeException("PayPal refund status is not COMPLETED/PENDING: " + status);
            }
        } catch (Exception e) {
            log.error("Error executing PayPal refund for capture ID {}: ", captureId, e);
            throw new RuntimeException("PayPal refund failed: " + e.getMessage());
        }
    }
}
