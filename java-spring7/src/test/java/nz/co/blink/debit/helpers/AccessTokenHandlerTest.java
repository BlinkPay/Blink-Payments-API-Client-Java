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

import nz.co.blink.debit.client.v1.OAuthApiClient;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link AccessTokenHandler}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AccessTokenHandlerTest {

    @Mock
    private OAuthApiClient client;

    @InjectMocks
    private AccessTokenHandler handler;

    @Test
    @DisplayName("Verify that new access token is generated")
    void setAccessTokenOnFirstRequest() throws BlinkServiceException {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("header.payload.signature");
        when(client.generateAccessToken(any(String.class)))
                .thenReturn(Mono.just(accessTokenResponse));

        String body = WebClient.builder()
                .filter(handler.setAccessToken(UUID.randomUUID().toString()))
                .exchangeFunction(clientRequest ->
                        Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", "plain/text")
                                .body("Bearer header.payload.signature")
                                .build()))
                .build()
                .get()
                .uri("http://localhost:8080/index.html")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(body).isEqualTo("Bearer " + accessTokenResponse.getAccessToken());
        verify(client).generateAccessToken(any(String.class));
    }

    @Test
    @DisplayName("Verify that existing access token is reused")
    @Disabled("Replace ACCESS_TOKEN environment variable value before running")
    void setAccessTokenWhenAccessTokenExists() throws BlinkServiceException {
        // ACCESS_TOKEN environment variable value must not be expired
        handler.setAccessTokenAtomicReference(System.getenv("ACCESS_TOKEN"));

        WebClient.builder()
                .filter(handler.setAccessToken(UUID.randomUUID().toString()))
                .exchangeFunction(clientRequest ->
                        Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", "plain/text")
                                .body("Bearer header.payload.signature")
                                .build()))
                .build()
                .get()
                .uri("http://localhost:8080/index.html")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        verify(client, never()).generateAccessToken(any(String.class));
    }
}
