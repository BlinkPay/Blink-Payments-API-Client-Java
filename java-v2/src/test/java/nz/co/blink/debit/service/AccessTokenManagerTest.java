package nz.co.blink.debit.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenManagerTest {

    // ===== Token Lifecycle Tests =====

    @Test
    void testGetAccessTokenFirstTime() {
        StubOAuthApiClient stub = new StubOAuthApiClient(createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)));
        AccessTokenManager manager = new AccessTokenManager(stub);

        String token = manager.getAccessToken();

        assertThat(token).isNotNull();
        assertThat(stub.getCallCount()).isEqualTo(1);
    }

    @Test
    void testGetAccessTokenCached() {
        StubOAuthApiClient stub = new StubOAuthApiClient(createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)));
        AccessTokenManager manager = new AccessTokenManager(stub);

        String token1 = manager.getAccessToken();
        String token2 = manager.getAccessToken();
        String token3 = manager.getAccessToken();

        assertThat(token1).isEqualTo(token2).isEqualTo(token3);
        assertThat(stub.getCallCount()).isEqualTo(1); // Only called once
    }

    @Test
    void testGetAccessTokenExpired() {
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);

        StubOAuthApiClient stub = new StubOAuthApiClient(
                createJwtToken(pastTime),  // Expired token
                createJwtToken(futureTime) // Fresh token
        );
        AccessTokenManager manager = new AccessTokenManager(stub);

        String token1 = manager.getAccessToken();
        String token2 = manager.getAccessToken();

        assertThat(token1).isNotEqualTo(token2);
        assertThat(stub.getCallCount()).isEqualTo(2); // Called twice - first expired, second fresh
    }

    @Test
    void testGetAccessTokenNearExpiry() {
        // Token expires in 30 seconds (within 60 second buffer)
        Instant nearExpiry = Instant.now().plus(30, ChronoUnit.SECONDS);
        Instant farExpiry = Instant.now().plus(2, ChronoUnit.HOURS);

        // First token expires soon, second token is fresh
        StubOAuthApiClient stub = new StubOAuthApiClient(
                createJwtToken(nearExpiry),
                createJwtToken(farExpiry)
        );
        AccessTokenManager manager = new AccessTokenManager(stub);

        // First call fetches the near-expiry token (OAuth client returns it)
        String token1 = manager.getAccessToken();
        assertThat(stub.getCallCount()).isEqualTo(1);

        // Second call detects the token is near expiry and refreshes it
        String token2 = manager.getAccessToken();
        assertThat(token2).isNotEqualTo(token1); // Different token
        assertThat(stub.getCallCount()).isEqualTo(2); // Second fetch happened

        // Third call uses the cached fresh token
        String token3 = manager.getAccessToken();
        assertThat(token3).isEqualTo(token2); // Same as second
        assertThat(stub.getCallCount()).isEqualTo(2); // No additional fetch
    }

    // ===== Token Parsing Tests =====

    @Test
    void testTokenExpiryParsing() {
        Instant expectedExpiry = Instant.now().plus(2, ChronoUnit.HOURS);
        String token = createJwtToken(expectedExpiry);

        StubOAuthApiClient stub = new StubOAuthApiClient(token);
        AccessTokenManager manager = new AccessTokenManager(stub);

        manager.getAccessToken();

        // Token should be valid and cached
        assertThat(manager.getAccessToken()).isEqualTo(token);
        assertThat(stub.getCallCount()).isEqualTo(1);
    }

    @Test
    void testTokenWithoutExpiry() {
        String tokenWithoutExpiry = JWT.create()
                .withIssuer("test")
                .sign(Algorithm.none());

        StubOAuthApiClient stub = new StubOAuthApiClient(tokenWithoutExpiry);
        AccessTokenManager manager = new AccessTokenManager(stub);

        String token = manager.getAccessToken();

        assertThat(token).isNotNull();
        assertThat(stub.getCallCount()).isEqualTo(1);
    }

    @Test
    void testTokenWithoutExpiryTreatsNonPositiveExpiresInAsExpired() {
        String tokenWithoutExpiry = JWT.create()
                .withIssuer("test")
                .sign(Algorithm.none());

        // The server says the token is already dead, so it must not be cached for an hour
        StubOAuthApiClient stub = new StubOAuthApiClient(0, tokenWithoutExpiry);
        AccessTokenManager manager = new AccessTokenManager(stub);

        manager.getAccessToken();
        manager.getAccessToken();

        assertThat(stub.getCallCount()).isEqualTo(2);
    }

    @Test
    void testTokenWithoutExpiryRefreshesWhenExpiresInHasLapsed() {
        String tokenWithoutExpiry = JWT.create()
                .withIssuer("test")
                .sign(Algorithm.none());

        // expires_in is inside the refresh buffer, so every call fetches a new token
        StubOAuthApiClient stub = new StubOAuthApiClient(1, tokenWithoutExpiry);
        AccessTokenManager manager = new AccessTokenManager(stub);

        manager.getAccessToken();
        manager.getAccessToken();

        assertThat(stub.getCallCount()).isEqualTo(2);
    }

    @Test
    void testInvalidJwtFormat() {
        StubOAuthApiClient stub = new StubOAuthApiClient("not-a-jwt-token");
        AccessTokenManager manager = new AccessTokenManager(stub);

        assertThatThrownBy(manager::getAccessToken)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to refresh access token");
    }

    // ===== Thread Safety Tests =====

    @Test
    void testConcurrentGetAccessToken() throws InterruptedException {
        StubOAuthApiClient stub = new StubOAuthApiClient(createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)));
        AccessTokenManager manager = new AccessTokenManager(stub);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        String[] tokens = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    tokens[index] = manager.getAccessToken();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Start all threads
        endLatch.await(5, TimeUnit.SECONDS); // Wait for completion

        // All threads should get the same token
        assertThat(successCount.get()).isEqualTo(threadCount);
        for (String token : tokens) {
            assertThat(token).isEqualTo(tokens[0]);
        }

        // OAuth client should only be called once despite concurrent requests
        assertThat(stub.getCallCount()).isEqualTo(1);
    }

    @Test
    void testConcurrentRefresh() throws InterruptedException {
        StubOAuthApiClient stub = new StubOAuthApiClient(createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)));
        AccessTokenManager manager = new AccessTokenManager(stub);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    manager.refreshToken();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);

        // Even with concurrent refresh calls, token should be consistent
        String token = manager.getAccessToken();
        assertThat(token).isNotNull();
    }

    // ===== Error Handling Tests =====

    @Test
    void testRefreshTokenFailure() {
        OAuthApiClient failingClient = new OAuthApiClient(null, createMockConfig(), null) {
            @Override
            public AccessTokenResponse generateAccessToken() throws BlinkServiceException {
                throw new BlinkServiceException("OAuth server error");
            }
        };

        AccessTokenManager manager = new AccessTokenManager(failingClient);

        assertThatThrownBy(manager::getAccessToken)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to refresh access token")
                .hasCauseInstanceOf(BlinkServiceException.class);
    }

    @Test
    void testOAuthClientThrowsException() {
        OAuthApiClient throwingClient = new OAuthApiClient(null, createMockConfig(), null) {
            @Override
            public AccessTokenResponse generateAccessToken() {
                throw new RuntimeException("Network error");
            }
        };

        AccessTokenManager manager = new AccessTokenManager(throwingClient);

        assertThatThrownBy(manager::getAccessToken)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to refresh access token")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    // ===== Token Clearing Tests =====

    @Test
    void testClearToken() {
        StubOAuthApiClient stub = new StubOAuthApiClient(
                createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)),
                createJwtToken(Instant.now().plus(2, ChronoUnit.HOURS))
        );
        AccessTokenManager manager = new AccessTokenManager(stub);

        String token1 = manager.getAccessToken();
        assertThat(stub.getCallCount()).isEqualTo(1);

        manager.clearToken();

        String token2 = manager.getAccessToken();
        assertThat(stub.getCallCount()).isEqualTo(2); // Fetched again after clear
        assertThat(token2).isNotEqualTo(token1);
    }

    @Test
    void testGetTokenAfterClear() {
        StubOAuthApiClient stub = new StubOAuthApiClient(
                createJwtToken(Instant.now().plus(1, ChronoUnit.HOURS)),
                createJwtToken(Instant.now().plus(2, ChronoUnit.HOURS))
        );
        AccessTokenManager manager = new AccessTokenManager(stub);

        manager.getAccessToken();
        manager.clearToken();
        String newToken = manager.getAccessToken();

        assertThat(newToken).isNotNull();
        assertThat(stub.getCallCount()).isEqualTo(2);
    }

    // ===== Helper Methods =====

    private static BlinkDebitConfig createMockConfig() {
        try {
            return BlinkDebitConfig.builder()
                    .debitUrl("https://test.example.com")
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .build();
        } catch (BlinkInvalidValueException e) {
            throw new RuntimeException(e);
        }
    }

    private String createJwtToken(Instant expiresAt) {
        return JWT.create()
                .withIssuer("test-issuer")
                .withSubject("test-subject")
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.none());
    }

    // ===== Stub OAuth Client =====

    private static class StubOAuthApiClient extends OAuthApiClient {
        private final String[] tokens;
        private final Integer expiresIn;
        private final AtomicInteger callCount = new AtomicInteger(0);

        StubOAuthApiClient(String... tokens) {
            this(null, tokens);
        }

        StubOAuthApiClient(Integer expiresIn, String... tokens) {
            super(null, createMockConfig(), null);
            this.expiresIn = expiresIn;
            this.tokens = tokens;
        }

        @Override
        public AccessTokenResponse generateAccessToken() {
            int index = callCount.getAndIncrement();
            // Return the last token if called more times
            String token = index < tokens.length ? tokens[index] : tokens[tokens.length - 1];

            AccessTokenResponse response = new AccessTokenResponse();
            response.setAccessToken(token);
            response.setExpiresIn(expiresIn);
            return response;
        }

        int getCallCount() {
            return callCount.get();
        }
    }
}
