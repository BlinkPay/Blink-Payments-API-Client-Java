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
package nz.co.blink.debit.client.v1;

import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.dto.v1.AccessTokenRequest;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static nz.co.blink.debit.enums.BlinkDebitConstant.TOKEN_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link OAuthApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OAuthApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ReactorClientHttpConnector connector;

    @Spy
    private BlinkPayProperties properties = new BlinkPayProperties();

    @Spy

    @InjectMocks
    private OAuthApiClient client;

    @Test
    @DisplayName("Verify that access token is generated")
    @SuppressWarnings("unchecked")
    void generateAccessToken() throws BlinkServiceException {
        // GIVEN
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(client, "clientId", "BLINKPAY_CLIENT_ID");
        ReflectionTestUtils.setField(client, "clientSecret", "BLINKPAY_CLIENT_SECRET");

        AccessTokenResponse response = new AccessTokenResponse();
        response.setAccessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setScope("create:payment view:payment create:single_consent view:single_consent view:metadata create:enduring_consent view:enduring_consent revoke:enduring_consent view:transaction create:quick_payment view:quick_payment create:refund view:refund revoke:single_consent");

        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(TOKEN_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(AccessTokenRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        // WHEN
        Mono<AccessTokenResponse> accessTokenResponseMono = client.generateAccessToken(UUID.randomUUID().toString());

        // THEN
        assertThat(accessTokenResponseMono).isNotNull();
        AccessTokenResponse actual = accessTokenResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(AccessTokenResponse::getTokenType, AccessTokenResponse::getExpiresIn,
                        AccessTokenResponse::getRefreshToken, AccessTokenResponse::getIdToken,
                        AccessTokenResponse::getScope)
                .containsExactly("Bearer", 3600, null, null, "create:payment view:payment create:single_consent"
                        + " view:single_consent view:metadata create:enduring_consent view:enduring_consent"
                        + " revoke:enduring_consent view:transaction create:quick_payment view:quick_payment"
                        + " create:refund view:refund revoke:single_consent");
        assertThat(actual.getAccessToken())
                .isNotBlank()
                .startsWith("ey");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that blank client ID is handled")
    void generateAccessTokenWithBlankClientId(String clientId) {
        ReflectionTestUtils.setField(client, "clientId", clientId);
        ReflectionTestUtils.setField(client, "clientSecret", "BLINKPAY_CLIENT_SECRET");

        Throwable thrown = catchThrowable(
                () -> client.generateAccessToken(UUID.randomUUID().toString()).block());

        assertThat(thrown)
                .isNotNull()
                .hasCauseInstanceOf(BlinkInvalidValueException.class);
        assertThat(thrown.getCause())
                .hasMessage("Client ID and client secret must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that blank client secret is handled")
    void generateAccessTokenWithBlankClientSecret(String clientSecret) {
        ReflectionTestUtils.setField(client, "clientId", "BLINKPAY_CLIENT_ID");
        ReflectionTestUtils.setField(client, "clientSecret", clientSecret);

        Throwable thrown = catchThrowable(
                () -> client.generateAccessToken(UUID.randomUUID().toString()).block());

        assertThat(thrown)
                .isNotNull()
                .hasCauseInstanceOf(BlinkInvalidValueException.class);
        assertThat(thrown.getCause())
                .hasMessage("Client ID and client secret must not be blank");
    }
}
