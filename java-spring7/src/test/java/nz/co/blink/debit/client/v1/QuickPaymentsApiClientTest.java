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
import nz.co.blink.debit.dto.v1.CardNetwork;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.ConsentDetail;
import nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.DecoupledFlowHint;
import nz.co.blink.debit.dto.v1.FlowHint;
import nz.co.blink.debit.dto.v1.GatewayFlow;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.OneOfauthFlowDetail;
import nz.co.blink.debit.dto.v1.OneOfconsentDetail;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.QuickPaymentRequest;
import nz.co.blink.debit.dto.v1.QuickPaymentResponse;
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

import static nz.co.blink.debit.enums.BlinkDebitConstant.QUICK_PAYMENTS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link QuickPaymentsApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class QuickPaymentsApiClientTest {

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

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
    private QuickPaymentsApiClient client;

    @Test
    @DisplayName("Verify that null request is handled")
    void createQuickPaymentWithNullRequest() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment request must not be null");
    }

    @Test
    @DisplayName("Verify that null authorisation flow is handled")
    void createQuickPaymentWithNullAuthorisationFlow() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Authorisation flow must not be null");
    }

    @Test
    @DisplayName("Verify that null authorisation flow detail is handled")
    void createQuickPaymentWithNullAuthorisationFlowDetail() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Authorisation flow detail must not be null");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createQuickPaymentWithRedirectFlowAndNullBank() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that redirect flow with blank redirect URI is handled")
    void createQuickPaymentWithRedirectFlowAndBlankRedirectUri(String redirectUri) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(redirectUri)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Redirect URI must not be blank");
    }

    @Test
    @DisplayName("Verify that null amount is handled")
    void createQuickPaymentWithRedirectFlowAndNullAmount() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Amount must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createQuickPaymentWithRedirectFlowAndNullCurrency() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createQuickPaymentWithRedirectFlowAndInvalidTotal(String total) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for quick payment request");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createQuickPaymentWithRedirectFlowAndNullPcr() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .total("1.25"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createQuickPaymentWithRedirectFlowAndBlankParticulars(String particulars) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createQuickPaymentWithRedirectFlowAndLongPcrValues() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not exceed 12 characters");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createQuickPaymentWithDecoupledFlowAndNullBank() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @Test
    @DisplayName("Verify that null amount is handled")
    void createQuickPaymentWithDecoupledFlowAndNullAmount() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Amount must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createQuickPaymentWithDecoupledFlowAndNullCurrency() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createQuickPaymentWithDecoupledFlowAndInvalidTotal(String total) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for quick payment request");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createQuickPaymentWithDecoupledFlowAndNullPcr() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .total("1.25"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createQuickPaymentWithDecoupledFlowAndBlankParticulars(String particulars) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createQuickPaymentWithDecoupledFlowAndLongPcrValues() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not exceed 12 characters");
    }

    @Test
    @DisplayName("Verify that decoupled flow with null identifier type is handled")
    void createQuickPaymentWithDecoupledFlowAndNullIdentifierType() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that decoupled flow with blank redirect URI is handled")
    void createQuickPaymentWithDecoupledFlowAndBlankIdentifierValue(String identifierValue) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue(identifierValue)
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier value must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that decoupled flow with blank callback URL is handled")
    void createQuickPaymentWithDecoupledFlowAndBlankCallbackUrl(String callbackUrl) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(callbackUrl)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Callback/webhook URL must not be blank");
    }

    @Test
    @DisplayName("Verify that null bank is handled")
    void createQuickPaymentWithGatewayFlowAndNullBank() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint())))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Bank must not be null");
    }

    @Test
    @DisplayName("Verify that null amount is handled")
    void createQuickPaymentWithGatewayFlowAndNullAmount() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Amount must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createQuickPaymentWithGatewayFlowAndNullCurrency() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createQuickPaymentWithGatewayFlowAndInvalidTotal(String total) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for quick payment request");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createQuickPaymentWithGatewayFlowAndNullPcr() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createQuickPaymentWithGatewayFlowAndBlankParticulars(String particulars) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createQuickPaymentWithGatewayFlowAndRedirectFlowHintAndLongPcrValues() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not exceed 12 characters");
    }

    @Test
    @DisplayName("Verify that null authorisation flow hint is handled")
    @SuppressWarnings("unchecked")
    void createQuickPaymentWithGatewayFlowAndNullFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(QUICK_PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(QuickPaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    @DisplayName("Verify that null authorisation flow hint type is handled")
    void createQuickPaymentWithGatewayFlowAndNullFlowHintType() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new FlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Flow hint type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that gateway flow with blank redirect URI is handled")
    void createQuickPaymentWithGatewayFlowAndBlankRedirectUri(String redirectUri) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(redirectUri)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Redirect URI must not be blank");
    }

    @Test
    @DisplayName("Verify that gateway flow with null identifier type is handled")
    void createQuickPaymentWithGatewayFlowAndNullIdentifierType() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier type must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Verify that gateway flow with blank redirect URI is handled")
    void createQuickPaymentWithGatewayFlowAndBlankIdentifierValue(String identifierValue) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue(identifierValue)
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Identifier value must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createQuickPaymentWithGatewayFlowAndDecoupledFlowHintAndBlankParticulars(String particulars) {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createQuickPaymentWithGatewayFlowAndDecoupledFlowHintAndLongPcrValues() {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createQuickPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not exceed 12 characters");
    }

    @Test
    @DisplayName("Verify that null quick payment ID is handled")
    void getQuickPaymentWithNullQuickPaymentId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.getQuickPayment(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that quick payment is retrieved")
    @SuppressWarnings("unchecked")
    void getQuickPayment() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        QuickPaymentResponse response = new QuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .consent(new Consent()
                        .consentId(quickPaymentId)
                        .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                        .creationTimestamp(now.minusMinutes(5))
                        .statusUpdatedTimestamp(now)
                        .detail((OneOfconsentDetail) new QuickPaymentRequest()
                                .flow(new AuthFlow()
                                        .detail((OneOfauthFlowDetail) new RedirectFlow()
                                                .bank(Bank.PNZ)
                                                .redirectUri(REDIRECT_URI)
                                                .type(AuthFlowDetail.TypeEnum.REDIRECT)))
                                .pcr(new Pcr()
                                        .particulars("particulars")
                                        .code("code")
                                        .reference("reference"))
                                .amount(new Amount()
                                        .currency(Amount.CurrencyEnum.NZD)
                                        .total("1.25"))
                                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e")
                                .type(ConsentDetail.TypeEnum.SINGLE))
                        .payments(Collections.emptyList())
                        .cardNetwork(CardNetwork.VISA));

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        Mono<QuickPaymentResponse> quickPaymentResponseMono = client.getQuickPayment(quickPaymentId);

        assertThat(quickPaymentResponseMono).isNotNull();
        QuickPaymentResponse actual = quickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(QuickPaymentResponse::getQuickPaymentId)
                .isEqualTo(quickPaymentId);
        Consent consent = actual.getConsent();
        assertThat(consent)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
        assertThat(consent.getCreationTimestamp()).isEqualTo(now.minusMinutes(5));
        assertThat(consent.getStatusUpdatedTimestamp()).isEqualTo(now);
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(QuickPaymentRequest.class);
        QuickPaymentRequest detail = (QuickPaymentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(RedirectFlow.class);
        RedirectFlow flow = (RedirectFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .extracting(RedirectFlow::getType, RedirectFlow::getBank, RedirectFlow::getRedirectUri)
                .containsExactly(AuthFlowDetail.TypeEnum.REDIRECT, Bank.PNZ, REDIRECT_URI);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
    }

    @Test
    @DisplayName("Verify that null quick payment ID is handled")
    void revokeQuickPaymentWithNullQuickPaymentId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.revokeQuickPayment(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that quick payment is revoked")
    @SuppressWarnings("unchecked")
    void revokeQuickPayment() {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId).block());
    }

    @Test
    @DisplayName("Verify that quick payment with redirect flow is created")
    @SuppressWarnings("unchecked")
    void createQuickPaymentWithRedirectFlow() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(QUICK_PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(QuickPaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    @DisplayName("Verify that quick payment with decoupled flow is created")
    @SuppressWarnings("unchecked")
    void createQuickPaymentWithDecoupledFlow() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(QUICK_PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(QuickPaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }

    @Test
    @DisplayName("Verify that quick payment with gateway flow and redirect flow hint is created")
    @SuppressWarnings("unchecked")
    void createQuickPaymentWithGatewayFlowAndRedirectFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(QUICK_PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(QuickPaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new RedirectFlowHint()
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    @DisplayName("Verify that quick payment with gateway flow and decoupled flow hint is created")
    @SuppressWarnings("unchecked")
    void createQuickPaymentWithGatewayFlowAndDecoupledFlowHint() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(QUICK_PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(QuickPaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .redirectUri(REDIRECT_URI)
                                .flowHint(new DecoupledFlowHint()
                                        .identifierType(IdentifierType.PHONE_NUMBER)
                                        .identifierValue("+64-259531933")
                                        .bank(Bank.PNZ))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }
}
