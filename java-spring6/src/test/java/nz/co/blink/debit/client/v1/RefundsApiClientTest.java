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
import nz.co.blink.debit.dto.v1.AccountNumberRefundRequest;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.FullRefundRequest;
import nz.co.blink.debit.dto.v1.PartialRefundRequest;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundRequest;
import nz.co.blink.debit.dto.v1.RefundResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static nz.co.blink.debit.enums.BlinkDebitConstant.IDEMPOTENCY_KEY;
import static nz.co.blink.debit.enums.BlinkDebitConstant.REFUNDS_PATH;
import static nz.co.blink.debit.enums.BlinkDebitConstant.REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The test case for {@link RefundsApiClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RefundsApiClientTest {

    private static final String CALLER_KEY = "caller-supplied-not-a-uuid";

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
    private RefundsApiClient client;

    @Test
    @DisplayName("Verify that null request is handled")
    void createRefundWithNullRequest() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(null).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Refund request must not be null");
    }

    @Test
    @DisplayName("Verify that null payment ID is handled")
    void createAccountNumberRefundWithNullPaymentId() {
        AccountNumberRefundRequest request = new AccountNumberRefundRequest();

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that null payment ID is handled")
    void createFullRefundWithNullPaymentId() {
        FullRefundRequest request = new FullRefundRequest();

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createFullRefundWithNullPcr() {
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createFullRefundWithBlankParticulars(String particulars) {
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that long PCR values are handled")
    void createFullRefundWithLongPcrValues() {
        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("merchant particulars")
                        .code("merchant code")
                        .reference("merchant reference"))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not exceed 12 characters");
    }

    @Test
    @DisplayName("Verify that null payment ID is handled")
    void createPartialRefundWithNullPaymentId() {
        PartialRefundRequest request = new PartialRefundRequest();

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Payment ID must not be null");
    }

    @Test
    @DisplayName("Verify that null PCR is handled")
    void createPartialRefundWithNullPcr() {
        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("PCR must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Verify that blank particulars is handled")
    void createPartialRefundWithBlankParticulars(String particulars) {
        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars(particulars)
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Particulars must have at least 1 character");
    }

    @Test
    @DisplayName("Verify that null amount is handled")
    void createPartialRefundWithNullAmount() {
        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Amount must not be null");
    }

    @Test
    @DisplayName("Verify that null currency is handled")
    void createPartialRefundWithNullCurrency() {
        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .total("25.50"))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Currency must not be null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc.de", "/!@#$%^&*()[{}]/=',.\"<>`~;:|\\"})
    @DisplayName("Verify that invalid total is handled")
    void createPartialRefundWithInvalidTotal(String total) {
        PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total(total))
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request).block());

        assertThat(exception)
                .isNotNull()
                .hasMessageStartingWith("Validation failed for refund request");
    }

    @Test
    @DisplayName("Verify that null refund ID is handled")
    void getRefundWithNullRefundId() {
        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.getRefund(null, UUID.randomUUID().toString()).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Refund ID must not be null");
    }

    @Test
    @DisplayName("Verify that refund is retrieved")
    @SuppressWarnings("unchecked")
    void getRefund() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

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

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(refund));

        Mono<Refund> refundMono = client.getRefund(refundId);

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
    @DisplayName("Verify that account number refund is created")
    @SuppressWarnings("unchecked")
    void createAccountNumberRefund() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(REFUNDS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(RefundDetail.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefund(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    @DisplayName("Verify that full refund is created")
    @SuppressWarnings("unchecked")
    void createFullRefund() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(REFUNDS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(RefundDetail.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

        FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(UUID.randomUUID());

        Mono<RefundResponse> refundResponseMono = client.createRefund(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    @DisplayName("Verify that partial refund is created")
    @SuppressWarnings("unchecked")
    void createPartialRefund() throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        UUID refundId = UUID.randomUUID();
        RefundResponse response = new RefundResponse()
                .refundId(refundId);

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(REFUNDS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(RefundDetail.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class))).thenReturn(Mono.just(response));

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

        Mono<RefundResponse> refundResponseMono = client.createRefund(request);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(RefundResponse::getRefundId)
                .isEqualTo(refundId);
    }

    @Test
    @DisplayName("Verify that the caller-supplied idempotency key is sent unchanged")
    @SuppressWarnings("unchecked")
    void createRefundWithCallerSuppliedIdempotencyKey() throws BlinkServiceException {
        HttpHeaders httpHeaders = captureRefundRequestHeaders(
                request -> client.createRefund(request, "rid-1", CALLER_KEY));

        assertThat(httpHeaders.getFirst(IDEMPOTENCY_KEY.getValue())).isEqualTo(CALLER_KEY);
        assertThat(httpHeaders.getFirst(REQUEST_ID.getValue())).isEqualTo("rid-1");
    }

    @Test
    @DisplayName("Verify that an idempotency key in the request headers map is sent unchanged")
    @SuppressWarnings("unchecked")
    void createRefundWithIdempotencyKeyInRequestHeaders() throws BlinkServiceException {
        HttpHeaders httpHeaders = captureRefundRequestHeaders(
                request -> client.createRefund(request, Map.of(IDEMPOTENCY_KEY.getValue(), CALLER_KEY)));

        assertThat(httpHeaders.getFirst(IDEMPOTENCY_KEY.getValue())).isEqualTo(CALLER_KEY);
    }

    @Test
    @DisplayName("Verify that an idempotency key is generated when none is supplied")
    @SuppressWarnings("unchecked")
    void createRefundWithoutIdempotencyKey() throws BlinkServiceException {
        HttpHeaders httpHeaders = captureRefundRequestHeaders(request -> client.createRefund(request));

        String sent = httpHeaders.getFirst(IDEMPOTENCY_KEY.getValue());
        assertThat(sent).isNotBlank();
        assertThatCode(() -> UUID.fromString(sent)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Verify that a blank idempotency key in the request headers map is replaced by a generated one")
    @SuppressWarnings("unchecked")
    void createRefundWithBlankIdempotencyKeyInRequestHeaders() throws BlinkServiceException {
        HttpHeaders httpHeaders = captureRefundRequestHeaders(
                request -> client.createRefund(request, Map.of(IDEMPOTENCY_KEY.getValue(), " ")));

        String sent = httpHeaders.getFirst(IDEMPOTENCY_KEY.getValue());
        assertThat(sent).isNotBlank();
        assertThatCode(() -> UUID.fromString(sent)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("Verify that a blank caller-supplied idempotency key is rejected")
    void createRefundWithBlankIdempotencyKey(String idempotencyKey) {
        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        BlinkInvalidValueException exception = catchThrowableOfType(BlinkInvalidValueException.class,
                () -> client.createRefund(request, "rid-1", idempotencyKey).block());

        assertThat(exception)
                .isNotNull()
                .hasMessage("Idempotency key must not be blank");
    }

    /**
     * Runs a refund creation against the mocked WebClient chain and replays the captured
     * {@code headers(Consumer)} argument onto a real {@link HttpHeaders}, so the assertions see the
     * header values the client actually adds rather than the fact that it called {@code headers}.
     */
    @SuppressWarnings("unchecked")
    private HttpHeaders captureRefundRequestHeaders(RefundCall call) throws BlinkServiceException {
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
        ReflectionTestUtils.setField(client, "debitUrl", "http://localhost:8080");

        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.filter(any(ExchangeFilterFunction.class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(REFUNDS_PATH.getValue())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(RefundDetail.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.just(new RefundResponse().refundId(UUID.randomUUID())));

        AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(UUID.randomUUID());

        assertThat(call.apply(request).block()).isNotNull();

        ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestBodySpec).headers(headersCaptor.capture());

        HttpHeaders httpHeaders = new HttpHeaders();
        headersCaptor.getValue().accept(httpHeaders);
        return httpHeaders;
    }

    @FunctionalInterface
    private interface RefundCall {
        Mono<RefundResponse> apply(RefundDetail request) throws BlinkServiceException;
    }
}
