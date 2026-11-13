package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.client.v1.EnduringConsentsApiClient;
import nz.co.blink.debit.client.v1.PaymentsApiClient;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.client.v1.RefundsApiClient;
import nz.co.blink.debit.client.v1.SingleConsentsApiClient;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.helpers.HttpClientHelper;
import nz.co.blink.debit.service.AccessTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Surcharge fields are read from every response that carries them, and never written into a request, even when an
 * {@link Amount} read from a response is reused. Runs through the API clients with the client's own mapper.
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

    @Mock
    private HttpClient httpClient;

    @Mock
    private AccessTokenManager tokenManager;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = BlinkDebitClient.newObjectMapper();

    private SingleConsentsApiClient singleConsentsApi;

    private EnduringConsentsApiClient enduringConsentsApi;

    private QuickPaymentsApiClient quickPaymentsApi;

    private PaymentsApiClient paymentsApi;

    private RefundsApiClient refundsApi;

    @BeforeEach
    void setUp() throws Exception {
        BlinkDebitConfig config = BlinkDebitConfig.builder()
                .debitUrl("https://test.blinkpay.co.nz")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();
        HttpClientHelper httpHelper = new HttpClientHelper(httpClient, config, objectMapper, tokenManager);
        singleConsentsApi = new SingleConsentsApiClient(httpHelper);
        enduringConsentsApi = new EnduringConsentsApiClient(httpHelper);
        quickPaymentsApi = new QuickPaymentsApiClient(httpHelper);
        paymentsApi = new PaymentsApiClient(httpHelper);
        refundsApi = new RefundsApiClient(httpHelper);
    }

    @Test
    void redirectSingleConsentReadsSurchargeAndTotalCharge() throws Exception {
        stubResponse(200, REDIRECT_SINGLE_SURCHARGED);

        Consent consent = singleConsentsApi.getConsent(ID);

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getCurrency()).isEqualTo(Amount.CurrencyEnum.NZD);
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void gatewaySingleConsentReadsBankAndCardSurchargeOptions() throws Exception {
        stubResponse(200, GATEWAY_SINGLE_OPTIONS);

        Consent consent = singleConsentsApi.getConsent(ID);

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
        stubResponse(200, GATEWAY_QUICK_OPTIONS);

        SurchargeOptions options = quickPaymentsApi.getQuickPayment(ID).getConsent().getSurchargeOptions();

        assertThat(options.getBank().getSurcharge()).isEqualTo("0.68");
        assertThat(options.getBank().getTotalCharge()).isEqualTo("100.68");
        assertThat(options.getCard()).isNull();
    }

    @Test
    void enduringConsentReadsRaisedMaximaAndTheirOptions() throws Exception {
        stubResponse(200, GATEWAY_ENDURING_BOTH_MAXIMA);

        Consent consent = enduringConsentsApi.getConsent(ID);

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
        stubResponse(200, GATEWAY_ENDURING_PERIOD_ONLY);

        Consent consent = enduringConsentsApi.getConsent(ID);

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
        stubResponse(200, PAYMENT_SURCHARGED);

        Amount amount = paymentsApi.getPayment(ID).getDetail().getAmount();

        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
    }

    @Test
    void unsurchargedConsentHasNoSurchargeFields() throws Exception {
        stubResponse(200, single("AwaitingAuthorisation", REDIRECT, PLAIN_AMOUNT, ""));

        Consent consent = singleConsentsApi.getConsent(ID);

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isNull();
        assertThat(amount.getTotalCharge()).isNull();
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void explicitNullSurchargeFieldsReadAsNull() throws Exception {
        stubResponse(200, single("AwaitingAuthorisation", REDIRECT,
                "{\"currency\":\"NZD\",\"total\":\"100.00\",\"surcharge\":null,\"total_charge\":null}",
                ",\"surcharge_options\":null"));

        Consent consent = singleConsentsApi.getConsent(ID);

        Amount amount = ((SingleConsentRequest) consent.getDetail()).getAmount();
        assertThat(amount.getTotal()).isEqualTo("100.00");
        assertThat(amount.getSurcharge()).isNull();
        assertThat(amount.getTotalCharge()).isNull();
        assertThat(consent.getSurchargeOptions()).isNull();
    }

    @Test
    void unknownResponseFieldsAreIgnored() throws Exception {
        stubResponse(200, single("GatewayAwaitingSubmission", GATEWAY, PLAIN_AMOUNT,
                ",\"surcharge_options\":{"
                        + "\"bank\":{\"surcharge\":\"0.68\",\"total_charge\":\"100.68\",\"rate_description\":\"0.68 per payment\"},"
                        + "\"wallet\":{\"surcharge\":\"0.50\",\"total_charge\":\"100.50\"}},"
                        + "\"some_future_field\":{\"nested\":true}"));

        Consent consent = singleConsentsApi.getConsent(ID);

        assertThat(consent.getSurchargeOptions().getBank().getSurcharge()).isEqualTo("0.68");
        assertThat(consent.getSurchargeOptions().getBank().getTotalCharge()).isEqualTo("100.68");
        assertThat(consent.getSurchargeOptions().getCard()).isNull();
    }

    @Test
    void unknownFieldInsideASurchargeDetailIsIgnored() throws Exception {
        stubResponse(200, enduring(MAXIMUM_AMOUNT_PERIOD,
                ",\"surcharge_options\":{\"bank\":{\"maximum_amount_period\":"
                        + "{\"surcharge\":\"2.72\",\"total_charge\":\"402.72\",\"rate\":\"0.68%\"}}}"));

        SurchargeDetail period = enduringConsentsApi.getConsent(ID).getSurchargeOptions().getBank().getMaximumAmountPeriod();

        assertThat(period.getSurcharge()).isEqualTo("2.72");
        assertThat(period.getTotalCharge()).isEqualTo("402.72");
    }

    @Test
    void singleConsentRequestNeverSendsSurchargeFields() throws Exception {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow().detail(new RedirectFlow().bank(Bank.PNZ).redirectUri(URI.create(REDIRECT_URI))))
                .pcr(new Pcr().particulars("particulars"))
                .amount(surchargedAmountFromAResponse());
        ArgumentCaptor<HttpRequest> sent = stubResponse(201, "{\"consent_id\":\"" + ID + "\"}");

        singleConsentsApi.createSingleConsent(request);

        assertAmountSentWithoutSurcharge(bodyOf(sent.getValue()).get("amount"), "100.00");
    }

    @Test
    void quickPaymentRequestNeverSendsSurchargeFields() throws Exception {
        QuickPaymentRequest request = new QuickPaymentRequest()
                .flow(new AuthFlow().detail(new GatewayFlow().redirectUri(URI.create(REDIRECT_URI))))
                .pcr(new Pcr().particulars("particulars"))
                .amount(surchargedAmountFromAResponse());
        ArgumentCaptor<HttpRequest> sent = stubResponse(201,
                "{\"quick_payment_id\":\"" + ID + "\",\"redirect_uri\":\"" + REDIRECT_URI + "\"}");

        quickPaymentsApi.createQuickPayment(request);

        assertAmountSentWithoutSurcharge(bodyOf(sent.getValue()).get("amount"), "100.00");
    }

    @Test
    void enduringConsentRequestNeverSendsSurchargeFieldsOnEitherMaximum() throws Exception {
        EnduringConsentRequest maxima = (EnduringConsentRequest) objectMapper
                .readValue(GATEWAY_ENDURING_BOTH_MAXIMA, Consent.class).getDetail();
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow().detail(new GatewayFlow().redirectUri(URI.create(REDIRECT_URI))))
                .fromTimestamp(OffsetDateTime.parse("2026-11-10T09:00:00+13:00"))
                .period(Period.MONTHLY)
                .maximumAmountPeriod(maxima.getMaximumAmountPeriod())
                .maximumAmountPayment(maxima.getMaximumAmountPayment());
        assertThat(request.getMaximumAmountPeriod().getSurcharge()).isEqualTo("2.72");
        ArgumentCaptor<HttpRequest> sent = stubResponse(201, "{\"consent_id\":\"" + ID + "\"}");

        enduringConsentsApi.createEnduringConsent(request);

        JsonNode body = bodyOf(sent.getValue());
        assertAmountSentWithoutSurcharge(body.get("maximum_amount_period"), "400.00");
        assertAmountSentWithoutSurcharge(body.get("maximum_amount_payment"), "100.00");
    }

    @Test
    void paymentRequestNeverSendsSurchargeFields() throws Exception {
        PaymentRequest request = new PaymentRequest().consentId(ID).amount(surchargedAmountFromAResponse());
        ArgumentCaptor<HttpRequest> sent = stubResponse(201, "{\"payment_id\":\"" + ID + "\"}");

        paymentsApi.createPayment(request);

        assertAmountSentWithoutSurcharge(bodyOf(sent.getValue()).get("amount"), "100.00");
    }

    @Test
    void partialRefundRequestNeverSendsSurchargeFields() throws Exception {
        PartialRefundRequest request = new PartialRefundRequest()
                .amount(surchargedAmountFromAResponse())
                .pcr(new Pcr().particulars("particulars"));
        request.setPaymentId(ID);
        ArgumentCaptor<HttpRequest> sent = stubResponse(201, "{\"refund_id\":\"" + ID + "\"}");

        refundsApi.createRefund(request);

        assertAmountSentWithoutSurcharge(bodyOf(sent.getValue()).get("amount"), "100.00");
    }

    private Amount surchargedAmountFromAResponse() throws Exception {
        Amount amount = ((SingleConsentRequest) objectMapper.readValue(REDIRECT_SINGLE_SURCHARGED, Consent.class)
                .getDetail()).getAmount();
        assertThat(amount.getSurcharge()).isEqualTo("0.68");
        assertThat(amount.getTotalCharge()).isEqualTo("100.68");
        return amount;
    }

    private static void assertAmountSentWithoutSurcharge(JsonNode amount, String total) {
        assertThat(amount.get("total").asText()).isEqualTo(total);
        assertThat(amount.get("currency").asText()).isEqualTo("NZD");
        for (String key : READ_ONLY_KEYS) {
            assertThat(amount.has(key)).as(key).isFalse();
        }
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<HttpRequest> stubResponse(int status, String body) throws Exception {
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        when(tokenManager.getAccessToken()).thenReturn("test-access-token");
        when(httpResponse.statusCode()).thenReturn(status);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(request.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        return request;
    }

    private JsonNode bodyOf(HttpRequest request) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch done = new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                out.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                done.countDown();
            }

            @Override
            public void onComplete() {
                done.countDown();
            }
        });
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        String json = out.toString(StandardCharsets.UTF_8);
        for (String key : READ_ONLY_KEYS) {
            assertThat(json).doesNotContain("\"" + key + "\"");
        }
        return objectMapper.readTree(json);
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
