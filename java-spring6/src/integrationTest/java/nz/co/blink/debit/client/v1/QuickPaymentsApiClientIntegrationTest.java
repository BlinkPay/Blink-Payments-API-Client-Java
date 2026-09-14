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
import nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.DecoupledFlowHint;
import nz.co.blink.debit.dto.v1.FlowHint;
import nz.co.blink.debit.dto.v1.GatewayFlow;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.QuickPaymentRequest;
import nz.co.blink.debit.dto.v1.QuickPaymentResponse;
import nz.co.blink.debit.dto.v1.RedirectFlow;
import nz.co.blink.debit.dto.v1.RedirectFlowHint;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.exception.BlinkResourceNotFoundException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
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
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * The integration test for {@link QuickPaymentsApiClient}.
 */
@SpringBootTest(classes = {AccessTokenHandler.class, OAuthApiClient.class, QuickPaymentsApiClient.class})
@Import(BlinkDebitConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuickPaymentsApiClientIntegrationTest {

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    @Autowired
    private QuickPaymentsApiClient client;

    private static UUID quickPaymentId;

    @Test
    @DisplayName("Verify that quick payment with redirect flow is created in PNZ")
    @Order(1)
    void createQuickPaymentWithRedirectFlowInPnz() throws BlinkServiceException {
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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono = client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual).isNotNull();

        quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();
        assertThat(actual.getRedirectUri())
                .isNotBlank()
                .startsWith("https://api-nomatls.apicentre.middleware.co.nz/oauth/v2.0/authorize?scope=openid%20payments&response_type=code%20id_token&request=");
    }

    @Test
    @DisplayName("Verify that quick payment with redirect flow is retrieved from PNZ")
    @Order(2)
    void getQuickPaymentWithRedirectFlowFromPnz() throws BlinkServiceException {
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
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
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
    @DisplayName("Verify that quick payment with redirect flow is revoked in PNZ")
    @Order(3)
    void revokeQuickPaymentWithRedirectFlowInPnz() throws BlinkServiceException {
        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId).block());

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
                .containsExactly(Consent.StatusEnum.REVOKED, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
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
    @DisplayName("Verify that rejected/timed out quick payment with redirect flow is retrieved from PNZ")
    @Order(4)
    void getRejectedQuickPaymentWithRedirectFlowFromPnz() throws BlinkServiceException {
        Mono<QuickPaymentResponse> quickPaymentResponseMono =
                client.getQuickPayment(UUID.fromString("057a08f7-4ee1-499d-8726-e4fe802d64fc"));

        assertThat(quickPaymentResponseMono).isNotNull();

        try {
            QuickPaymentResponse actual = quickPaymentResponseMono.block();
            assertThat(actual)
                    .isNotNull()
                    .extracting(QuickPaymentResponse::getQuickPaymentId)
                    .isEqualTo(UUID.fromString("057a08f7-4ee1-499d-8726-e4fe802d64fc"));
            Consent consent = actual.getConsent();
            assertThat(consent)
                    .isNotNull()
                    .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                    .containsExactly(Consent.StatusEnum.REJECTED, Collections.emptyList(), null);
            assertThat(consent.getCreationTimestamp()).isNotNull();
            assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
            assertThat(consent.getDetail())
                    .isNotNull()
                    .isInstanceOf(SingleConsentRequest.class);
            SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
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
                    .containsExactly(Amount.CurrencyEnum.NZD, "1.50");
        } catch (RuntimeException e) {
            assertThat(e.getCause())
                    .isInstanceOf(BlinkResourceNotFoundException.class)
                    .hasMessage("Quick payment with ID [057a08f7-4ee1-499d-8726-e4fe802d64fc] does not exist");
        }
    }

    @Test
    @DisplayName("Verify that quick payment with decoupled flow is created in PNZ")
    @Order(5)
    void createQuickPaymentWithDecoupledFlowInPnz() throws BlinkServiceException {
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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono = client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual).isNotNull();

        quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();
        assertThat(actual.getRedirectUri()).isBlank();
    }

    @Test
    @DisplayName("Verify that quick payment with decoupled flow is retrieved from PNZ")
    @Order(6)
    void getQuickPaymentWithDecoupledFlowFromPnz() throws BlinkServiceException {
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
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(DecoupledFlow.class);
        DecoupledFlow flow = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
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
    @DisplayName("Verify that quick payment with gateway flow and decoupled flow hint is revoked in PNZ")
    @Order(7)
    void revokeQuickPaymentWithDecoupledFlowInPnz() throws BlinkServiceException {
        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId).block());

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
                .containsExactly(Consent.StatusEnum.REVOKED, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(DecoupledFlow.class);
        DecoupledFlow flow = (DecoupledFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .isNotNull()
                .extracting(DecoupledFlow::getType, DecoupledFlow::getBank, DecoupledFlow::getIdentifierType,
                        DecoupledFlow::getIdentifierValue, DecoupledFlow::getCallbackUrl)
                .containsExactly(AuthFlowDetail.TypeEnum.DECOUPLED, Bank.PNZ, IdentifierType.PHONE_NUMBER,
                        "+64-259531933", CALLBACK_URL);
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
    @DisplayName("Verify that quick payment with gateway flow and redirect flow hint is created in PNZ")
    @Order(8)
    void createQuickPaymentWithGatewayFlowAndRedirectFlowHintInPnz() throws BlinkServiceException {
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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono = client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual).isNotNull();

        quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        assertThat(actual.getRedirectUri())
                .isNotBlank()
                .endsWith("/gateway/pay?id=" + quickPaymentId);
    }

    @Test
    @DisplayName("Verify that quick payment with gateway flow and redirect flow hint is retrieved from PNZ")
    @Order(9)
    void getQuickPaymentWithGatewayFlowAndRedirectFlowHintFromPnz() throws BlinkServiceException {
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
                .containsExactly(Consent.StatusEnum.GATEWAYAWAITINGSUBMISSION, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(GatewayFlow.class);
        GatewayFlow flow = (GatewayFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .extracting(GatewayFlow::getType, GatewayFlow::getRedirectUri)
                .containsExactly(AuthFlowDetail.TypeEnum.GATEWAY, REDIRECT_URI);
        assertThat(flow.getFlowHint())
                .isNotNull()
                .isInstanceOf(RedirectFlowHint.class);
        RedirectFlowHint flowHint = (RedirectFlowHint) flow.getFlowHint();
        assertThat(flowHint)
                .isNotNull()
                .extracting(RedirectFlowHint::getType, RedirectFlowHint::getBank)
                .containsExactly(FlowHint.TypeEnum.REDIRECT, Bank.PNZ);
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
    @DisplayName("Verify that quick payment with gateway flow and redirect flow hint is revoked in PNZ")
    @Order(10)
    void revokeQuickPaymentWithGatewayFlowAndRedirectFlowHintInPnz() throws BlinkServiceException {
        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId).block());

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
                .containsExactly(Consent.StatusEnum.REVOKED, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(GatewayFlow.class);
        GatewayFlow flow = (GatewayFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .extracting(GatewayFlow::getType, GatewayFlow::getRedirectUri)
                .containsExactly(AuthFlowDetail.TypeEnum.GATEWAY, REDIRECT_URI);
        assertThat(flow.getFlowHint())
                .isNotNull()
                .isInstanceOf(RedirectFlowHint.class);
        RedirectFlowHint flowHint = (RedirectFlowHint) flow.getFlowHint();
        assertThat(flowHint)
                .isNotNull()
                .extracting(RedirectFlowHint::getType, RedirectFlowHint::getBank)
                .containsExactly(FlowHint.TypeEnum.REDIRECT, Bank.PNZ);
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
    @DisplayName("Verify that quick payment with gateway flow and decoupled flow hint is created in PNZ")
    @Order(11)
    void createQuickPaymentWithGatewayFlowAndDecoupledFlowHintInPnz() throws BlinkServiceException {
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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono = client.createQuickPayment(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual).isNotNull();

        quickPaymentId = actual.getQuickPaymentId();
        assertThat(quickPaymentId).isNotNull();

        assertThat(actual.getRedirectUri())
                .isNotBlank()
                .endsWith("/gateway/pay?id=" + quickPaymentId);
    }

    @Test
    @DisplayName("Verify that quick payment with gateway flow and decoupled flow hint is retrieved from PNZ")
    @Order(12)
    void getQuickPaymentWithGatewayFlowAndDecoupledFlowHintFromPnz() throws BlinkServiceException {
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
                .containsExactly(Consent.StatusEnum.GATEWAYAWAITINGSUBMISSION, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(GatewayFlow.class);
        GatewayFlow flow = (GatewayFlow) detail.getFlow().getDetail();
        assertThat(flow)
                .extracting(GatewayFlow::getType, GatewayFlow::getRedirectUri)
                .containsExactly(AuthFlowDetail.TypeEnum.GATEWAY, REDIRECT_URI);
        assertThat(flow.getFlowHint())
                .isNotNull()
                .isInstanceOf(DecoupledFlowHint.class);
        DecoupledFlowHint flowHint = (DecoupledFlowHint) flow.getFlowHint();
        assertThat(flowHint)
                .isNotNull()
                .extracting(DecoupledFlowHint::getType, DecoupledFlowHint::getBank,
                        DecoupledFlowHint::getIdentifierType, DecoupledFlowHint::getIdentifierValue)
                .containsExactly(FlowHint.TypeEnum.DECOUPLED, Bank.PNZ,
                        IdentifierType.PHONE_NUMBER, "+64-259531933");
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
    @DisplayName("Verify that quick payment with gateway flow and decoupled flow hint is revoked in PNZ")
    @Order(13)
    void revokeQuickPaymentWithGatewayFlowAndDecoupledFlowHintInPnz() throws BlinkServiceException {
        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId).block());

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
                .containsExactly(Consent.StatusEnum.REVOKED, Collections.emptyList(), null);
        assertThat(consent.getCreationTimestamp()).isNotNull();
        assertThat(consent.getStatusUpdatedTimestamp()).isNotNull();
        assertThat(consent.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) consent.getDetail();
        assertThat(detail.getType()).isEqualTo(ConsentDetail.TypeEnum.SINGLE);
        assertThat(detail.getFlow()).isNotNull();
        assertThat(detail.getFlow().getDetail())
                .isNotNull()
                .isInstanceOf(GatewayFlow.class);
        GatewayFlow flow = (GatewayFlow) detail.getFlow().getDetail();
        assertThat(flow.getFlowHint())
                .isNotNull()
                .isInstanceOf(DecoupledFlowHint.class);
        DecoupledFlowHint flowHint = (DecoupledFlowHint) flow.getFlowHint();
        assertThat(flowHint)
                .isNotNull()
                .extracting(DecoupledFlowHint::getType, DecoupledFlowHint::getBank,
                        DecoupledFlowHint::getIdentifierType, DecoupledFlowHint::getIdentifierValue)
                .containsExactly(FlowHint.TypeEnum.DECOUPLED, Bank.PNZ,
                        IdentifierType.PHONE_NUMBER, "+64-259531933");
        assertThat(detail.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly("particulars", "code", "reference");
        assertThat(detail.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "1.25");
    }
}
