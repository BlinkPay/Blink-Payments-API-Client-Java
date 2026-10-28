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

import lombok.extern.slf4j.Slf4j;
import nz.co.blink.debit.dto.v1.AccessTokenRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nz.co.blink.debit.enums.BlinkDebitConstant.CORRELATION_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * The helper class for handling HTTP requests.
 */
@Slf4j
public final class RequestHandler {

    /**
     * Private constructor.
     */
    private RequestHandler() {
        // prevent instantiation
    }

    /**
     * Logs the HTTP requests to external APIs.
     *
     * @param request          the HTTP request body
     * @param clientRequest    the {@link ClientRequest}
     * @param exchangeFunction the {@link ExchangeFunction}
     * @return the {@link Mono} containing the {@link ClientResponse}
     */
    public static Mono<ClientResponse> logRequest(Object request, ClientRequest clientRequest,
                                                  ExchangeFunction exchangeFunction) {
        // clone the request to add a new correlation ID even for retries
        ClientRequest modifiedClientRequest = ClientRequest.from(clientRequest)
                .header(CORRELATION_ID.getValue(), UUID.randomUUID().toString())
                .build();

        if (request instanceof AccessTokenRequest) {
            log.debug("Action: {} {}\nHeaders: {}\nBody: {}", clientRequest.method(), clientRequest.url(),
                    sanitiseHeaders(modifiedClientRequest.headers()), "***REDACTED ACCESS TOKEN REQUEST PAYLOAD***");
        } else {
            log.debug("Action: {} {}\nHeaders: {}\nBody: {}", clientRequest.method(), clientRequest.url(),
                    sanitiseHeaders(modifiedClientRequest.headers()), request);
        }

        return exchangeFunction.exchange(modifiedClientRequest);
    }

    private static Map<String, String> sanitiseHeaders(HttpHeaders headers) {
        Map<String, String> map = new HashMap<>(headers.toSingleValueMap());

        String authorization = map.get(AUTHORIZATION);
        if (StringUtils.isNotBlank(authorization)) {
            map.replace(AUTHORIZATION, "***REDACTED BEARER TOKEN***");
        }

        return map;
    }
}
