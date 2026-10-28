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
import nz.co.blink.debit.dto.v1.AccountNumberRefundRequest;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.AuthFlowDetail;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.BankMetadata;
import nz.co.blink.debit.dto.v1.BankmetadataFeatures;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesDecoupledFlow;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesDecoupledFlowAvailableIdentifiers;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesEnduringConsent;
import nz.co.blink.debit.dto.v1.BankmetadataRedirectFlow;
import nz.co.blink.debit.dto.v1.CardNetwork;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.ConsentDetail;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.DecoupledFlowHint;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.FullRefundRequest;
import nz.co.blink.debit.dto.v1.GatewayFlow;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.OneOfauthFlowDetail;
import nz.co.blink.debit.dto.v1.OneOfconsentDetail;
import nz.co.blink.debit.dto.v1.PartialRefundRequest;
import nz.co.blink.debit.dto.v1.Payment;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.QuickPaymentRequest;
import nz.co.blink.debit.dto.v1.QuickPaymentResponse;
import nz.co.blink.debit.dto.v1.RedirectFlow;
import nz.co.blink.debit.dto.v1.RedirectFlowHint;
import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundRequest;
import nz.co.blink.debit.dto.v1.RefundResponse;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.service.ValidationService;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link BlinkDebitClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BlinkDebitClientTest {

    @Mock
    private SingleConsentsApiClient singleConsentsApiClient;

    @Mock
    private EnduringConsentsApiClient enduringConsentsApiClient;

    @Mock
    private QuickPaymentsApiClient quickPaymentsApiClient;

    @Mock
    private PaymentsApiClient paymentsApiClient;

    @Mock
    private RefundsApiClient refundsApiClient;

    @Mock
    private MetaApiClient metaApiClient;

    @Spy
    private ValidationService validationService = new JakartaValidationServiceImpl(Validation.buildDefaultValidatorFactory().getValidator());

    @InjectMocks
    private BlinkDebitClient client;

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    private static final ZoneId ZONE_ID = ZoneId.of("Pacific/Auckland");

    private static final BankMetadata BNZ = new BankMetadata()
            .name(Bank.BNZ)
            .features(new BankmetadataFeatures()
                    .decoupledFlow(new BankmetadataFeaturesDecoupledFlow()
                            .enabled(true)
                            .availableIdentifiers(Collections.singletonList(
                                    new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                            .type(IdentifierType.CONSENT_ID)
                                            .name("Consent ID")))
                            .requestTimeout("PT4M")))
            .redirectFlow(new BankmetadataRedirectFlow()
                    .enabled(true)
                    .requestTimeout("PT5M"));

    private static final BankMetadata PNZ = new BankMetadata()
            .name(Bank.PNZ)
            .features(new BankmetadataFeatures()
                    .enduringConsent(new BankmetadataFeaturesEnduringConsent()
                            .enabled(true)
                            .consentIndefinite(true))
                    .decoupledFlow(new BankmetadataFeaturesDecoupledFlow()
                            .enabled(true)
                            .availableIdentifiers(Stream.of(
                                            new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                                    .type(IdentifierType.PHONE_NUMBER)
                                                    .name("Phone Number"),
                                            new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                                    .type(IdentifierType.MOBILE_NUMBER)
                                                    .name("Mobile Number"))
                                    .toList())
                            .requestTimeout("PT3M")))
            .redirectFlow(new BankmetadataRedirectFlow()
                    .enabled(true)
                    .requestTimeout("PT10M"));

    private static final BankMetadata WESTPAC = new BankMetadata()
            .name(Bank.WESTPAC)
            .features(new BankmetadataFeatures())
            .redirectFlow(new BankmetadataRedirectFlow()
                    .enabled(true)
                    .requestTimeout("PT10M"));

    private static final BankMetadata ASB = new BankMetadata()
            .name(Bank.ASB)
            .features(new BankmetadataFeatures())
            .redirectFlow(new BankmetadataRedirectFlow()
                    .enabled(true)
                    .requestTimeout("PT10M"));

    private static final BankMetadata ANZ = new BankMetadata()
            .name(Bank.ANZ)
            .features(new BankmetadataFeatures()
                    .decoupledFlow(new BankmetadataFeaturesDecoupledFlow()
                            .enabled(true)
                            .availableIdentifiers(Stream.of(
                                            new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                                    .type(IdentifierType.MOBILE_NUMBER)
                                                    .name("Mobile Number"))
                                    .toList())
                            .requestTimeout("PT3M")));

    @Test
    void getMeta() throws BlinkServiceException {
        when(metaApiClient.getMeta())
                .thenReturn(Flux.just(BNZ, PNZ, WESTPAC, ASB, ANZ));

        List<BankMetadata> actual = client.getMeta();

        assertThat(actual)
                .isNotNull()
                .hasSize(5)
                .containsExactlyInAnyOrder(BNZ, PNZ, WESTPAC, ASB, ANZ);
    }

    @Test
    void getMetaWithRequestId() throws BlinkServiceException {
        when(metaApiClient.getMeta(anyString()))
                .thenReturn(Flux.just(BNZ, PNZ, WESTPAC, ASB, ANZ));

        List<BankMetadata> actual = client.getMeta(UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .hasSize(5)
                .containsExactlyInAnyOrder(BNZ, PNZ, WESTPAC, ASB, ANZ);
    }

    @Test
    void getMetaAsFlux() throws BlinkServiceException {
        when(metaApiClient.getMeta())
                .thenReturn(Flux.just(BNZ, PNZ, WESTPAC, ASB, ANZ));

        Flux<BankMetadata> actual = client.getMetaAsFlux();

        assertThat(actual).isNotNull();
        Set<BankMetadata> set = new HashSet<>();
        StepVerifier
                .create(actual)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .verifyComplete();
        assertThat(set)
                .hasSize(5)
                .containsExactlyInAnyOrder(BNZ, PNZ, WESTPAC, ASB, ANZ);
    }

    @Test
    void getMetaAsFluxWithRequestId() throws BlinkServiceException {
        when(metaApiClient.getMeta(anyString()))
                .thenReturn(Flux.just(BNZ, PNZ, WESTPAC, ASB, ANZ));

        Flux<BankMetadata> actual = client.getMetaAsFlux(UUID.randomUUID().toString());

        assertThat(actual).isNotNull();
        Set<BankMetadata> set = new HashSet<>();
        StepVerifier
                .create(actual)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .verifyComplete();
        assertThat(set)
                .hasSize(5)
                .containsExactlyInAnyOrder(BNZ, PNZ, WESTPAC, ASB, ANZ);
    }

    @Test
    void createSingleConsentWithRedirectFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createSingleConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createSingleConsentWithRedirectFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createSingleConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createSingleConsentWithRedirectFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createSingleConsentWithRedirectFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createSingleConsentWithDecoupledFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createSingleConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithDecoupledFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createSingleConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithDecoupledFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithDecoupledFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithGatewayFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse actual = client.createSingleConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithGatewayFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

        SingleConsentRequest request = new SingleConsentRequest()
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

        CreateConsentResponse actual = client.createSingleConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithGatewayFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class)))
                .thenReturn(Mono.just(response));

        SingleConsentRequest request = new SingleConsentRequest()
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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createSingleConsentWithGatewayFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(singleConsentsApiClient.createSingleConsent(any(SingleConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        SingleConsentRequest request = new SingleConsentRequest()
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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createSingleConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void getSingleConsent() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        Consent consent = new Consent()
                .consentId(consentId)
                .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                .creationTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")).minusHours(1))
                .detail((OneOfconsentDetail) new SingleConsentRequest()
                        .flow(new AuthFlow()
                                .detail((OneOfauthFlowDetail) new RedirectFlow()
                                        .bank(Bank.PNZ)
                                        .redirectUri(REDIRECT_URI)
                                        .redirectToApp(true)
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
                .cardNetwork(CardNetwork.VISA);
        when(singleConsentsApiClient.getSingleConsent(consentId))
                .thenReturn(Mono.just(consent));

        Consent actual = client.getSingleConsent(consentId);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNull();
        assertThat(actual.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
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
    void getSingleConsentWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        Consent consent = new Consent()
                .consentId(consentId)
                .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                .creationTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")).minusHours(1))
                .detail((OneOfconsentDetail) new SingleConsentRequest()
                        .flow(new AuthFlow()
                                .detail((OneOfauthFlowDetail) new RedirectFlow()
                                        .bank(Bank.PNZ)
                                        .redirectUri(REDIRECT_URI)
                                        .redirectToApp(true)
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
                .cardNetwork(CardNetwork.VISA);
        when(singleConsentsApiClient.getSingleConsent(consentId, requestId))
                .thenReturn(Mono.just(consent));

        Consent actual = client.getSingleConsent(consentId, requestId);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNull();
        assertThat(actual.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
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
    void getSingleConsentAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        Consent consent = new Consent()
                .consentId(consentId)
                .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                .creationTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")).minusHours(1))
                .detail((OneOfconsentDetail) new SingleConsentRequest()
                        .flow(new AuthFlow()
                                .detail((OneOfauthFlowDetail) new RedirectFlow()
                                        .bank(Bank.PNZ)
                                        .redirectUri(REDIRECT_URI)
                                        .redirectToApp(true)
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
                .cardNetwork(CardNetwork.VISA);
        when(singleConsentsApiClient.getSingleConsent(consentId))
                .thenReturn(Mono.just(consent));

        Mono<Consent> consentMono = client.getSingleConsentAsMono(consentId);

        Consent actual = consentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNull();
        assertThat(actual.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
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
    void getSingleConsentAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        Consent consent = new Consent()
                .consentId(consentId)
                .status(Consent.StatusEnum.AWAITINGAUTHORISATION)
                .creationTimestamp(OffsetDateTime.now(ZoneId.of("Pacific/Auckland")).minusHours(1))
                .detail((OneOfconsentDetail) new SingleConsentRequest()
                        .flow(new AuthFlow()
                                .detail((OneOfauthFlowDetail) new RedirectFlow()
                                        .bank(Bank.PNZ)
                                        .redirectUri(REDIRECT_URI)
                                        .redirectToApp(true)
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
                .cardNetwork(CardNetwork.VISA);
        when(singleConsentsApiClient.getSingleConsent(consentId, requestId))
                .thenReturn(Mono.just(consent));

        Mono<Consent> consentMono = client.getSingleConsentAsMono(consentId, requestId);

        Consent actual = consentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNull();
        assertThat(actual.getDetail())
                .isNotNull()
                .isInstanceOf(SingleConsentRequest.class);
        SingleConsentRequest detail = (SingleConsentRequest) actual.getDetail();
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
    void revokeSingleConsent() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        when(singleConsentsApiClient.revokeSingleConsent(consentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeSingleConsent(consentId));
    }

    @Test
    void revokeSingleConsentWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        when(singleConsentsApiClient.revokeSingleConsent(consentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeSingleConsent(consentId, requestId));
    }

    @Test
    void revokeSingleConsentAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        when(singleConsentsApiClient.revokeSingleConsent(consentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeSingleConsentAsMono(consentId).block());
    }

    @Test
    void revokeSingleConsentAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        when(singleConsentsApiClient.revokeSingleConsent(consentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeSingleConsentAsMono(consentId, requestId).block());
    }

    @Test
    void createEnduringConsentWithRedirectFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createEnduringConsentWithRedirectFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class),
                anyString()))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createEnduringConsentWithRedirectFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createEnduringConsentWithRedirectFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId)
                .redirectUri(REDIRECT_URI);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class),
                anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, REDIRECT_URI);
    }

    @Test
    void createEnduringConsentWithDecoupledFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithDecoupledFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class),
                anyString()))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithDecoupledFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithDecoupledFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class),
                anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithGatewayFlow() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithGatewayFlowAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateConsentResponse actual = client.createEnduringConsent(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithGatewayFlowAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void createEnduringConsentWithGatewayFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        CreateConsentResponse response = new CreateConsentResponse()
                .consentId(consentId);
        when(enduringConsentsApiClient.createEnduringConsent(any(EnduringConsentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateConsentResponse> createConsentResponseMono = client.createEnduringConsentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse actual = createConsentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateConsentResponse::getConsentId, CreateConsentResponse::getRedirectUri)
                .containsExactly(consentId, null);
    }

    @Test
    void getEnduringConsent() throws BlinkServiceException {
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
                .payments(Collections.emptyList())
                .cardNetwork(CardNetwork.VISA);
        when(enduringConsentsApiClient.getEnduringConsent(consentId))
                .thenReturn(Mono.just(consent));

        Consent actual = client.getEnduringConsent(consentId);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
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
    void getEnduringConsentWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
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
                .payments(Collections.emptyList())
                .cardNetwork(CardNetwork.VISA);
        when(enduringConsentsApiClient.getEnduringConsent(consentId, requestId))
                .thenReturn(Mono.just(consent));

        Consent actual = client.getEnduringConsent(consentId, requestId);

        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
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
    void getEnduringConsentAsMono() throws BlinkServiceException {
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
                .payments(Collections.emptyList())
                .cardNetwork(CardNetwork.VISA);
        when(enduringConsentsApiClient.getEnduringConsent(consentId))
                .thenReturn(Mono.just(consent));

        Mono<Consent> consentMono = client.getEnduringConsentAsMono(consentId);

        Consent actual = consentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
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
    void getEnduringConsentAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
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
                .payments(Collections.emptyList())
                .cardNetwork(CardNetwork.VISA);
        when(enduringConsentsApiClient.getEnduringConsent(consentId, requestId))
                .thenReturn(Mono.just(consent));

        Mono<Consent> consentMono = client.getEnduringConsentAsMono(consentId, requestId);

        Consent actual = consentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Consent::getStatus, Consent::getPayments, Consent::getCardNetwork)
                .containsExactly(Consent.StatusEnum.AWAITINGAUTHORISATION, Collections.emptyList(), CardNetwork.VISA);
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
    void revokeEnduringConsent() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        when(enduringConsentsApiClient.revokeEnduringConsent(consentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeEnduringConsent(consentId));
    }

    @Test
    void revokeEnduringConsentWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        when(enduringConsentsApiClient.revokeEnduringConsent(consentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeEnduringConsent(consentId, requestId));
    }

    @Test
    void revokeEnduringConsentAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        when(enduringConsentsApiClient.revokeEnduringConsent(consentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeEnduringConsentAsMono(consentId).block());
    }

    @Test
    void revokeEnduringConsentAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        when(enduringConsentsApiClient.revokeEnduringConsent(consentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeEnduringConsentAsMono(consentId, requestId).block());
    }

    @Test
    void createQuickPaymentWithRedirectFlow() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithRedirectFlowAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateQuickPaymentResponse actual = client.createQuickPayment(request,
                UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithRedirectFlowAsMono() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPaymentAsMono(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithRedirectFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        Mono<CreateQuickPaymentResponse> createQuickPaymentResponseMono =
                client.createQuickPaymentAsMono(request, UUID.randomUUID().toString());

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithDecoupledFlow() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateQuickPaymentResponse actual = client.createQuickPayment(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }

    @Test
    void createQuickPaymentWithDecoupledFlowAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateQuickPaymentResponse actual = client.createQuickPayment(request,
                UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }

    @Test
    void createQuickPaymentWithDecoupledFlowAsMono() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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
                client.createQuickPaymentAsMono(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }

    @Test
    void createQuickPaymentWithDecoupledFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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
                client.createQuickPaymentAsMono(request, UUID.randomUUID().toString());

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, null);
    }

    @Test
    void createQuickPaymentWithGatewayFlow() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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

        CreateQuickPaymentResponse actual = client.createQuickPayment(request);

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithGatewayFlowAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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

        CreateQuickPaymentResponse actual = client.createQuickPayment(request,
                UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithGatewayFlowAsMono() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class)))
                .thenReturn(Mono.just(response));

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
                client.createQuickPaymentAsMono(request);

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void createQuickPaymentWithGatewayFlowAsMonoAndRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        CreateQuickPaymentResponse response = new CreateQuickPaymentResponse()
                .quickPaymentId(quickPaymentId)
                .redirectUri(REDIRECT_URI);
        when(quickPaymentsApiClient.createQuickPayment(any(QuickPaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

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
                client.createQuickPaymentAsMono(request, UUID.randomUUID().toString());

        assertThat(createQuickPaymentResponseMono).isNotNull();
        CreateQuickPaymentResponse actual = createQuickPaymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(CreateQuickPaymentResponse::getQuickPaymentId, CreateQuickPaymentResponse::getRedirectUri)
                .containsExactly(quickPaymentId, REDIRECT_URI);
    }

    @Test
    void getQuickPayment() throws BlinkServiceException {
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
                                                .redirectToApp(true)
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
        when(quickPaymentsApiClient.getQuickPayment(quickPaymentId))
                .thenReturn(Mono.just(response));

        QuickPaymentResponse actual = client.getQuickPayment(quickPaymentId);

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
    void getQuickPaymentWithRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

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
                                                .redirectToApp(true)
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
        when(quickPaymentsApiClient.getQuickPayment(quickPaymentId, requestId))
                .thenReturn(Mono.just(response));

        QuickPaymentResponse actual = client.getQuickPayment(quickPaymentId, requestId);

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
    void getQuickPaymentAsMono() throws BlinkServiceException {
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
                                                .redirectToApp(true)
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
        when(quickPaymentsApiClient.getQuickPayment(quickPaymentId))
                .thenReturn(Mono.just(response));

        Mono<QuickPaymentResponse> quickPaymentResponseMono = client.getQuickPaymentAsMono(quickPaymentId);

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
    void getQuickPaymentAsMonoWithRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();

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
                                                .redirectToApp(true)
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
        when(quickPaymentsApiClient.getQuickPayment(quickPaymentId, requestId))
                .thenReturn(Mono.just(response));

        Mono<QuickPaymentResponse> quickPaymentResponseMono = client.getQuickPaymentAsMono(quickPaymentId, requestId);

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
    void revokeQuickPayment() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        when(quickPaymentsApiClient.revokeQuickPayment(quickPaymentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId));
    }

    @Test
    void revokeQuickPaymentWithRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();
        when(quickPaymentsApiClient.revokeQuickPayment(quickPaymentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeQuickPayment(quickPaymentId, requestId));
    }

    @Test
    void revokeQuickPaymentAsMono() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        when(quickPaymentsApiClient.revokeQuickPayment(quickPaymentId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeQuickPaymentAsMono(quickPaymentId).block());
    }

    @Test
    void revokeQuickPaymentAsMonoWithRequestId() throws BlinkServiceException {
        UUID quickPaymentId = UUID.randomUUID();
        String requestId = UUID.randomUUID().toString();
        when(quickPaymentsApiClient.revokeQuickPayment(quickPaymentId, requestId))
                .thenReturn(Mono.empty());

        assertThatNoException().isThrownBy(() -> client.revokeQuickPaymentAsMono(quickPaymentId, requestId).block());
    }

    @Test
    void createSinglePayment() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID());

        PaymentResponse actual = client.createPayment(request);

        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createSinglePaymentWithRequestId() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID());

        PaymentResponse actual = client.createPayment(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createSinglePaymentAsMono() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID());

        Mono<PaymentResponse> paymentResponseMono = client.createPaymentAsMono(request);

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createSinglePaymentAsMonoWithRequestId() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID());

        Mono<PaymentResponse> paymentResponseMono = client.createPaymentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createEnduringPayment() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.75"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        PaymentResponse actual = client.createPayment(request);

        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createEnduringPaymentWithRequestId() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.75"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        PaymentResponse actual = client.createPayment(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createEnduringPaymentAsMono() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.75"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        Mono<PaymentResponse> paymentResponseMono = client.createPaymentAsMono(request);

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void createEnduringPaymentAsMonoWithRequestId() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);
        when(paymentsApiClient.createPayment(any(PaymentRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.75"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        Mono<PaymentResponse> paymentResponseMono = client.createPaymentAsMono(request,
                UUID.randomUUID().toString());

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    void getPayment() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Payment payment = new Payment()
                .paymentId(paymentId)
                .type(Payment.TypeEnum.SINGLE)
                .status(Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .refunds(Collections.emptyList())
                .detail(new PaymentRequest()
                        .consentId(consentId));
        when(paymentsApiClient.getPayment(paymentId))
                .thenReturn(Mono.just(payment));

        Payment actual = client.getPayment(paymentId);

        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        PaymentRequest paymentRequest = actual.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, null);
    }

    @Test
    void getPaymentWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Payment payment = new Payment()
                .paymentId(paymentId)
                .type(Payment.TypeEnum.SINGLE)
                .status(Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .refunds(Collections.emptyList())
                .detail(new PaymentRequest()
                        .consentId(consentId));
        when(paymentsApiClient.getPayment(paymentId, requestId))
                .thenReturn(Mono.just(payment));

        Payment actual = client.getPayment(paymentId, requestId);

        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        PaymentRequest paymentRequest = actual.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, null);
    }

    @Test
    void getPaymentAsMono() throws BlinkServiceException {
        UUID consentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Payment payment = new Payment()
                .paymentId(paymentId)
                .type(Payment.TypeEnum.SINGLE)
                .status(Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .refunds(Collections.emptyList())
                .detail(new PaymentRequest()
                        .consentId(consentId));
        when(paymentsApiClient.getPayment(paymentId))
                .thenReturn(Mono.just(payment));

        Mono<Payment> paymentMono = client.getPaymentAsMono(paymentId);

        assertThat(paymentMono).isNotNull();
        Payment actual = paymentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        PaymentRequest paymentRequest = actual.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, null);
    }

    @Test
    void getPaymentAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID consentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Payment payment = new Payment()
                .paymentId(paymentId)
                .type(Payment.TypeEnum.SINGLE)
                .status(Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .refunds(Collections.emptyList())
                .detail(new PaymentRequest()
                        .consentId(consentId));
        when(paymentsApiClient.getPayment(paymentId, requestId))
                .thenReturn(Mono.just(payment));

        Mono<Payment> paymentMono = client.getPaymentAsMono(paymentId, requestId);

        assertThat(paymentMono).isNotNull();
        Payment actual = paymentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        PaymentRequest paymentRequest = actual.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, null);
    }

    @Test
    void createFullRefund() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(FullRefundRequest.class)))
                .thenReturn(Mono.just(response));
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request);

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createFullRefundWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(FullRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createFullRefundAsMono() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(FullRefundRequest.class)))
                .thenReturn(Mono.just(response));
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createFullRefundAsMonoWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(FullRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request, UUID.randomUUID().toString());

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createPartialRefund() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(PartialRefundRequest.class)))
                .thenReturn(Mono.just(response));

        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request);

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createPartialRefundWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(PartialRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createPartialRefundAsMono() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(PartialRefundRequest.class)))
                .thenReturn(Mono.just(response));

        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createPartialRefundAsMonoWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(PartialRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request, UUID.randomUUID().toString());

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createAccountNumberRefund() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(AccountNumberRefundRequest.class)))
                .thenReturn(Mono.just(response));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request);

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createAccountNumberRefundWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(AccountNumberRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        RefundResponse actual = client.createRefund(request, UUID.randomUUID().toString());

        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createAccountNumberRefundAsMono() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(AccountNumberRefundRequest.class)))
                .thenReturn(Mono.just(response));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void createAccountNumberRefundAsMonoWithRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);
        when(refundsApiClient.createRefund(any(AccountNumberRefundRequest.class), anyString()))
                .thenReturn(Mono.just(response));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefundAsMono(request,
                UUID.randomUUID().toString());

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    void getRefund() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Refund refund = new Refund()
                .refundId(refundId)
                .accountNumber("99-6121-6242460-00")
                .status(Refund.StatusEnum.COMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .detail((RefundRequest) new AccountNumberRefundRequest()
                        .paymentId(paymentId)
                        .type(RefundDetail.TypeEnum.ACCOUNT_NUMBER));
        when(refundsApiClient.getRefund(refundId))
                .thenReturn(Mono.just(refund));

        Refund actual = client.getRefund(refundId);

        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus, Refund::getAccountNumber)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED, "99-6121-6242460-00");
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    void getRefundWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Refund refund = new Refund()
                .refundId(refundId)
                .accountNumber("99-6121-6242460-00")
                .status(Refund.StatusEnum.COMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .detail((RefundRequest) new AccountNumberRefundRequest()
                        .paymentId(paymentId)
                        .type(RefundDetail.TypeEnum.ACCOUNT_NUMBER));
        when(refundsApiClient.getRefund(refundId, requestId))
                .thenReturn(Mono.just(refund));

        Refund actual = client.getRefund(refundId, requestId);

        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus, Refund::getAccountNumber)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED, "99-6121-6242460-00");
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    void getRefundAsMono() throws BlinkServiceException {
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Refund refund = new Refund()
                .refundId(refundId)
                .accountNumber("99-6121-6242460-00")
                .status(Refund.StatusEnum.COMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .detail((RefundRequest) new AccountNumberRefundRequest()
                        .paymentId(paymentId)
                        .type(RefundDetail.TypeEnum.ACCOUNT_NUMBER));
        when(refundsApiClient.getRefund(refundId))
                .thenReturn(Mono.just(refund));

        Mono<Refund> refundMono = client.getRefundAsMono(refundId);

        assertThat(refundMono).isNotNull();
        Refund actual = refundMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus, Refund::getAccountNumber)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED, "99-6121-6242460-00");
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    void getRefundAsMonoWithRequestId() throws BlinkServiceException {
        String requestId = UUID.randomUUID().toString();
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Pacific/Auckland"));
        Refund refund = new Refund()
                .refundId(refundId)
                .accountNumber("99-6121-6242460-00")
                .status(Refund.StatusEnum.COMPLETED)
                .creationTimestamp(now)
                .statusUpdatedTimestamp(now.plusMinutes(5))
                .detail((RefundRequest) new AccountNumberRefundRequest()
                        .paymentId(paymentId)
                        .type(RefundDetail.TypeEnum.ACCOUNT_NUMBER));
        when(refundsApiClient.getRefund(refundId, requestId))
                .thenReturn(Mono.just(refund));

        Mono<Refund> refundMono = client.getRefundAsMono(refundId, requestId);

        assertThat(refundMono).isNotNull();
        Refund actual = refundMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus, Refund::getAccountNumber)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED, "99-6121-6242460-00");
        assertThat(actual.getCreationTimestamp()).isEqualTo(now);
        assertThat(actual.getStatusUpdatedTimestamp()).isEqualTo(now.plusMinutes(5));
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    void construct() throws IOException {
        Path propertiesFile = Paths.get("src", "test", "resources", "blinkdebit.properties");

        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(propertiesFile));

        assertThatNoException().isThrownBy(() -> new BlinkDebitClient(properties));
    }
}
