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

import nz.co.blink.debit.config.BlinkDebitConfiguration;
import nz.co.blink.debit.dto.v1.AccessTokenResponse;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The integration test for {@link OAuthApiClient}.
 */
@SpringBootTest(classes = {OAuthApiClient.class})
@Import(BlinkDebitConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class OAuthApiClientIntegrationTest {

    @Autowired
    private OAuthApiClient client;

    @Test
    @DisplayName("Verify that access token is generated")
    void generateAccessToken() throws BlinkServiceException {
        Mono<AccessTokenResponse> accessTokenResponseMono = client.generateAccessToken(UUID.randomUUID().toString());

        assertThat(accessTokenResponseMono).isNotNull();
        AccessTokenResponse actual = accessTokenResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(AccessTokenResponse::getTokenType, AccessTokenResponse::getExpiresIn,
                        AccessTokenResponse::getRefreshToken, AccessTokenResponse::getIdToken)
                .containsExactly("Bearer", 3600, null, null);
        assertThat(actual.getScope()).isNotBlank();
        assertThat(actual.getScope().trim().split("\\s+"))
                .contains("create:payment", "view:payment", "create:single_consent", "view:single_consent",
                        "view:metadata", "create:enduring_consent", "view:enduring_consent",
                        "revoke:enduring_consent", "view:transaction", "create:quick_payment",
                        "view:quick_payment", "create:refund", "view:refund", "revoke:single_consent",
                        "manage:consents", "read:meta", "read:accounts", "read:balances", "read:party",
                        "read:transactions");
        assertThat(actual.getAccessToken())
                .isNotBlank()
                .startsWith("ey");
    }
}
