package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.config.PaypalConfig;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaypalServiceImplTest {

    @Mock
    private PaypalConfig paypalConfig;
    @Mock
    private RestClient paypalRestClient;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private PaypalServiceImpl paypalService;

    @BeforeEach
    void setUp() {
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        requestBodySpec = mock(RestClient.RequestBodySpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().when(paypalConfig.getClientId()).thenReturn("mock_client_id");
        lenient().when(paypalConfig.getClientSecret()).thenReturn("mock_client_secret");
        lenient().when(paypalConfig.getReturnUrl()).thenReturn("http://localhost/return");
        lenient().when(paypalConfig.getCancelUrl()).thenReturn("http://localhost/cancel");
    }

    private void mockRestClientFluentChain() {
        lenient().when(paypalRestClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    // ==========================================
    // 1. getAccessToken Test Cases (UTCID01 - UTCID05, UTCID19 - UTCID20)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Return cached token from Redis directly without making HTTP call")
    void should_returnCachedToken_when_tokenExistsInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("cached_token_123");

        String token = paypalService.getAccessToken();

        assertEquals("cached_token_123", token);
        verify(paypalRestClient, never()).post();
    }

    @Test
    @DisplayName("UTCID02 - Fetch new token from PayPal API when Redis cache is empty and cache it")
    void should_fetchNewTokenAndCacheInRedis_when_redisCacheIsEmpty() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn(null);

        Map<String, Object> oauthResponse = new HashMap<>();
        oauthResponse.put("access_token", "new_token_456");
        oauthResponse.put("expires_in", 3600);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(oauthResponse);

        String token = paypalService.getAccessToken();

        assertEquals("new_token_456", token);
        verify(valueOperations).set(eq("paypal:access_token"), eq("new_token_456"), eq(Duration.ofSeconds(3540)));
    }

    @Test
    @DisplayName("UTCID03 - Catch Redis exception silently and fetch new token from PayPal API")
    void should_catchRedisExceptionSilentlyAndFetchToken_when_redisThrowsError() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenThrow(new RuntimeException("Redis connection refused"));

        Map<String, Object> oauthResponse = new HashMap<>();
        oauthResponse.put("access_token", "new_token_456");
        oauthResponse.put("expires_in", 3600);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(oauthResponse);

        String token = paypalService.getAccessToken();

        assertEquals("new_token_456", token);
        verify(paypalRestClient).post();
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when PayPal OAuth response is empty or missing access_token")
    void should_throwException_when_paypalOauthReturnsEmptyResponseOrMissingToken() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn(null);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paypalService.getAccessToken());

        assertTrue(ex.getMessage().contains("Failed to retrieve access token from PayPal: Empty response"));
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when PayPal OAuth HTTP endpoint throws error")
    void should_throwException_when_paypalOauthHttpEndpointThrowsError() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn(null);

        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paypalService.getAccessToken());

        assertTrue(ex.getMessage().contains("PayPal Authentication failed: Connection refused"));
    }

    @Test
    @DisplayName("UTCID19 - Default cache expiry to 3600s when expires_in field is null in OAuth response")
    void should_defaultCacheExpiry_when_expiresInFieldIsNull() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn(null);

        Map<String, Object> oauthResponse = new HashMap<>();
        oauthResponse.put("access_token", "token_no_expiry");
        oauthResponse.put("expires_in", null);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(oauthResponse);

        String token = paypalService.getAccessToken();

        assertEquals("token_no_expiry", token);
        verify(valueOperations).set(eq("paypal:access_token"), eq("token_no_expiry"), eq(Duration.ofSeconds(3540)));
    }

    @Test
    @DisplayName("UTCID20 - Catch Redis write exception silently during getAccessToken")
    void should_catchRedisWriteExceptionSilently_when_cachingTokenToRedis() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn(null);
        doThrow(new RuntimeException("Redis write failure"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        Map<String, Object> oauthResponse = new HashMap<>();
        oauthResponse.put("access_token", "valid_token");
        oauthResponse.put("expires_in", 3600);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(oauthResponse);

        String token = assertDoesNotThrow(() -> paypalService.getAccessToken());

        assertEquals("valid_token", token);
    }

    // ==========================================
    // 2. createPaypalOrder Test Cases (UTCID06 - UTCID10, UTCID21)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Successful createPaypalOrder with approval link having rel=approve")
    void should_createPaypalOrderSuccessfully_when_approvalLinkWithRelApprovePresent() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("id", "ORD-100");
        orderResponse.put("status", "CREATED");
        orderResponse.put("links", List.of(
                Map.of("rel", "self", "href", "http://paypal/self"),
                Map.of("rel", "approve", "href", "https://www.sandbox.paypal.com/checkoutnow?token=ORD-100")
        ));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(orderResponse);

        PaypalOrderResponse response = paypalService.createPaypalOrder(1, new BigDecimal("100.00"), "ik-001");

        assertNotNull(response);
        assertEquals("ORD-100", response.getPaypalOrderId());
        assertEquals("https://www.sandbox.paypal.com/checkoutnow?token=ORD-100", response.getApproveUrl());
        assertEquals("CREATED", response.getStatus());
    }

    @Test
    @DisplayName("UTCID07 - Successful createPaypalOrder with approval link having rel=payer-action")
    void should_createPaypalOrderSuccessfully_when_linkWithRelPayerActionPresent() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("id", "ORD-101");
        orderResponse.put("status", "CREATED");
        orderResponse.put("links", List.of(
                Map.of("rel", "payer-action", "href", "https://www.sandbox.paypal.com/payer-action?token=ORD-101")
        ));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(orderResponse);

        PaypalOrderResponse response = paypalService.createPaypalOrder(1, new BigDecimal("50.00"), "ik-002");

        assertNotNull(response);
        assertEquals("https://www.sandbox.paypal.com/payer-action?token=ORD-101", response.getApproveUrl());
    }

    @Test
    @DisplayName("UTCID08 - Successful createPaypalOrder when links list contains no approve rel")
    void should_createPaypalOrderSuccessfully_when_noApproveLinkPresent() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("id", "ORD-102");
        orderResponse.put("status", "CREATED");
        orderResponse.put("links", List.of(
                Map.of("rel", "self", "href", "http://paypal/self")
        ));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(orderResponse);

        PaypalOrderResponse response = paypalService.createPaypalOrder(1, new BigDecimal("50.00"), "ik-003");

        assertNotNull(response);
        assertEquals("", response.getApproveUrl());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when PayPal Order response is missing id")
    void should_throwException_when_createPaypalOrderReturnsEmptyResponseOrMissingId() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(new HashMap<>());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.createPaypalOrder(1, new BigDecimal("50.00"), "ik-004"));

        assertTrue(ex.getMessage().contains("Invalid response from PayPal Order Creation"));
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when PayPal Order HTTP endpoint throws error")
    void should_throwException_when_createPaypalOrderHttpCallFails() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("HTTP 500 Internal Server Error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.createPaypalOrder(1, new BigDecimal("50.00"), "ik-005"));

        assertTrue(ex.getMessage().contains("Failed to create PayPal order: HTTP 500 Internal Server Error"));
    }

    @Test
    @DisplayName("UTCID21 - Successful createPaypalOrder when links field is null in response")
    void should_createPaypalOrderSuccessfully_when_linksFieldIsNull() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("id", "ORD-201");
        orderResponse.put("status", "CREATED");
        orderResponse.put("links", null);

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(orderResponse);

        PaypalOrderResponse response = paypalService.createPaypalOrder(1, new BigDecimal("50.00"), "ik-021");

        assertNotNull(response);
        assertEquals("ORD-201", response.getPaypalOrderId());
        assertEquals("", response.getApproveUrl());
    }

    // ==========================================
    // 3. capturePaypalOrder Test Cases (UTCID11 - UTCID14, UTCID22 - UTCID24)
    // ==========================================

    @Test
    @DisplayName("UTCID11 - Successful capturePaypalOrder with purchase_units and capture details")
    void should_capturePaypalOrderSuccessfully_when_purchaseUnitsAndCapturesPresent() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> amountObj = Map.of("value", "100.00", "currency_code", "USD");
        Map<String, Object> captureObj = Map.of("id", "TXN-001", "status", "COMPLETED", "amount", amountObj);
        Map<String, Object> paymentsObj = Map.of("captures", List.of(captureObj));
        Map<String, Object> puObj = Map.of("payments", paymentsObj);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "COMPLETED");
        responseMap.put("purchase_units", List.of(puObj));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        PaypalCaptureResponse response = paypalService.capturePaypalOrder("ORD-100", "ik-011");

        assertNotNull(response);
        assertEquals("ORD-100", response.getPaypalOrderId());
        assertEquals("TXN-001", response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    @DisplayName("UTCID12 - Successful capturePaypalOrder when purchase_units is empty")
    void should_capturePaypalOrderSuccessfully_when_purchaseUnitsIsEmpty() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "COMPLETED");
        responseMap.put("purchase_units", List.of());

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        PaypalCaptureResponse response = paypalService.capturePaypalOrder("ORD-100", "ik-012");

        assertNotNull(response);
        assertNull(response.getTransactionId());
        assertEquals(BigDecimal.ZERO, response.getAmount());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when capture response is null")
    void should_throwException_when_capturePaypalOrderReturnsNullResponse() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.capturePaypalOrder("ORD-100", "ik-013"));

        assertTrue(ex.getMessage().contains("Empty response from PayPal Capture Order"));
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when capture HTTP call fails")
    void should_throwException_when_capturePaypalOrderHttpCallFails() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("HTTP 400 Bad Request"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.capturePaypalOrder("ORD-100", "ik-014"));

        assertTrue(ex.getMessage().contains("Failed to capture PayPal order: HTTP 400 Bad Request"));
    }

    @Test
    @DisplayName("UTCID22 - Successful capturePaypalOrder when payments map in purchase_units is null")
    void should_capturePaypalOrderSuccessfully_when_paymentsMapIsNull() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> puObj = new HashMap<>();
        puObj.put("payments", null);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "COMPLETED");
        responseMap.put("purchase_units", List.of(puObj));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        PaypalCaptureResponse response = paypalService.capturePaypalOrder("ORD-202", "ik-022");

        assertNotNull(response);
        assertNull(response.getTransactionId());
        assertEquals(BigDecimal.ZERO, response.getAmount());
    }

    @Test
    @DisplayName("UTCID23 - Successful capturePaypalOrder when capStatus and amountObj are null in captures")
    void should_capturePaypalOrderSuccessfully_when_capStatusAndAmountObjAreNull() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> captureObj = new HashMap<>();
        captureObj.put("id", "TXN-203");
        captureObj.put("status", null);
        captureObj.put("amount", null);

        Map<String, Object> paymentsObj = Map.of("captures", List.of(captureObj));
        Map<String, Object> puObj = Map.of("payments", paymentsObj);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "COMPLETED");
        responseMap.put("purchase_units", List.of(puObj));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        PaypalCaptureResponse response = paypalService.capturePaypalOrder("ORD-203", "ik-023");

        assertNotNull(response);
        assertEquals("TXN-203", response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus()); // Top-level status preserved
        assertEquals(BigDecimal.ZERO, response.getAmount());
    }

    @Test
    @DisplayName("UTCID24 - Successful capturePaypalOrder when value field in amountObj is null")
    void should_capturePaypalOrderSuccessfully_when_amountValueIsNull() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> amountObj = new HashMap<>();
        amountObj.put("value", null);
        amountObj.put("currency_code", "USD");

        Map<String, Object> captureObj = Map.of("id", "TXN-204", "status", "COMPLETED", "amount", amountObj);
        Map<String, Object> paymentsObj = Map.of("captures", List.of(captureObj));
        Map<String, Object> puObj = Map.of("payments", paymentsObj);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", "COMPLETED");
        responseMap.put("purchase_units", List.of(puObj));

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        PaypalCaptureResponse response = paypalService.capturePaypalOrder("ORD-204", "ik-024");

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getAmount());
        assertEquals("USD", response.getCurrency());
    }

    // ==========================================
    // 4. refundPaypalCapture Test Cases (UTCID15 - UTCID18)
    // ==========================================

    @Test
    @DisplayName("UTCID15 - Successful refundPaypalCapture when status is COMPLETED")
    void should_refundPaypalCaptureSuccessfully_when_statusIsCompleted() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> responseMap = Map.of("id", "REF-001", "status", "COMPLETED");
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        assertDoesNotThrow(() -> paypalService.refundPaypalCapture("CAP-100", new BigDecimal("50.00"), "ik-015"));
    }

    @Test
    @DisplayName("UTCID16 - Successful refundPaypalCapture when status is PENDING")
    void should_refundPaypalCaptureSuccessfully_when_statusIsPending() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> responseMap = Map.of("id", "REF-002", "status", "PENDING");
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        assertDoesNotThrow(() -> paypalService.refundPaypalCapture("CAP-100", new BigDecimal("50.00"), "ik-016"));
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when refund API returns null response")
    void should_throwException_when_refundPaypalCaptureReturnsNullResponse() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.refundPaypalCapture("CAP-100", new BigDecimal("50.00"), "ik-017"));

        assertTrue(ex.getMessage().contains("Empty response from PayPal Refund API"));
    }

    @Test
    @DisplayName("UTCID18 - Throw exception when refund API returns status FAILED")
    void should_throwException_when_refundPaypalCaptureReturnsFailedStatus() {
        mockRestClientFluentChain();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("paypal:access_token")).thenReturn("token_123");

        Map<String, Object> responseMap = Map.of("id", "REF-003", "status", "FAILED");
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseMap);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paypalService.refundPaypalCapture("CAP-100", new BigDecimal("50.00"), "ik-018"));

        assertTrue(ex.getMessage().contains("PayPal refund status is not COMPLETED/PENDING: FAILED"));
    }
}
