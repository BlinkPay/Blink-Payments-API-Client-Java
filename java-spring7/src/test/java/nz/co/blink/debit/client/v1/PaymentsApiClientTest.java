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
import nz.co.blink.debit.dto.v1.Payment;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.Pcr;
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

import static nz.co.blink.debit.enums.BlinkDebitConstant.PAYMENTS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link PaymentsApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentsApiClientTest {

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
    private PaymentsApiClient client;

    @Test
    @DisplayName("Verify that null request is handled")
    void createPaymentWithNullRequest() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment request must not be null");
    }

    @Test
    @DisplayName("Verify that null consent ID is handled")
    void createPaymentWithNullConsentId() {
        PaymentRequest request = new PaymentRequest();

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Consent ID must not be null");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createEnduringPaymentWithNullPcr() {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .pcr(new Pcr())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"));

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageContaining("Particulars must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createEnduringPaymentWithBlankParticulars(String particulars) {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"));

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception).isNotNull();
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createEnduringPaymentWithLongPcrValues() {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"));

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageContaining("Particulars has a maximum length of 12 characters and must have at least 1 non-whitespace character");
    }

    @Test
    @DisplayName("Verify that null amount is handled")
    void createEnduringPaymentWithNullAmount() {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageContaining("Total must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createEnduringPaymentWithNullCurrency() {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .total("25.50"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageContaining("Currency must not be null and only NZD is supported");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createEnduringPaymentWithInvalidTotal(String total) {
        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createPayment(request).block());

        assertThat(exception).isNotNull();
    }

    @Test
    @DisplayName("Verify that null refund ID is handled")
    void getPaymentWithNullPaymentId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.getPayment(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that payment is retrieved")
    @SuppressWarnings("unchecked")
    void getPayment() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

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

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(payment));

        Mono<Payment> paymentMono = client.getPayment(paymentId);

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
    @DisplayName("Verify that single payment is created")
    @SuppressWarnings("unchecked")
    void createSinglePayment() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(PaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("123.50"));

        Mono<PaymentResponse> paymentResponseMono = client.createPayment(request);

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }

    @Test
    @DisplayName("Verify that enduring payment is created")
    @SuppressWarnings("unchecked")
    void createEnduringPayment() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse()
                .paymentId(paymentId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(PAYMENTS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(PaymentRequest.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        PaymentRequest request = new PaymentRequest()
                .consentId(UUID.randomUUID())
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.75"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"));

        Mono<PaymentResponse> paymentResponseMono = client.createPayment(request);

        assertThat(paymentResponseMono).isNotNull();
        PaymentResponse actual = paymentResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(PaymentResponse::getPaymentId)
                .isEqualTo(paymentId);
    }
}
