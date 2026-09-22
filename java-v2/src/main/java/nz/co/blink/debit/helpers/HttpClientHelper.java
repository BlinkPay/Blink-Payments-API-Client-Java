package nz.co.blink.debit.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.service.AccessTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Helper class for HTTP operations with Blink Debit API.
 * Handles common concerns like authentication, serialization, error handling, and retries.
 */
public class HttpClientHelper {

    private static final Logger log = LoggerFactory.getLogger(HttpClientHelper.class);
    private static final int MAX_RETRIES = 2; // Retry up to 2 times (3 total attempts)
    private static final long FIRST_RETRY_DELAY_MS = 1000;  // 1 second
    private static final long SECOND_RETRY_DELAY_MS = 5000; // 5 seconds

    private final HttpClient httpClient;
    private final BlinkDebitConfig config;
    private final ObjectMapper objectMapper;
    private final AccessTokenManager tokenManager;
    private final String baseUrl;

    public HttpClientHelper(HttpClient httpClient, BlinkDebitConfig config,
                          ObjectMapper objectMapper, AccessTokenManager tokenManager) {
        this.httpClient = httpClient;
        this.config = config;
        this.objectMapper = objectMapper;
        this.tokenManager = tokenManager;
        this.baseUrl = config.getDebitUrl();
    }

    /**
     * Execute a POST request with JSON body.
     */
    public <T, R> R post(String path, T requestBody, Class<R> responseType) throws BlinkServiceException {
        return post(path, requestBody, responseType, UUID.randomUUID().toString());
    }

    /**
     * Execute a POST request with JSON body and custom request ID.
     */
    public <T, R> R post(String path, T requestBody, Class<R> responseType, String requestId)
            throws BlinkServiceException {
        return post(path, requestBody, responseType, requestId, null);
    }

    /**
     * Execute a POST request with JSON body, custom request ID and caller-supplied idempotency key.
     *
     * <p>The key is what the API de-duplicates on, so a caller that retries at its own level should
     * pass the same key it used on the original attempt. A null or blank key is generated instead.</p>
     */
    public <T, R> R post(String path, T requestBody, Class<R> responseType, String requestId,
                         String idempotencyKey) throws BlinkServiceException {
        return postWithRetry(path, requestBody, responseType, defaultIfBlank(requestId),
                defaultIfBlank(idempotencyKey), 0);
    }

    /**
     * Internal POST method with retry logic.
     * Reuses the same idempotency-key across retries to prevent duplicate requests.
     */
    private <T, R> R postWithRetry(String path, T requestBody, Class<R> responseType,
                                    String requestId, String idempotencyKey, int attemptNumber)
            throws BlinkServiceException {
        try {
            String accessToken = tokenManager.getAccessToken();
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(config.getTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("request-id", requestId)
                    .header("idempotency-key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            log.debug("POST {} with request-id: {}, idempotency-key: {}, attempt: {}",
                    path, requestId, idempotencyKey, attemptNumber + 1);
            log.debug("Request body: {}", requestBodyJson);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Response status: {}", response.statusCode());
            log.debug("Response body: {}", response.body());

            return handleResponseWithRetry(response, responseType, path, requestBody,
                                          requestId, idempotencyKey, attemptNumber, true);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request body", e);
            throw new BlinkServiceException("Failed to serialize request body", e);
        } catch (IOException e) {
            // Network error - retryable
            if (attemptNumber < MAX_RETRIES) {
                log.warn("Network error on attempt {} for POST {}: {}. Retrying with same idempotency-key...",
                        attemptNumber + 1, path, e.getMessage());
                sleepForRetry(attemptNumber);
                return postWithRetry(path, requestBody, responseType, requestId, idempotencyKey, attemptNumber + 1);
            }
            log.error("Network error after {} attempts for POST {}", attemptNumber + 1, path, e);
            throw new BlinkServiceException("Network error after " + (attemptNumber + 1) + " attempts: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during POST request to {}", path, e);
            throw new BlinkServiceException("Interrupted during POST request to " + path, e);
        }
    }

    /**
     * Execute a GET request.
     */
    public <R> R get(String path, Class<R> responseType) throws BlinkServiceException {
        return get(path, responseType, UUID.randomUUID().toString());
    }

    /**
     * Execute a GET request with custom request ID.
     */
    public <R> R get(String path, Class<R> responseType, String requestId) throws BlinkServiceException {
        return getWithRetry(path, responseType, requestId, 0);
    }

    /**
     * Internal GET method with retry logic.
     */
    private <R> R getWithRetry(String path, Class<R> responseType, String requestId, int attemptNumber)
            throws BlinkServiceException {
        try {
            String accessToken = tokenManager.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(config.getTimeout())
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("request-id", requestId)
                    .GET()
                    .build();

            log.debug("GET {} with request-id: {}, attempt: {}", path, requestId, attemptNumber + 1);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponseWithRetry(response, responseType, path, null, requestId, null, attemptNumber, false);

        } catch (IOException e) {
            // Network error - retryable
            if (attemptNumber < MAX_RETRIES) {
                log.warn("Network error on attempt {} for GET {}: {}. Retrying...",
                        attemptNumber + 1, path, e.getMessage());
                sleepForRetry(attemptNumber);
                return getWithRetry(path, responseType, requestId, attemptNumber + 1);
            }
            log.error("Network error after {} attempts for GET {}", attemptNumber + 1, path, e);
            throw new BlinkServiceException("Network error after " + (attemptNumber + 1) + " attempts: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during GET request to {}", path, e);
            throw new BlinkServiceException("Interrupted during GET request to " + path, e);
        }
    }

    /**
     * Execute a DELETE request.
     */
    public void delete(String path) throws BlinkServiceException {
        delete(path, UUID.randomUUID().toString());
    }

    /**
     * Execute a DELETE request with custom request ID.
     */
    public void delete(String path, String requestId) throws BlinkServiceException {
        deleteWithRetry(path, requestId, 0);
    }

    /**
     * Internal DELETE method with retry logic.
     */
    private void deleteWithRetry(String path, String requestId, int attemptNumber) throws BlinkServiceException {
        try {
            String accessToken = tokenManager.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(config.getTimeout())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("request-id", requestId)
                    .DELETE()
                    .build();

            log.debug("DELETE {} with request-id: {}, attempt: {}", path, requestId, attemptNumber + 1);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return; // Success
            }

            // Handle error response with retry logic
            if (isRetryableStatus(statusCode) && attemptNumber < MAX_RETRIES) {
                log.warn("Retryable error {} on attempt {} for DELETE {}. Retrying...",
                        statusCode, attemptNumber + 1, path);
                sleepForRetry(attemptNumber);
                deleteWithRetry(path, requestId, attemptNumber + 1);
                return;
            }

            // Non-retryable error or max retries exceeded
            String errorMessage = String.format("DELETE request failed: HTTP %d - %s",
                    statusCode, response.body());
            log.error(errorMessage);
            throw new BlinkServiceException(errorMessage);

        } catch (IOException e) {
            // Network error - retryable
            if (attemptNumber < MAX_RETRIES) {
                log.warn("Network error on attempt {} for DELETE {}: {}. Retrying...",
                        attemptNumber + 1, path, e.getMessage());
                sleepForRetry(attemptNumber);
                deleteWithRetry(path, requestId, attemptNumber + 1);
                return;
            }
            log.error("Network error after {} attempts for DELETE {}", attemptNumber + 1, path, e);
            throw new BlinkServiceException("Network error after " + (attemptNumber + 1) + " attempts: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during DELETE request to {}", path, e);
            throw new BlinkServiceException("Interrupted during DELETE request to " + path, e);
        }
    }

    /**
     * Execute a GET request that returns a List.
     */
    public <R> java.util.List<R> getList(String path, Class<R> elementType) throws BlinkServiceException {
        return getList(path, elementType, UUID.randomUUID().toString());
    }

    /**
     * Execute a GET request that returns a List with custom request ID.
     */
    public <R> java.util.List<R> getList(String path, Class<R> elementType, String requestId)
            throws BlinkServiceException {
        return getListWithRetry(path, elementType, requestId, 0);
    }

    /**
     * Internal GET List method with retry logic.
     */
    private <R> java.util.List<R> getListWithRetry(String path, Class<R> elementType, String requestId, int attemptNumber)
            throws BlinkServiceException {
        try {
            String accessToken = tokenManager.getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(config.getTimeout())
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("request-id", requestId)
                    .GET()
                    .build();

            log.debug("GET {} (list) with request-id: {}, attempt: {}", path, requestId, attemptNumber + 1);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleListResponseWithRetry(response, elementType, path, requestId, attemptNumber);

        } catch (IOException e) {
            // Network error - retryable
            if (attemptNumber < MAX_RETRIES) {
                log.warn("Network error on attempt {} for GET list {}: {}. Retrying...",
                        attemptNumber + 1, path, e.getMessage());
                sleepForRetry(attemptNumber);
                return getListWithRetry(path, elementType, requestId, attemptNumber + 1);
            }
            log.error("Network error after {} attempts for GET list {}", attemptNumber + 1, path, e);
            throw new BlinkServiceException("Network error after " + (attemptNumber + 1) + " attempts: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during GET request to {}", path, e);
            throw new BlinkServiceException("Interrupted during GET request to " + path, e);
        }
    }

    /**
     * Handle HTTP response with retry logic for POST/GET operations.
     */
    private <R, T> R handleResponseWithRetry(HttpResponse<String> response, Class<R> responseType,
                                             String path, T requestBody, String requestId,
                                             String idempotencyKey, int attemptNumber, boolean isPost)
            throws BlinkServiceException {
        int statusCode = response.statusCode();

        // Success
        if (statusCode >= 200 && statusCode < 300) {
            try {
                if (response.body() == null || response.body().isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(response.body(), responseType);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize response body", e);
                throw new BlinkServiceException("Failed to deserialize response body", e);
            }
        }

        // 401 Unauthorized - refresh token and retry ONCE (for token refresh only, not a general retry)
        if (statusCode == 401 && attemptNumber == 0) {
            log.warn("401 Unauthorized on attempt 1. Refreshing token and retrying...");
            tokenManager.refreshToken();
            if (isPost) {
                return postWithRetry(path, requestBody, responseType, requestId, idempotencyKey, attemptNumber + 1);
            } else {
                return getWithRetry(path, responseType, requestId, attemptNumber + 1);
            }
        }

        // Retryable errors (429, all 5xx)
        if (isRetryableStatus(statusCode) && attemptNumber < MAX_RETRIES) {
            log.warn("Retryable error {} on attempt {} for {} {}. Retrying...",
                    statusCode, attemptNumber + 1, isPost ? "POST" : "GET", path);
            sleepForRetry(attemptNumber);
            if (isPost) {
                return postWithRetry(path, requestBody, responseType, requestId, idempotencyKey, attemptNumber + 1);
            } else {
                return getWithRetry(path, responseType, requestId, attemptNumber + 1);
            }
        }

        // Non-retryable error or max retries exceeded
        String errorMessage = String.format("Request failed: HTTP %d - %s",
                statusCode, response.body());
        log.error(errorMessage);
        throw new BlinkServiceException(errorMessage);
    }

    /**
     * Handle HTTP response with retry logic for GET List operations.
     */
    private <R> java.util.List<R> handleListResponseWithRetry(HttpResponse<String> response, Class<R> elementType,
                                                               String path, String requestId, int attemptNumber)
            throws BlinkServiceException {
        int statusCode = response.statusCode();

        // Success
        if (statusCode >= 200 && statusCode < 300) {
            try {
                if (response.body() == null || response.body().isEmpty()) {
                    return java.util.Collections.emptyList();
                }
                return objectMapper.readValue(response.body(),
                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, elementType));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize response body", e);
                throw new BlinkServiceException("Failed to deserialize response body", e);
            }
        }

        // 401 Unauthorized - refresh token and retry ONCE
        if (statusCode == 401 && attemptNumber == 0) {
            log.warn("401 Unauthorized on attempt 1 for GET list. Refreshing token and retrying...");
            tokenManager.refreshToken();
            return getListWithRetry(path, elementType, requestId, attemptNumber + 1);
        }

        // Retryable errors (429, all 5xx)
        if (isRetryableStatus(statusCode) && attemptNumber < MAX_RETRIES) {
            log.warn("Retryable error {} on attempt {} for GET list {}. Retrying...",
                    statusCode, attemptNumber + 1, path);
            sleepForRetry(attemptNumber);
            return getListWithRetry(path, elementType, requestId, attemptNumber + 1);
        }

        // Non-retryable error or max retries exceeded
        String errorMessage = String.format("Request failed: HTTP %d - %s",
                statusCode, response.body());
        log.error(errorMessage);
        throw new BlinkServiceException(errorMessage);
    }

    /**
     * Check if HTTP status code is retryable.
     * Retries on 429 (Too Many Requests) and all 5xx (Server Errors).
     * Does NOT retry on 408 (Request Timeout) as it's a client-side timeout.
     * 401 is handled separately for token refresh only.
     */
    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 ||  // Too Many Requests
               (statusCode >= 500 && statusCode < 600);  // All 5xx Server Errors
    }

    /**
     * Returns the value, or a fresh UUID when it is null or blank. A blank header value would be
     * accepted by the builder and sent, which for an idempotency key means no de-duplication at all.
     */
    private static String defaultIfBlank(String value) {
        return (value == null || value.trim().isEmpty()) ? UUID.randomUUID().toString() : value;
    }

    /**
     * Sleep with appropriate delay for retry attempt.
     * First retry: 1 second
     * Second retry: 5 seconds
     */
    private void sleepForRetry(int attemptNumber) {
        try {
            long delay = (attemptNumber == 0) ? FIRST_RETRY_DELAY_MS : SECOND_RETRY_DELAY_MS;
            log.debug("Sleeping for {}ms before retry attempt {}", delay, attemptNumber + 2);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted during retry sleep", e);
        }
    }
}
