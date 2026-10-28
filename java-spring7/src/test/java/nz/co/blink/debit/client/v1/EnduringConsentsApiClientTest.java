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

import jakarta.validation.Validation;
import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.AuthFlowDetail;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.ConsentDetail;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.DecoupledFlowHint;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.FlowHint;
import nz.co.blink.debit.dto.v1.GatewayFlow;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.OneOfauthFlowDetail;
import nz.co.blink.debit.dto.v1.OneOfconsentDetail;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.RedirectFlow;
import nz.co.blink.debit.dto.v1.RedirectFlowHint;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import nz.co.blink.debit.service.ValidationService;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static nz.co.blink.debit.enums.BlinkDebitConstant.ENDURING_CONSENTS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link EnduringConsentsApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EnduringConsentsApiClientTest {

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    private static final ZoneId ZONE_ID = ZoneId.of("Pacific/Auckland");

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ReactorClientHttpConnector connector;

    @Spy
    private BlinkPayProperties properties = new BlinkPayProperties();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AccessTokenHandler accessTokenHandler;

    @Spy
    private ValidationService validationService = new JakartaValidationServiceImpl(Validation.buildDefaultValidatorFactory().getValidator());

    @Spy

    @InjectMocks
    private EnduringConsentsApiClient client;

    @Test
    @DisplayName("Verify that null request is handled")
    void createEnduringConsentWithNullRequest() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Enduring consent request must not be null");
    }

    @Test
    @DisplayName("Verify that null authorisation flow is handled")
    void createEnduringConsentWithNullAuthorisationFlow() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Authorisation flow must not be null");
    }

    @Test
    @DisplayName("Verify that null authorisation flow detail is handled")
    void createEnduringConsentWithNullAuthorisationFlowDetail() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow())
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Authorisation flow detail must not be null");
    }

    @Test
    @DisplayName("Verify that null period is handled")
    void createEnduringConsentWithRedirectFlowAndNullPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Period must not be null");
    }

    @Test
    @DisplayName("Verify that null start date is handled")
    void createEnduringConsentWithRedirectFlowAndNullStartDate() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Start date must not be null");
    }

    @Test
    @DisplayName("Verify that null maximum amount period is handled")
    void createEnduringConsentWithRedirectFlowAndNullMaximumAmountPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Maximum amount period must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createEnduringConsentWithRedirectFlowAndNullCurrency() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createEnduringConsentWithRedirectFlowAndInvalidTotal(String total) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for enduring consent request");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createEnduringConsentWithRedirectFlowAndNullBank() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that redirect flow with blank redirect URI is handled")
    void createEnduringConsentWithRedirectFlowAndBlankRedirectUri(String redirectUri) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(redirectUri)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Redirect URI must not be blank");
    }

    @Test
    @DisplayName("Verify that null period is handled")
    void createEnduringConsentWithDecoupledFlowAndNullPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Period must not be null");
    }

    @Test
    @DisplayName("Verify that null start date is handled")
    void createEnduringConsentWithDecoupledFlowAndNullStartDate() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Start date must not be null");
    }

    @Test
    @DisplayName("Verify that null maximum amount period is handled")
    void createEnduringConsentWithDecoupledFlowAndNullMaximumAmountPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Maximum amount period must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createEnduringConsentWithDecoupledFlowAndNullCurrency() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createEnduringConsentWithDecoupledFlowAndInvalidTotal(String total) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for enduring consent request");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createEnduringConsentWithDecoupledFlowAndNullBank() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @Test
    @DisplayName("Verify that decoupled flow with null identifier type is handled")
    void createEnduringConsentWithDecoupledFlowAndNullIdentifierType() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that decoupled flow with blank identifier value is handled")
    void createEnduringConsentWithDecoupledFlowAndBlankIdentifierValue(String identifierValue) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue(identifierValue)
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier value must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that decoupled flow with blank callback URL is handled")
    void createEnduringConsentWithDecoupledFlowAndBlankCallbackUrl(String callbackUrl) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(callbackUrl)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Callback/webhook URL must not be blank");
    }

    @Test
    @DisplayName("Verify that null period is handled")
    void createEnduringConsentWithGatewayFlowAndNullPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Period must not be null");
    }

    @Test
    @DisplayName("Verify that null start date is handled")
    void createEnduringConsentWithGatewayFlowAndNullStartDate() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Start date must not be null");
    }

    @Test
    @DisplayName("Verify that null maximum amount period is handled")
    void createEnduringConsentWithGatewayFlowAndNullMaximumAmountPeriod() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Maximum amount period must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createEnduringConsentWithGatewayFlowAndNullCurrency() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createEnduringConsentWithGatewayFlowAndInvalidTotal(String total) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for enduring consent request");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createEnduringConsentWithGatewayFlowAndNullBank() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint())))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @Test
    @DisplayName("Verify that enduring consent with gateway flow and null flow hint is created")
    @SuppressWarnings("unchecked")
    void createEnduringConsentWithGatewayFlowAndNullFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri("http://localhost:8080");

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ENDURING_CONSENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EnduringConsentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, "http://localhost:8080");
    }

    @Test
    @DisplayName("Verify that null authorisation flow hint type is handled")
    void createEnduringConsentWithGatewayFlowAndNullFlowHintType() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new FlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Flow hint type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that gateway flow with blank redirect URI is handled")
    void createEnduringConsentWithGatewayFlowAndBlankRedirectUri(String redirectUri) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(redirectUri)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Redirect URI must not be blank");
    }

    @Test
    @DisplayName("Verify that gateway flow with null identifier type is handled")
    void createEnduringConsentWithGatewayFlowAndNullIdentifierType() {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that gateway flow with blank identifier value is handled")
    void createEnduringConsentWithGatewayFlowAndBlankIdentifierValue(String identifierValue) {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue(identifierValue)
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createEnduringConsent(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier value must not be blank");
    }

    @Test
    @DisplayName("Verify that null consent ID is handled")
    void getEnduringConsentWithNullConsentId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.getEnduringConsent(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent ID must not be null");
    }

    @Test
    @DisplayName("Verify that enduring consent is retrieved")
    @SuppressWarnings("unchecked")
    void getEnduringConsent() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Consent consent = new Consent()
                .consentId(consentId)
                .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                .creationTimestamp(now.minusHours(1))
                .detail((OneOfconsentDetail) new EnduringConsentRequest()
                        .flow(new AuthFlow()
                                .detail((OneOfauthFlowDetail) new RedirectFlow()
                                        .bank(Bank.PNZ)
                                        .redirectUri(REDIRECT_URI)
                                        .redirectToApp(true)
                                        .type(AuthFlowDetail.TypeEnum.REDIRECT)))
                        .period(Period.MONTHLY)
                        .fromTimestamp(now)
                        .maximumAmountPeriod(new Amount()
                                .currency(Amount.CurrencyEnum.NZD)
                                .total("50.00"))
                        .maximumAmountPayment(new Amount()
                                .currency(Amount.CurrencyEnum.NZD)
                                .total("50.00"))
                        .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e")
                        .type(ConsentDetail.TypeEnum.ENDURING))
                .payments(Collections.emptyList());

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(consent));

        Mono<Consent> consentMono = client.getEnduringConsent(consentId);

        Consent actual = consentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), null);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNull();
        assertThat(actual.getDetail())
                .isNotNull()
                .isInstanceOf(EnduringConsentRequest.class);
        EnduringConsentRequest detail = (EnduringConsentRequest) actual.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.ENDURING);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(RedirectFlow.class);
        RedirectFlow flow = (RedirectFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .extracting(RedirectFlow::getType, RedirectFlow::getBank, RedirectFlow::getRedirectUri)
                .containsExactly(AuthFlowDetail.TypeEnum.REDIRECT, Bank.PNZ, REDIRECT_URI);
        assertThat(detail.getPeriod()).isEqualTo(Period.MONTHLY);
        assertThat(detail.getFromTimestamp()).isEqualTo(now);
        assertThat(detail.getMaximumAmountPeriod())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
        assertThat(detail.getMaximumAmountPayment())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
    }

    @Test
    @DisplayName("Verify that null consent ID is handled")
    void revokeEnduringConsentWithNullConsentId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.revokeEnduringConsent(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent ID must not be null");
    }

    @Test
    @DisplayName("Verify that enduring consent is revoked")
    @SuppressWarnings("unchecked")
    void revokeEnduringConsent() {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeEnduringConsent(consentId).block());
    }

    @Test
    @DisplayName("Verify that enduring consent with redirect flow is created")
    @SuppressWarnings("unchecked")
    void createEnduringConsentWithRedirectFlow() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri("http://localhost:8080");

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ENDURING_CONSENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EnduringConsentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, "http://localhost:8080");
    }

    @Test
    @DisplayName("Verify that enduring consent with decoupled flow is created")
    @SuppressWarnings("unchecked")
    void createEnduringConsentWithDecoupledFlow() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ENDURING_CONSENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EnduringConsentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    @DisplayName("Verify that enduring consent with gateway flow and redirect flow hint is created")
    @SuppressWarnings("unchecked")
    void createEnduringConsentWithGatewayFlowAndRedirectFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri("http://localhost:8080");

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ENDURING_CONSENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EnduringConsentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, "http://localhost:8080");
    }

    @Test
    @DisplayName("Verify that enduring consent with gateway flow and decoupled flow hint is created")
    @SuppressWarnings("unchecked")
    void createEnduringConsentWithGatewayFlowAndDecoupledFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(ENDURING_CONSENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(EnduringConsentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .expiryTimestamp(OffsetDateTime.now(ZONE_ID).plusMonths(11))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }
}
