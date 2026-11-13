package nz.co.blink.debit.dto.v1;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import nz.co.blink.debit.client.v1.EnduringConsentsApiClient;
import nz.co.blink.debit.client.v1.PaymentsApiClient;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.client.v1.RefundsApiClient;
import nz.co.blink.debit.client.v1.SingleConsentsApiClient;
import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Surcharge fields are read from every response that carries them, and never written into a request, even when an
 * {@link Amount} read from a response is reused. Runs through the API clients and WebClient's default codecs.
 */
@ExtendWith(MockitoExtension.class)
class SurchargeFieldsTest {

    private static final UUID ID = UUID.fromString("11111111-1111-4111-8111-000000000001");

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    private static final String TIMESTAMPS = "\"creation_timestamp\":\"2026-11-10T09:00:00+13:00\","
            + "\"status_updated_timestamp\":\"2026-11-10T09:00:01+13:00\",\"payments\":[]";

    private static final String PCR = "\"pcr\":{\"particulars\":\"particulars\",\"code\":\"code\",\"reference\":\"reference\"}";

    private static final String REDIRECT = "{\"detail\":{\"type\":\"redirect\",\"bank\":\"PNZ\",\"redirect_uri\":\"" + REDIRECT_URI + "\"}}";

    private static final String GATEWAY = "{\"detail\":{\"type\":\"gateway\",\"redirect_uri\":\"" + REDIRECT_URI + "\"}}";

    private static final String SURCHARGED_AMOUNT =
            "{\"currency\":\"NZD\",\"total\":\"100.00\",\"surcharge\":\"0.68\",\"total_charge\":\"100.68\"}";

    private static final String PLAIN_AMOUNT = "{\"currency\":\"NZD\",\"total\":\"100.00\"}";

    private static final String REDIRECT_SINGLE_SURCHARGED = single("AwaitingAuthorisation", REDIRECT, SURCHARGED_AMOUNT, "");

    private static final String GATEWAY_SINGLE_OPTIONS = single("GatewayAwaitingSubmission", GATEWAY, PLAIN_AMOUNT,
            ",\"surcharge_options\":{\"bank\":{\"surcharge\":\"0.68\",\"total_charge\":\"100.68\"},"
                    + "\"card\":{\"surcharge\":\"1.49\",\"total_charge\":\"101.49\"}}");

    private static final String GATEWAY_QUICK_OPTIONS = "{\"quick_payment_id\":\"" + ID + "\",\"consent\":"
            + single("GatewayAwaitingSubmission", GATEWAY, PLAIN_AMOUNT,
            ",\"surcharge_options\":{\"bank\":{\"surcharge\":\"0.68\",\"total_charge\":\"100.68\"}}") + "}";

    private static final String MAXIMUM_AMOUNT_PERIOD = "\"maximum_amount_period\":"
            + "{\"currency\":\"NZD\",\"total\":\"400.00\",\"surcharge\":\"2.72\",\"total_charge\":\"402.72\"}";

    private static final String GATEWAY_ENDURING_BOTH_MAXIMA = enduring(MAXIMUM_AMOUNT_PERIOD
                    + ",\"maximum_amount_payment\":" + SURCHARGED_AMOUNT,
            ",\"surcharge_options\":{\"bank\":{\"maximum_amount_payment\":{\"surcharge\":\"0.68\",\"total_charge\":\"100.68\"},"
                    + "\"maximum_amount_period\":{\"surcharge\":\"2.72\",\"total_charge\":\"402.72\"}}}");

    private static final String GATEWAY_ENDURING_PERIOD_ONLY = enduring(MAXIMUM_AMOUNT_PERIOD,
            ",\"surcharge_options\":{\"bank\":{\"maximum_amount_period\":{\"surcharge\":\"2.72\",\"total_charge\":\"402.72\"}}}");

    private static final String PAYMENT_SURCHARGED = "{\"payment_id\":\"" + ID + "\",\"type\":\"single\","
            + "\"status\":\"AcceptedSettlementCompleted\",\"accepted_reason\":\"source_bank_payment_sent\","
            + "\"creation_timestamp\":\"2026-11-10T09:00:00+13:00\",\"status_updated_timestamp\":\"2026-11-10T09:00:01+13:00\","
            + "\"detail\":{\"consent_id\":\"" + ID + "\"," + PCR + ",\"amount\":" + SURCHARGED_AMOUNT + "},\"refunds\":[]}";

    private static final List<String> READ_ONLY_KEYS = List.of("surcharge", "total_charge", "surcharge_options");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private ReactorClientHttpConnector connector;

    @Mock
    private AccessTokenHandler accessTokenHandler;

    private SingleConsentsApiClient singleConsentsApi;

    private EnduringConsentsApiClient enduringConsentsApi;

    private QuickPaymentsApiClient quickPaymentsApi;

    private PaymentsApiClient paymentsApi;

    private RefundsApiClient refundsApi;

    @BeforeEach
    void setUp() {
        BlinkPayProperties properties = new BlinkPayProperties();
        JakartaValidationServiceImpl validationService = new JakartaValidationServiceImpl(validator);
        singleConsentsApi = new SingleConsentsApiClient(connector, properties, accessTokenHandler, validationService);
        enduringConsentsApi = new EnduringConsentsApiClient(connector, properties, accessTokenHandler, validationService);
        quickPaymentsApi = new QuickPaymentsApiClient(connector, properties, accessTokenHandler, validationService);
        paymentsApi = new PaymentsApiClient(connector, properties, accessTokenHandler, validationService);
        refundsApi = new RefundsApiClient(connector, properties, accessTokenHandler, validationService);
        when(accessTokenHandler.setAccessToken(any())).thenReturn(ExchangeFilterFunction.ofRequestProcessor(Mono::just));
    }

    @Test
    void redirectSingleConsentReadsSurchargeAndTotalCharge() throws Exception {
        respondWith(singleConsentsApi, REDIRECT_SINGLE_SURCHARGED);

        Consent consent = singleConsentsApi.getSingleConsent(ID).block();

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getCurrency()).isEqualTo(Amount.CurrencyEnum.NZD);
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void gatewaySingleConsentReadsBankAndCardSurchargeOptions() throws Exception {
        respondWith(singleConsentsApi, GATEWAY_SINGLE_OPTIONS);

        Consent consent = singleConsentsApi.getSingleConsent(ID).block();

        SurchargeOptions options = consent.getSurchargeOptions();
        assertThat(options.getBank().getSurcharge()).isEqualTo("0.68");
        assertThat(options.getBank().getTotalCharge()).isEqualTo("100.68");
        assertThat(options.getCard().getSurcharge()).isEqualTo("1.49");
        assertThat(options.getCard().getTotalCharge()).isEqualTo("101.49");
        assertThat(options.getBank().getMaximumAmountPayment()).isNull();
        assertThat(options.getBank().getMaximumAmountPeriod()).isNull();
        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getSurcharge()).isNull();
        assertThat(amount.getTotalCharge()).isNull();
    }

    @Test
    void quickPaymentReadsSurchargeOptionsOnItsConsent() throws Exception {
        respondWith(quickPaymentsApi, GATEWAY_QUICK_OPTIONS);

        SurchargeOptions options = quickPaymentsApi.getQuickPayment(ID).block().getConsent().getSurchargeOptions();

        assertThat(options.getBank().getSurcharge()).isEqualTo("0.68");
        assertThat(options.getBank().getTotalCharge()).isEqualTo("100.68");
        assertThat(options.getCard()).isNull();
    }

    @Test
    void enduringConsentReadsRaisedMaximaAndTheirOptions() throws Exception {
        respondWith(enduringConsentsApi, GATEWAY_ENDURING_BOTH_MAXIMA);

        Consent consent = enduringConsentsApi.getEnduringConsent(ID).block();

        EnduringConsentRequest detail = (EnduringConsentRequest) consent.getDetail();
        assertThat(detail.getMaximumAmountPeriod().getTotal()).isEqualTo("400.00");
        assertThat(detail.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        assertThat(detail.getMaximumAmountPeriod().getTotalCharge()).isEqualTo("402.72");
        assertThat(detail.getMaximumAmountPayment().getTotal()).isEqualTo("100.00");
        assertThat(detail.getMaximumAmountPayment().getSurcharge()).isEqualTo("0.68");
        assertThat(detail.getMaximumAmountPayment().getTotalCharge()).isEqualTo("100.68");
        SurchargeOption bank = consent.getSurchargeOptions().getBank();
        assertThat(bank.getMaximumAmountPayment().getSurcharge()).isEqualTo("0.68");
        assertThat(bank.getMaximumAmountPayment().getTotalCharge()).isEqualTo("100.68");
        assertThat(bank.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        assertThat(bank.getMaximumAmountPeriod().getTotalCharge()).isEqualTo("402.72");
        assertThat(bank.getSurcharge()).isNull();
        assertThat(bank.getTotalCharge()).isNull();
        assertThat(consent.getSurchargeOptions().getCard()).isNull();
    }

    @Test
    void enduringConsentWithoutAPaymentMaximumReadsThePeriodOnly() throws Exception {
        respondWith(enduringConsentsApi, GATEWAY_ENDURING_PERIOD_ONLY);

        Consent consent = enduringConsentsApi.getEnduringConsent(ID).block();

        EnduringConsentRequest detail = (EnduringConsentRequest) consent.getDetail();
        assertThat(detail.getMaximumAmountPayment()).isNull();
        assertThat(detail.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        assertThat(detail.getMaximumAmountPeriod().getTotalCharge()).isEqualTo("402.72");
        SurchargeOption bank = consent.getSurchargeOptions().getBank();
        assertThat(bank.getMaximumAmountPayment()).isNull();
        assertThat(bank.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        assertThat(bank.getMaximumAmountPeriod().getTotalCharge()).isEqualTo("402.72");
    }

    @Test
    void paymentReadsSurchargeAndTotalCharge() throws Exception {
        respondWith(paymentsApi, PAYMENT_SURCHARGED);

        Amount amount = paymentsApi.getPayment(ID).block().getDetail().getAmount();

        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
    }

    @Test
    void unsurchargedConsentHasNoSurchargeFields() throws Exception {
        respondWith(singleConsentsApi, single("AwaitingAuthorisation", REDIRECT, PLAIN_AMOUNT, ""));

        Consent consent = singleConsentsApi.getSingleConsent(ID).block();

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isNull();
        assertThat(amount.getTotalCharge()).isNull();
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void explicitNullSurchargeFieldsReadAsNull() throws Exception {
        respondWith(singleConsentsApi, single("AwaitingAuthorisation", REDIRECT,
                "{\"currency\":\"NZD\",\"total\":\"100.00\",\"surcharge\":null,\"total_charge\":null}",
                ",\"surcharge_options\":null"));

        Consent consent = singleConsentsApi.getSingleConsent(ID).block();

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isNull();
        assertThat(amount.getTotalCharge()).isNull();
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void unknownResponseFieldsAreIgnored() throws Exception {
        respondWith(enduringConsentsApi, enduring(MAXIMUM_AMOUNT_PERIOD,
                ",\"surcharge_options\":{\"bank\":{\"maximum_amount_period\":"
                        + "{\"surcharge\":\"2.72\",\"total_charge\":\"402.72\",\"rate\":\"0.68%\"},"
                        + "\"rate_description\":\"0.68 per payment\"},"
                        + "\"wallet\":{\"surcharge\":\"0.50\"}},"
                        + "\"some_future_field\":{\"nested\":true}"));

        SurchargeDetail period = enduringConsentsApi.getEnduringConsent(ID).block()
                .getSurchargeOptions().getBank().getMaximumAmountPeriod();

        assertThat(period.getSurcharge()).isEqualTo("2.72");
        assertThat(period.getTotalCharge()).isEqualTo("402.72");
    }

    @Test
    void singleConsentRequestNeverSendsSurchargeFields() throws Exception {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow().detail(new RedirectFlow().bank(Bank.PNZ).redirectUri(REDIRECT_URI)))
                .pcr(new Pcr().particulars("particulars"))
                .amount(surchargedAmountFromAResponse());
        AtomicReference<String> sent = captureRequest(singleConsentsApi, "{\"consent_id\":\"" + ID + "\"}");

        singleConsentsApi.createSingleConsent(request).block();

        assertAmountSentWithoutSurcharge(bodyOf(sent).get("amount"), "100.00");
    }

    @Test
    void quickPaymentRequestNeverSendsSurchargeFields() throws Exception {
        QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
                .flow(new AuthFlow().detail(new GatewayFlow().redirectUri(REDIRECT_URI)))
                .pcr(new Pcr().particulars("particulars"))
                .amount(surchargedAmountFromAResponse());
        AtomicReference<String> sent = captureRequest(quickPaymentsApi,
                "{\"quick_payment_id\":\"" + ID + "\",\"redirect_uri\":\"" + REDIRECT_URI + "\"}");

        quickPaymentsApi.createQuickPayment(request).block();

        assertAmountSentWithoutSurcharge(bodyOf(sent).get("amount"), "100.00");
    }

    @Test
    void enduringConsentRequestNeverSendsSurchargeFieldsOnEitherMaximum() throws Exception {
        respondWith(enduringConsentsApi, GATEWAY_ENDURING_BOTH_MAXIMA);
        EnduringConsentRequest maxima = (EnduringConsentRequest) enduringConsentsApi.getEnduringConsent(ID).block()
                .getDetail();
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow().detail(new GatewayFlow().redirectUri(REDIRECT_URI)))
                .fromTimestamp(OffsetDateTime.parse("2026-11-10T09:00:00+13:00"))
                .period(Period.MONTHLY)
                .maximumAmountPeriod(maxima.getMaximumAmountPeriod())
                .maximumAmountPayment(maxima.getMaximumAmountPayment());
        assertThat(request.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        AtomicReference<String> sent = captureRequest(enduringConsentsApi, "{\"consent_id\":\"" + ID + "\"}");

        enduringConsentsApi.createEnduringConsent(request).block();

        JsonNode body = bodyOf(sent);
        assertAmountSentWithoutSurcharge(body.get("maximum_amount_period"), "400.00");
        assertAmountSentWithoutSurcharge(body.get("maximum_amount_payment"), "100.00");
    }

    @Test
    void paymentRequestNeverSendsSurchargeFields() throws Exception {
        PaymentRequest request = new PaymentRequest().consentId(ID).amount(surchargedAmountFromAResponse());
        AtomicReference<String> sent = captureRequest(paymentsApi, "{\"payment_id\":\"" + ID + "\"}");

        paymentsApi.createPayment(request).block();

        assertAmountSentWithoutSurcharge(bodyOf(sent).get("amount"), "100.00");
    }

    @Test
    void partialRefundRequestNeverSendsSurchargeFields() throws Exception {
        PartialRefundRequest request = new PartialRefundRequest()
                .amount(surchargedAmountFromAResponse())
                .pcr(new Pcr().particulars("particulars"));
        request.setPaymentId(ID);
        AtomicReference<String> sent = captureRequest(refundsApi, "{\"refund_id\":\"" + ID + "\"}");

        refundsApi.createRefund(request).block();

        assertAmountSentWithoutSurcharge(bodyOf(sent).get("amount"), "100.00");
    }

    private Amount surchargedAmountFromAResponse() throws Exception {
        respondWith(singleConsentsApi, REDIRECT_SINGLE_SURCHARGED);
        Amount amount = ((SingleConsentRequest) singleConsentsApi.getSingleConsent(ID).block().getDetail()).getAmount();
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
        return amount;
    }

    private static void assertAmountSentWithoutSurcharge(JsonNode amount, String total) {
        assertThat(amount.get("total").asString()).isEqualTo(total);
        assertThat(amount.get("currency").asString()).isEqualTo("NZD");
        for (String key : READ_ONLY_KEYS) {
            assertThat(amount.has(key)).as(key).isFalse();
        }
    }

    private static JsonNode bodyOf(AtomicReference<String> sent) {
        String json = sent.get();
        assertThat(json).isNotNull();
        for (String key : READ_ONLY_KEYS) {
            assertThat(json).doesNotContain("\"" + key + "\"");
        }
        return JsonMapper.builder().build().readTree(json);
    }

    private static void respondWith(Object client, String body) {
        useExchange(client, request -> Mono.just(jsonResponse(HttpStatus.OK, body)));
    }

    private static AtomicReference<String> captureRequest(Object client, String responseBody) {
        AtomicReference<String> sentBody = new AtomicReference<>();
        useExchange(client, request -> {
            MockClientHttpRequest captured = new MockClientHttpRequest(request.method(), request.url());
            return request.writeTo(captured, ExchangeStrategies.withDefaults())
                    .then(Mono.defer(captured::getBodyAsString))
                    .doOnNext(sentBody::set)
                    .thenReturn(jsonResponse(HttpStatus.CREATED, responseBody));
        });
        return sentBody;
    }

    private static void useExchange(Object client, ExchangeFunction exchange) {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(exchange);
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
    }

    private static ClientResponse jsonResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private static String single(String status, String flow, String amount, String extra) {
        return "{\"consent_id\":\"" + ID + "\",\"status\":\"" + status + "\"," + TIMESTAMPS
                + ",\"detail\":{\"type\":\"single\",\"flow\":" + flow + "," + PCR + ",\"amount\":" + amount + "}"
                + extra + "}";
    }

    private static String enduring(String maxima, String extra) {
        return "{\"consent_id\":\"" + ID + "\",\"status\":\"GatewayAwaitingSubmission\"," + TIMESTAMPS
                + ",\"detail\":{\"type\":\"enduring\",\"flow\":" + GATEWAY
                + ",\"from_timestamp\":\"2026-11-10T09:00:00+13:00\",\"period\":\"monthly\"," + maxima + "}"
                + extra + "}";
    }
}
