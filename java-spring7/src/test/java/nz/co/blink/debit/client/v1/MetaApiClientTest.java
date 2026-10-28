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
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.BankMetadata;
import nz.co.blink.debit.dto.v1.BankmetadataFeatures;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesCardPayment;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesDecoupledFlow;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesDecoupledFlowAvailableIdentifiers;
import nz.co.blink.debit.dto.v1.BankmetadataFeaturesEnduringConsent;
import nz.co.blink.debit.dto.v1.BankmetadataRedirectFlow;
import nz.co.blink.debit.dto.v1.CardNetwork;
import nz.co.blink.debit.dto.v1.CardPaymentType;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static nz.co.blink.debit.enums.BlinkDebitConstant.METADATA_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link MetaApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class MetaApiClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ReactorClientHttpConnector connector;

    @Spy
    private BlinkPayProperties properties = new BlinkPayProperties();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AccessTokenHandler accessTokenHandler;

    @Spy

    @InjectMocks
    private MetaApiClient client;

    @Test
    @DisplayName("Verify that bank metadata is retrieved")
    @SuppressWarnings("unchecked")
    void getMeta() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        BankMetadata bnz = new BankMetadata()
                .name(Bank.BNZ)
                .paymentLimit(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50000"))
                .features(new BankmetadataFeatures()
                        .decoupledFlow(new BankmetadataFeaturesDecoupledFlow()
                                .enabled(true)
                                .availableIdentifiers(Collections.singletonList(
                                        new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                                .type(IdentifierType.CONSENT_ID)
                                                .name("Consent ID")))
                                .requestTimeout("PT4M"))
                        .enduringConsent(new BankmetadataFeaturesEnduringConsent()
                                .enabled(true)
                                .consentIndefinite(false)))
                .redirectFlow(new BankmetadataRedirectFlow()
                        .enabled(true)
                        .requestTimeout("PT5M"));

        BankMetadata pnz = new BankMetadata()
                .name(Bank.PNZ)
                .paymentLimit(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50000"))
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

        BankMetadata westpac = new BankMetadata()
                .name(Bank.WESTPAC)
                .paymentLimit(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("10000"))
                .features(new BankmetadataFeatures())
                .redirectFlow(new BankmetadataRedirectFlow()
                        .enabled(true)
                        .requestTimeout("PT10M"));

        BankMetadata asb = new BankMetadata()
                .name(Bank.ASB)
                .paymentLimit(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("30000"))
                .features(new BankmetadataFeatures()
                        .enduringConsent(new BankmetadataFeaturesEnduringConsent()
                                .enabled(true)
                                .consentIndefinite(false)))
                .redirectFlow(new BankmetadataRedirectFlow()
                        .enabled(true)
                        .requestTimeout("PT10M"));

        BankMetadata anz = new BankMetadata()
                .name(Bank.ANZ)
                .paymentLimit(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("2000"))
                .features(new BankmetadataFeatures()
                        .decoupledFlow(new BankmetadataFeaturesDecoupledFlow()
                                .enabled(true)
                                .availableIdentifiers(Stream.of(
                                                new BankmetadataFeaturesDecoupledFlowAvailableIdentifiers()
                                                        .type(IdentifierType.MOBILE_NUMBER)
                                                        .name("Mobile Number"))
                                        .toList())
                                .requestTimeout("PT7M")))
                .redirectFlow(new BankmetadataRedirectFlow()
                        .enabled(false));

        BankMetadata card = new BankMetadata()
                .name(Bank.CARD)
                .features(new BankmetadataFeatures()
                        .cardPayment(new BankmetadataFeaturesCardPayment()
                                .enabled(true)
                                .allowedCardPaymentTypes(List.of(CardPaymentType.PANENTRY, CardPaymentType.CLICKTOPAY,
                                        CardPaymentType.GOOGLEPAY))
                                .allowedCardNetworks(List.of(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX,
                                        CardNetwork.DISCOVER, CardNetwork.DINERSCLUB, CardNetwork.JCB))));

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(METADATA_PATH.getValue())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToFlux(any(Function.class)))
                .thenReturn(Flux.just(bnz, pnz, westpac, asb, anz, card));

        Flux<BankMetadata> actual = client.getMeta();

        assertThat(actual).isNotNull();
        Set<BankMetadata> set = new HashSet<>();
        StepVerifier
                .create(actual)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .consumeNextWith(set::add)
                .verifyComplete();
        assertThat(set)
                .hasSize(6)
                .containsExactlyInAnyOrder(bnz, pnz, westpac, asb, anz, card);
    }
}
