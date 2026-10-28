/**
 * Copyright (c) 2022 BlinkPay
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nz.co.blink.debit.helpers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test cases for {@link AccessTokenHandler}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AccessTokenHandlerComprehensiveTest {

    @Mock
    private OAuthApiClient oAuthApiClient;

    private AccessTokenHandler handler;

    private static final String SECRET = "test-secret";

    @BeforeEach
    void setup() {
        handler = new AccessTokenHandler(oAuthApiClient);
    }

    @Test
    @DisplayName("Should reuse valid unexpired token for multiple requests")
    void shouldReuseValidToken() throws BlinkServiceException {
        // Create a token that expires in 1 hour
        String validToken = createToken(new Date(System.currentTimeMillis() + 3600000));

        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(validToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        // First request to generate token
        String requestId1 = UUID.randomUUID().toString();
        makeRequest(handler, requestId1);

        // Second request should reuse token
        String requestId2 = UUID.randomUUID().toString();
        makeRequest(handler, requestId2);

        // Third request should still reuse token
        String requestId3 = UUID.randomUUID().toString();
        makeRequest(handler, requestId3);

        // Verify token was only fetched once
        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should fetch new token when current token is expired")
    void shouldFetchNewTokenWhenExpired() throws BlinkServiceException {
        // Create an expired token
        String expiredToken = createToken(new Date(System.currentTimeMillis() - 1000));

        // Create a new valid token
        String newToken = createToken(new Date(System.currentTimeMillis() + 3600000));

        AccessTokenResponse newResponse = new AccessTokenResponse();
        newResponse.setAccessToken(newToken);

        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(newResponse));

        // Set expired token directly
        handler.setAccessTokenAtomicReference(expiredToken);

        // Request should detect expired token and fetch new one
        makeRequest(handler, UUID.randomUUID().toString());

        // Verify token was fetched once for the expired token
        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should handle concurrent requests properly")
    void shouldHandleConcurrentRequestsProperly() throws Exception {
        String validToken = createToken(new Date(System.currentTimeMillis() + 3600000));

        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(validToken);

        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        int concurrentRequests = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentRequests);

        // Launch multiple concurrent requests
        IntStream.range(0, concurrentRequests).forEach(i ->
            new Thread(() -> {
                try {
                    startLatch.await();
                    makeRequest(handler, UUID.randomUUID().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            }).start()
        );

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all requests to complete
        assertThat(completeLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Verify all requests completed successfully
        // The exact number of token fetches depends on timing and concurrency
        verify(oAuthApiClient, atLeastOnce()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should include bearer token in authorization header")
    void shouldIncludeBearerTokenInAuthorizationHeader() throws BlinkServiceException {
        String validToken = createToken(new Date(System.currentTimeMillis() + 3600000));

        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(validToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        ClientRequest[] capturedRequest = new ClientRequest[1];

        WebClient.builder()
                .filter(handler.setAccessToken(UUID.randomUUID().toString()))
                .exchangeFunction(clientRequest -> {
                    capturedRequest[0] = clientRequest;
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("{}")
                            .build());
                })
                .build()
                .get()
                .uri("http://localhost:8080/test")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(capturedRequest[0]).isNotNull();
        assertThat(capturedRequest[0].headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + validToken);
    }

    @Test
    @DisplayName("Should handle invalid JWT format gracefully")
    void shouldHandleInvalidJwtFormat() throws BlinkServiceException {
        // Set an invalid token initially
        handler.setAccessTokenAtomicReference("invalid.jwt.token");

        String validToken = createToken(new Date(System.currentTimeMillis() + 3600000));
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(validToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        // Should fetch new token when invalid JWT is detected
        makeRequest(handler, UUID.randomUUID().toString());

        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should propagate errors from OAuth client")
    void shouldPropagateOAuthClientErrors() throws BlinkServiceException {
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.error(new BlinkServiceException("OAuth service unavailable")));

        StepVerifier.create(
                WebClient.builder()
                        .filter(handler.setAccessToken(UUID.randomUUID().toString()))
                        .exchangeFunction(clientRequest ->
                                Mono.just(ClientResponse.create(HttpStatus.OK)
                                        .body("{}")
                                        .build()))
                        .build()
                        .get()
                        .uri("http://localhost:8080/test")
                        .retrieve()
                        .bodyToMono(String.class)
        ).expectError(BlinkServiceException.class)
         .verify();
    }

    @Test
    @DisplayName("Should detect expired token and fetch new one")
    void shouldDetectExpiredTokenAndFetchNew() throws BlinkServiceException {
        // First set an expired token directly
        String expiredToken = createToken(new Date(System.currentTimeMillis() - 1000)); // Already expired
        handler.setAccessTokenAtomicReference(expiredToken);

        // Setup mock to return a new valid token
        String newToken = createToken(new Date(System.currentTimeMillis() + 3600000));
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(newToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        // Make a request - should detect expired token and fetch new one
        makeRequest(handler, UUID.randomUUID().toString());

        // Verify new token was fetched because the old one was expired
        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should refresh a token that expires inside the refresh buffer")
    void shouldRefreshTokenInsideRefreshBuffer() throws BlinkServiceException {
        // 30 seconds of life left, which is inside the 60 second refresh buffer
        String nearlyExpiredToken = createToken(new Date(System.currentTimeMillis() + 30000));
        handler.setAccessTokenAtomicReference(nearlyExpiredToken);

        String newToken = createToken(new Date(System.currentTimeMillis() + 3600000));
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(newToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        makeRequest(handler, UUID.randomUUID().toString());

        // The token is still technically valid, but too close to expiry to send
        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should fetch a new token when the current one carries no expiry claim")
    void shouldFetchNewTokenWhenExpiryClaimIsMissing() throws BlinkServiceException {
        handler.setAccessTokenAtomicReference(createTokenWithoutExpiry());

        String newToken = createToken(new Date(System.currentTimeMillis() + 3600000));
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(newToken);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        makeRequest(handler, UUID.randomUUID().toString());

        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should fetch a second token rather than replaying the first fetch")
    void shouldNotReplayACompletedTokenFetch() throws BlinkServiceException {
        // The first fetch returns a token that is itself inside the refresh buffer, so the
        // second request has to go back to the server rather than replay the cached fetch.
        AccessTokenResponse firstResponse = new AccessTokenResponse();
        firstResponse.setAccessToken(createToken(new Date(System.currentTimeMillis() + 30000)));
        AccessTokenResponse secondResponse = new AccessTokenResponse();
        secondResponse.setAccessToken(createToken(new Date(System.currentTimeMillis() + 3600000)));

        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(firstResponse))
                .thenReturn(Mono.just(secondResponse));

        handler.setAccessTokenAtomicReference(createToken(new Date(System.currentTimeMillis() - 1000)));

        makeRequest(handler, UUID.randomUUID().toString());
        makeRequest(handler, UUID.randomUUID().toString());

        verify(oAuthApiClient, times(2)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should collapse callers arriving during a fetch onto that one token request")
    void shouldCollapseConcurrentTokenFetches() {
        // A sink lets the fetch stay in flight for as long as the test needs, so the collapse
        // is asserted deterministically rather than by racing threads against a sleep.
        Sinks.One<AccessTokenResponse> pending = Sinks.one();
        AtomicInteger subscriptions = new AtomicInteger();
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(pending.asMono().doOnSubscribe(subscription -> subscriptions.incrementAndGet()));

        // First caller starts the fetch and leaves it in flight
        Disposable first = request(handler, UUID.randomUUID().toString()).subscribe();
        // Second caller arrives before the first has completed
        Disposable second = request(handler, UUID.randomUUID().toString()).subscribe();

        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(createToken(new Date(System.currentTimeMillis() + 3600000)));
        pending.tryEmitValue(response);

        // Both callers were served, but only one request reached the server. Subscriptions are
        // what count here: a caller that loses the publish race still assembles a Mono, and
        // assembly sends nothing because generateAccessToken defers.
        assertThat(subscriptions.get()).isEqualTo(1);
        assertThat(first.isDisposed()).isTrue();
        assertThat(second.isDisposed()).isTrue();
    }

    @Test
    @DisplayName("Should reuse a token with no expiry claim for the lifetime the server reported")
    void shouldReuseTokenForReportedLifetime() throws BlinkServiceException {
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(createTokenWithoutExpiry());
        response.setExpiresIn(3600);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        makeRequest(handler, UUID.randomUUID().toString());
        makeRequest(handler, UUID.randomUUID().toString());

        // The token carries no expiry claim, so expires_in is what keeps it in use
        verify(oAuthApiClient, times(1)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should refresh a token whose reported lifetime falls inside the refresh buffer")
    void shouldRefreshTokenWhoseReportedLifetimeIsInsideRefreshBuffer() throws BlinkServiceException {
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(createTokenWithoutExpiry());
        // 30 seconds of life left, which is inside the 60 second refresh buffer
        response.setExpiresIn(30);
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        makeRequest(handler, UUID.randomUUID().toString());
        makeRequest(handler, UUID.randomUUID().toString());

        verify(oAuthApiClient, times(2)).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Should refresh a token that reports neither an expiry claim nor a lifetime")
    void shouldRefreshTokenWithNoExpiryClaimAndNoReportedLifetime() throws BlinkServiceException {
        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken(createTokenWithoutExpiry());
        when(oAuthApiClient.generateAccessToken(anyString()))
                .thenReturn(Mono.just(response));

        makeRequest(handler, UUID.randomUUID().toString());
        makeRequest(handler, UUID.randomUUID().toString());

        // Nothing says how long the token lives, so it is replaced rather than trusted
        verify(oAuthApiClient, times(2)).generateAccessToken(anyString());
    }

    private Mono<String> request(AccessTokenHandler handler, String requestId) {
        return WebClient.builder()
                .filter(handler.setAccessToken(requestId))
                .exchangeFunction(clientRequest ->
                        Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body("{}")
                                .build()))
                .build()
                .get()
                .uri("http://localhost:8080/test")
                .retrieve()
                .bodyToMono(String.class);
    }

    private void makeRequest(AccessTokenHandler handler, String requestId) {
        WebClient.builder()
                .filter(handler.setAccessToken(requestId))
                .exchangeFunction(clientRequest ->
                        Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body("{}")
                                .build()))
                .build()
                .get()
                .uri("http://localhost:8080/test")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String createToken(Date expiryDate) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer("test-issuer")
                .withSubject("test-subject")
                .withExpiresAt(expiryDate)
                .sign(algorithm);
    }

    private String createTokenWithoutExpiry() {
        return JWT.create()
                .withIssuer("test-issuer")
                .withSubject("test-subject")
                .sign(Algorithm.HMAC256(SECRET));
    }
}
