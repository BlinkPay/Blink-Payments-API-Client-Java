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
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.AuthFlowDetail;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.ConsentDetail;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.FlowHint;
import nz.co.blink.debit.dto.v1.GatewayFlow;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.Payment;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.QuickPaymentRequest;
import nz.co.blink.debit.dto.v1.QuickPaymentResponse;
import nz.co.blink.debit.dto.v1.RedirectFlow;
import nz.co.blink.debit.dto.v1.RedirectFlowHint;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.exception.BlinkConsentRejectedException;
import nz.co.blink.debit.exception.BlinkConsentTimeoutException;
import nz.co.blink.debit.exception.BlinkResourceNotFoundException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * The integration test for {@link QuickPaymentsApiClient}.
 */
@SpringBootTest(classes = {AccessTokenHandler.class, OAuthApiClient.class, SingleConsentsApiClient.class,
        EnduringConsentsApiClient.class, QuickPaymentsApiClient.class, PaymentsApiClient.class, RefundsApiClient.class,
        MetaApiClient.class, BlinkDebitClient.class})
@Import(BlinkDebitConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BlinkDebitClientIntegrationTest {

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    @Autowired
    private BlinkDebitClient client;

    @Test
    @DisplayName("Verify that timed out single consent is handled")
    @Order(1)
    void awaitTimedOutSingleConsentThenThrowRuntimeException() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response).isNotNull();
        String redirectUri = response.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        BlinkServiceException exception = catchThrowableOfType(BlinkServiceException.class,
                () -> client.awaitAuthorisedSingleConsent(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent single consent is handled")
    @Order(2)
    void awaitNonExistentSingleConsentThenThrowRuntimeException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitAuthorisedSingleConsent(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that single consent with decoupled flow is retrieved")
    @Order(3)
    void awaitAuthorisedSingleConsent() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedSingleConsent(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that timed out single consent is handled")
    @Order(4)
    void awaitTimedOutSingleConsentThenThrowConsentTimeoutException() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response).isNotNull();
        String redirectUri = response.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        BlinkConsentTimeoutException exception = catchThrowableOfType(BlinkConsentTimeoutException.class,
                () -> client.awaitAuthorisedSingleConsentOrThrowException(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent single consent is handled")
    @Order(5)
    void awaitNonExistentSingleConsentThenThrowResourceNotFoundException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitAuthorisedSingleConsentOrThrowException(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that single consent with decoupled flow is retrieved")
    @Order(6)
    void awaitAuthorisedSingleConsentOrThrowException() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedSingleConsentOrThrowException(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that timed out enduring consent with redirect flow is handled")
    @Order(11)
    void awaitTimedOutEnduringConsentThenThrowRuntimeException() throws BlinkServiceException {
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
                .fromTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createEnduringConsent(request);
        assertThat(response).isNotNull();
        String redirectUri = response.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        BlinkServiceException exception = catchThrowableOfType(BlinkServiceException.class,
                () -> client.awaitAuthorisedEnduringConsent(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent enduring consent is handled")
    @Order(12)
    void awaitNonExistentEnduringConsentThenThrowRuntimeException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitAuthorisedEnduringConsent(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that enduring consent with decoupled flow is retrieved")
    @Order(13)
    void awaitAuthorisedEnduringConsent() throws BlinkServiceException {
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
                .fromTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createEnduringConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedEnduringConsent(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        EnduringConsentRequest detail = (EnduringConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(EnduringConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.ENDURING);
        assertThat(detail.getPeriod()).isEqualTo(Period.MONTHLY);
        assertThat(detail.getFromTimestamp()).isNotNull();
        assertThat(detail.getMaximumAmountPeriod())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
        assertThat(detail.getMaximumAmountPayment())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that timed out enduring consent is handled")
    @Order(14)
    void awaitTimedOutEnduringConsentThenThrowConsentTimeoutException() throws BlinkServiceException {
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
                .fromTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createEnduringConsent(request);
        assertThat(response).isNotNull();
        String redirectUri = response.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        BlinkConsentTimeoutException exception = catchThrowableOfType(BlinkConsentTimeoutException.class,
                () -> client.awaitAuthorisedEnduringConsentOrThrowException(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent enduring consent is handled")
    @Order(15)
    void awaitNonExistentEnduringConsentThenThrowResourceNotFoundException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitAuthorisedEnduringConsentOrThrowException(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that enduring consent with decoupled flow is retrieved")
    @Order(16)
    void awaitAuthorisedEnduringConsentOrThrowException() throws BlinkServiceException {
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
                .fromTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateConsentResponse response = client.createEnduringConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedEnduringConsentOrThrowException(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        EnduringConsentRequest detail = (EnduringConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(EnduringConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.ENDURING);
        assertThat(detail.getPeriod()).isEqualTo(Period.MONTHLY);
        assertThat(detail.getFromTimestamp()).isNotNull();
        assertThat(detail.getMaximumAmountPeriod())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
        assertThat(detail.getMaximumAmountPayment())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "50.00");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that timed out quick payment is handled")
    @Order(21)
    void awaitTimedOutQuickPaymentThenThrowRuntimeException() throws BlinkServiceException {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateQuickPaymentResponse response = client.createQuickPayment(request);
        assertThat(response).isNotNull();
        String redirectUri = response.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID quickPaymentId = response.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        BlinkServiceException exception = catchThrowableOfType(BlinkServiceException.class,
                () -> client.awaitSuccessfulQuickPayment(quickPaymentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent quick payment is handled")
    @Order(22)
    void awaitNonExistentQuickPaymentThenThrowRuntimeException() {
        UUID quickPaymentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitSuccessfulQuickPayment(quickPaymentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment with ID [" + quickPaymentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that quick payment with decoupled flow is retrieved")
    @Order(23)
    @Disabled("temporarily disabled to run in GitHub")
    void awaitSuccessfulQuickPayment() throws BlinkServiceException {
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

        CreateQuickPaymentResponse response = client.createQuickPayment(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getRedirectUri)
                .isNull();
        UUID quickPaymentId = response.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        QuickPaymentResponse actual = client.awaitSuccessfulQuickPayment(quickPaymentId, 2);
        assertThat(actual)
                .isNotNull()
                .extracting(QuickPaymentResponse::getQuickPaymentId)
                .isEqualTo(quickPaymentId);
        Consent consent = actual.getConsent();
        assertThat(consent)
                .isNotNull()
                .extracting(Consent::getConsentId)
                .isEqualTo(quickPaymentId);
        assertThat(consent.getStatus())
                .isNotNull()
                .isIn(Consent.StatusEnum.CONSUMED, Consent.StatusEnum.AUTHORISED);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        List<Payment> payments = consent.getPayments();
        assertThat(payments).isNotNull();
        if (!payments.isEmpty()) {
            assertThat(payments)
                    .hasSize(1)
                    .first()
                    .extracting(Payment::getType, Payment::getStatus, Payment::getRefunds)
                    .containsExactly(Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                            Collections.emptyList());
        }
        assertThat(consent.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that timed out quick payment is handled")
    @Order(24)
    void awaitTimedOutQuickPaymentThenThrowConsentTimeoutException() throws BlinkServiceException {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new RedirectFlow()
                                .bank(Bank.PNZ)
                                .redirectUri(REDIRECT_URI)
                                .redirectToApp(true)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateQuickPaymentResponse actual = client.createQuickPayment(request);
        assertThat(actual).isNotNull();
        String redirectUri = actual.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
        UUID quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        BlinkConsentTimeoutException exception = catchThrowableOfType(BlinkConsentTimeoutException.class,
                () -> client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 2));

        assertThat(exception)
                .isNotNull()
                .isInstanceOf(BlinkConsentTimeoutException.class)
                .hasMessage("Consent timed out");
    }

    @Test
    @DisplayName("Verify that non-existent quick payment is handled")
    @Order(25)
    void awaitNonExistentQuickPaymentThenThrowResourceNotFoundException() {
        UUID quickPaymentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment with ID [" + quickPaymentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that revoked quick payment with gateway flow is handled")
    @Order(26)
    void awaitRevokedQuickPaymentThenThrowRejectedException() throws BlinkServiceException {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow()
                        .detail(new GatewayFlow()
                                .flowHint(new RedirectFlowHint()
                                        .type(FlowHint.TypeEnum.REDIRECT)
                                        .bank(Bank.PNZ))
                                .redirectUri(REDIRECT_URI)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("1.25"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        CreateQuickPaymentResponse actual = client.createQuickPayment(request);
        assertThat(actual).isNotNull();
        UUID quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();
        String redirectUri = actual.getRedirectUri();
        assertThat(redirectUri)
                .isNotBlank()
                .endsWith("/gateway/pay?id=" + quickPaymentId);

        client.revokeQuickPayment(quickPaymentId);

        BlinkConsentRejectedException exception = catchThrowableOfType(BlinkConsentRejectedException.class,
                () -> client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 2));
        assertThat(exception)
                .isNotNull()
                .hasMessage("Quick payment [" + quickPaymentId + "] has been rejected or revoked");
    }

    @Test
    @DisplayName("Verify that quick payment with decoupled flow is retrieved")
    @Order(27)
    void awaitSuccessfulQuickPaymentOrThrowException() throws BlinkServiceException {
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

        CreateQuickPaymentResponse response = client.createQuickPayment(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getRedirectUri)
                .isNull();
        UUID quickPaymentId = response.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        QuickPaymentResponse actual = client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(QuickPaymentResponse::getQuickPaymentId)
                .isEqualTo(quickPaymentId);
        Consent consent = actual.getConsent();
        assertThat(consent)
                .isNotNull()
                .extracting(Consent::getConsentId)
                .isEqualTo(quickPaymentId);
        assertThat(consent.getStatus())
                .isNotNull()
                .isIn(Consent.StatusEnum.AUTHORISED, Consent.StatusEnum.CONSUMED);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        List<Payment> payments = consent.getPayments();
        assertThat(payments).isNotNull();
        if (!payments.isEmpty()) {
            assertThat(payments)
                    .hasSize(1);
            Payment payment = payments.get(0);
            assertThat(payment.getType()).isEqualTo(Payment.TypeEnum.SINGLE);
            assertThat(payment.getStatus()).isIn(Payment.StatusEnum.PENDING, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED);
            assertThat(payment.getRefunds()).isEqualTo(Collections.emptyList());
        }
        assertThat(consent.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
    }

    @Test
    @DisplayName("Verify that non-existent payment is handled")
    @Order(31)
    void awaitNonExistentPaymentThenThrowRuntimeException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitSuccessfulPayment(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that payment is retrieved")
    @Order(32)
    void awaitSuccessfulPayment() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedSingleConsent(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);

        PaymentRequest paymentRequest = new PaymentRequest()
                .consentId(consentId);

        PaymentResponse paymentResponse = client.createPayment(paymentRequest);

        assertThat(paymentResponse).isNotNull();
        UUID paymentId = paymentResponse.getPaymentId();

        Payment payment = client.awaitSuccessfulPayment(paymentId, 30);

        assertThat(payment)
                .isNotNull()
                .extracting(Payment::getType, Payment::getStatus)
                .containsExactly(Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED);
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getCreationTimestamp()).isNotNull();
        assertThat(payment.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(payment.getDetail())
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, new Amount().currency(Amount.CurrencyEnum.NZD).total("1.25"));
        assertThat(payment.getRefunds()).isEmpty();
    }

    @Test
    @DisplayName("Verify that non-existent payment is handled")
    @Order(33)
    void awaitNonExistentPaymentThenThrowResourceNotFoundException() {
        UUID consentId = UUID.randomUUID();

        BlinkResourceNotFoundException exception = catchThrowableOfType(BlinkResourceNotFoundException.class,
                () -> client.awaitSuccessfulPaymentOrThrowException(consentId, 2));

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment with ID [" + consentId + "] does not exist");
    }

    @Test
    @DisplayName("Verify that payment is retrieved")
    @Order(34)
    void awaitSuccessfulPaymentOrThrowException() throws BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse response = client.createSingleConsent(request);
        assertThat(response)
                .isNotNull()
                .extracting(CreateConsentResponse::getRedirectUri)
                .isNull();
        UUID consentId = response.getConsentId();
        assertThat(consentId).isNotNull();

        Consent actual = client.awaitAuthorisedSingleConsent(consentId, 30);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getConsentId, Consent::getStatus)
                .containsExactly(consentId, Consent.StatusEnum.AUTHORISED);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(actual.getPayments()).isEmpty();
        assertThat(actual.getCardNetwork()).isNull();
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
        assertThat(detail)
                .isNotNull()
                .extracting(SingleConsentRequest::getType)
                .isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
        assertThat(detail.getFlow()).isNotNull();
        DecoupledFlow flowDetail = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flowDetail)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);

        PaymentRequest paymentRequest = new PaymentRequest()
                .consentId(consentId);

        PaymentResponse paymentResponse = client.createPayment(paymentRequest);

        assertThat(paymentResponse).isNotNull();
        UUID paymentId = paymentResponse.getPaymentId();

        Payment payment = client.awaitSuccessfulPaymentOrThrowException(paymentId, 30);

        assertThat(payment)
                .isNotNull()
                .extracting(Payment::getType, Payment::getStatus)
                .containsExactly(Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED);
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getCreationTimestamp()).isNotNull();
        assertThat(payment.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(payment.getDetail())
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, new Amount().currency(Amount.CurrencyEnum.NZD).total("1.25"));
        assertThat(payment.getRefunds()).isEmpty();
    }
}
