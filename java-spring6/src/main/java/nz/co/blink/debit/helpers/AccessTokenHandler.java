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
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The helper class for adding Authorization request header. If the client code does not have a shared access token
 * or if it has expired, this helper class will fetch a new one.
 */
@Component
public class AccessTokenHandler {

    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60;

    private final OAuthApiClient client;

    private final AtomicReference<String> accessTokenAtomicReference = new AtomicReference<>();

    private final AtomicReference<Mono<AccessTokenResponse>> tokenFetchInProgress = new AtomicReference<>();

    /**
     * Default constructor.
     *
     * @param client the {@link OAuthApiClient}
     */
    @Autowired
    public AccessTokenHandler(OAuthApiClient client) {
        this.client = client;
    }

    /**
     * FOR TEST PURPOSES ONLY.
     * Stores the access token from the environment variable.
     *
     * @param accessToken the access token
     */
    @Value("${blinkpay.access.token:}")
    public void setAccessTokenAtomicReference(final String accessToken) {
        if (StringUtils.isNotBlank(accessToken)) {
            accessTokenAtomicReference.set(accessToken);
        }
    }

    /**
     * Returns the in-flight token fetch, starting one if there is none.
     * <p>
     * Concurrent callers collapse onto a single request, which is released once it settles so that the
     * next refresh goes back to the server instead of replaying the old response. A subscriber that
     * cancels mid-flight also releases it, so a caller arriving in that window may start a second
     * request; that costs a duplicate fetch rather than correctness.
     *
     * @param requestId the correlation ID
     * @return the {@link Mono} of the {@link AccessTokenResponse}
     */
    private Mono<AccessTokenResponse> obtainAccessToken(final String requestId) {
        while (true) {
            Mono<AccessTokenResponse> inFlight = tokenFetchInProgress.get();
            if (inFlight != null) {
                return inFlight;
            }

            // The fetch has to be able to clear itself, so publish the reference it clears.
            // An AtomicReference is required: doFinally runs on whichever thread terminates
            // the fetch, so the write has to be visible to it.
            AtomicReference<Mono<AccessTokenResponse>> published = new AtomicReference<>();
            Mono<AccessTokenResponse> fetch = client.generateAccessToken(requestId)
                    .cache() // Cache the result for concurrent subscribers
                    .doFinally(signal -> tokenFetchInProgress.compareAndSet(published.get(), null));
            published.set(fetch);

            if (tokenFetchInProgress.compareAndSet(null, fetch)) {
                return fetch;
            }
            // Another thread published first; retry and reuse its fetch. The discarded Mono is
            // pure assembly - generateAccessToken defers, so nothing was sent.
        }
    }

    /**
     * Sets the Authorization request header by reusing the access token or by replacing it with a new one
     * if it has expired. Thread-safe to prevent duplicate token fetches.
     *
     * @param requestId the correlation ID
     * @return the {@link ExchangeFilterFunction}
     */
    public ExchangeFilterFunction setAccessToken(final String requestId) {
        String currentAccessToken = accessTokenAtomicReference.get();
        if (StringUtils.isNotBlank(currentAccessToken)) {
            try {
                DecodedJWT jwt = JWT.decode(currentAccessToken);
                Date expiresAt = jwt.getExpiresAt();
                // Refresh ahead of expiry so a token cannot lapse in flight, and treat a token
                // with no expiry claim as unusable rather than trusting it indefinitely.
                if (expiresAt != null && expiresAt.toInstant()
                        .isAfter(Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
                    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                        String authorization = "Bearer " + currentAccessToken;
                        ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, authorization))
                                .build();
                        return Mono.just(authorizedRequest);
                    });
                }
            } catch (JWTDecodeException ignored) {
                // fetch a new access token
            }
        }

        Mono<AccessTokenResponse> accessTokenResponseMono = obtainAccessToken(requestId);

        return ExchangeFilterFunction.ofRequestProcessor(clientRequest ->
                accessTokenResponseMono.flatMap(accessTokenResponse -> {
                    String newAccessToken = accessTokenResponse.getAccessToken();
                    accessTokenAtomicReference.set(newAccessToken);

                    String authorization = "Bearer " + newAccessToken;
                    ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .build();
                    return Mono.just(authorizedRequest);
                }));
    }
}
