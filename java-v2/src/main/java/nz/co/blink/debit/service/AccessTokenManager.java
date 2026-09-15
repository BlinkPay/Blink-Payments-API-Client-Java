package nz.co.blink.debit.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;

/**
 * Thread-safe manager for OAuth2 access tokens.
 * Automatically refreshes tokens before expiry.
 */
public class AccessTokenManager {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenManager.class);
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60; // Refresh 1 minute before expiry

    private final OAuthApiClient oauthApiClient;
    private volatile String accessToken;
    private volatile Instant tokenExpiresAt;
    private final Object lock = new Object();

    public AccessTokenManager(OAuthApiClient oauthApiClient) {
        this.oauthApiClient = oauthApiClient;
    }

    /**
     * Get a valid access token, refreshing if necessary.
     * Thread-safe.
     */
    public String getAccessToken() {
        // Fast path: token is valid
        if (isTokenValid()) {
            return accessToken;
        }

        // Slow path: token needs refresh
        synchronized (lock) {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return accessToken;
            }

            log.debug("Access token expired or missing, fetching new token");
            refreshToken();
            return accessToken;
        }
    }

    /**
     * Force refresh the access token.
     */
    public void refreshToken() {
        synchronized (lock) {
            try {
                log.debug("Fetching new access token from OAuth server");
                AccessTokenResponse response = oauthApiClient.generateAccessToken();
                String newToken = response.getAccessToken();

                // Decode to get expiry
                DecodedJWT decodedJWT = JWT.decode(newToken);
                Date expiresAt = decodedJWT.getExpiresAt();

                if (expiresAt != null) {
                    this.tokenExpiresAt = expiresAt.toInstant();
                    log.debug("New access token expires at: {}", tokenExpiresAt);
                } else if (response.getExpiresIn() != null) {
                    // No exp claim, so fall back to the lifetime the server reported. A
                    // non-positive lifetime means the token is already dead, so expire it now
                    // and let the next call refresh rather than trusting it for an hour.
                    int expiresIn = response.getExpiresIn();
                    this.tokenExpiresAt = Instant.now().plusSeconds(Math.max(expiresIn, 0));
                    if (expiresIn > 0) {
                        log.debug("Access token has no expiry claim, using expires_in of {}s", expiresIn);
                    } else {
                        log.warn("Access token has no expiry claim and a non-positive expires_in of {}s",
                                expiresIn);
                    }
                } else {
                    this.tokenExpiresAt = Instant.now().plusSeconds(3600);
                    log.warn("Access token has no expiry claim and no expires_in, assuming 1 hour");
                }

                this.accessToken = newToken;

            } catch (Exception e) {
                log.error("Failed to refresh access token", e);
                throw new RuntimeException("Failed to refresh access token", e);
            }
        }
    }

    private boolean isTokenValid() {
        if (accessToken == null || tokenExpiresAt == null) {
            return false;
        }

        // Check if token expires within buffer period
        Instant refreshThreshold = Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS);
        boolean valid = tokenExpiresAt.isAfter(refreshThreshold);

        if (!valid) {
            log.debug("Token expires at {}, refresh threshold {}, needs refresh",
                    tokenExpiresAt, refreshThreshold);
        }

        return valid;
    }

    /**
     * Clear the cached token.
     */
    public void clearToken() {
        synchronized (lock) {
            this.accessToken = null;
            this.tokenExpiresAt = null;
            log.debug("Access token cleared");
        }
    }
}
