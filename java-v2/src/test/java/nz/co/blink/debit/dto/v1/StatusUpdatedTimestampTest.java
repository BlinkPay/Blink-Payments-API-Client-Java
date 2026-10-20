package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.helpers.HttpClientHelper;
import nz.co.blink.debit.service.AccessTokenManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The API omits {@code status_updated_timestamp} until a status first changes, so the SDK must accept its absence
 * and must not write it back as an explicit {@code null}.
 */
@ExtendWith(MockitoExtension.class)
class StatusUpdatedTimestampTest {

    private static final UUID QUICK_PAYMENT_ID = UUID.fromString("0b9c4bd2-3c5e-4bd3-8a77-1c2f3d4e5f60");

    private static final String TIMESTAMP_KEY = "status_updated_timestamp";

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-10-19T10:05:00+13:00");

    private static final String CONSENT_FIELDS = "\"consent_id\":\"" + QUICK_PAYMENT_ID + "\","
            + "\"status\":\"GatewayAwaitingSubmission\","
            + "\"creation_timestamp\":\"2026-10-19T10:00:00.000+13:00\","
            + "\"detail\":{\"type\":\"single\","
            + "\"flow\":{\"detail\":{\"type\":\"gateway\",\"redirect_uri\":\"https://www.blinkpay.co.nz/sample-merchant-return-page\"}},"
            + "\"pcr\":{\"particulars\":\"particulars\"},"
            + "\"amount\":{\"total\":\"1.00\",\"currency\":\"NZD\"}},"
            + "\"payments\":[]";

    private static final List<Class<?>> DTOS = List.of(Consent.class, Payment.class, Refund.class);

    @Mock
    private HttpClient httpClient;

    @Mock
    private AccessTokenManager tokenManager;

    @Mock
    private HttpResponse<String> httpResponse;

    private BlinkDebitClient blinkDebitClient;

    private ObjectMapper objectMapper;

    private QuickPaymentsApiClient quickPaymentsApi;

    @BeforeEach
    void setUp() throws Exception {
        BlinkDebitConfig config = BlinkDebitConfig.builder()
                .debitUrl("https://test.blinkpay.co.nz")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();
        blinkDebitClient = new BlinkDebitClient(config);
        objectMapper = sdkObjectMapper(blinkDebitClient);
        quickPaymentsApi = new QuickPaymentsApiClient(
                new HttpClientHelper(httpClient, config, objectMapper, tokenManager));
    }

    @AfterEach
    void tearDown() {
        blinkDebitClient.close();
    }

    @Test
    void getQuickPaymentAwaitingGatewaySubmissionHasNoStatusUpdatedTimestamp() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + QUICK_PAYMENT_ID + "\",\"consent\":{" + CONSENT_FIELDS + "}}");

        Consent consent = quickPaymentsApi.getQuickPayment(QUICK_PAYMENT_ID).getConsent();

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.GATEWAY_AWAITING_SUBMISSION);
        assertThat(consent.getStatusUpdatedTimestamp()).isNull();
        assertThat(objectMapper.valueToTree(consent).has(TIMESTAMP_KEY)).isFalse();
    }

    @Test
    void getQuickPaymentReadsStatusUpdatedTimestampWhenPresent() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + QUICK_PAYMENT_ID + "\",\"consent\":{" + CONSENT_FIELDS
                + ",\"" + TIMESTAMP_KEY + "\":\"2026-10-19T10:05:00.000+13:00\"}}");

        Consent consent = quickPaymentsApi.getQuickPayment(QUICK_PAYMENT_ID).getConsent();

        assertThat(consent.getStatusUpdatedTimestamp()).isAtSameInstantAs(TIMESTAMP);
        assertThat(objectMapper.valueToTree(consent).has(TIMESTAMP_KEY)).isTrue();
    }

    @Test
    void nullStatusUpdatedTimestampIsOmittedWhenSerialised() {
        assertThat(objectMapper.valueToTree(new Consent().consentId(QUICK_PAYMENT_ID)).has(TIMESTAMP_KEY)).isFalse();
        assertThat(objectMapper.valueToTree(new Payment().paymentId(QUICK_PAYMENT_ID)).has(TIMESTAMP_KEY)).isFalse();
        assertThat(objectMapper.valueToTree(new Refund().refundId(QUICK_PAYMENT_ID)).has(TIMESTAMP_KEY)).isFalse();
    }

    @Test
    void statusUpdatedTimestampIsSerialisedWhenSet() {
        assertThat(objectMapper.valueToTree(new Consent().statusUpdatedTimestamp(TIMESTAMP)).has(TIMESTAMP_KEY)).isTrue();
        assertThat(objectMapper.valueToTree(new Payment().statusUpdatedTimestamp(TIMESTAMP)).has(TIMESTAMP_KEY)).isTrue();
        assertThat(objectMapper.valueToTree(new Refund().statusUpdatedTimestamp(TIMESTAMP)).has(TIMESTAMP_KEY)).isTrue();
    }

    @Test
    void statusUpdatedTimestampIsNotDeclaredRequired() throws Exception {
        for (Class<?> dto : DTOS) {
            Method getter = dto.getMethod("getStatusUpdatedTimestamp");
            Method setter = dto.getMethod("setStatusUpdatedTimestamp", OffsetDateTime.class);

            assertThat(getter.isAnnotationPresent(NotNull.class)).as(dto.getSimpleName()).isFalse();
            for (Method accessor : List.of(getter, setter)) {
                assertThat(accessor.getAnnotation(JsonProperty.class).required()).as(accessor.toString()).isFalse();
                assertThat(accessor.getAnnotation(JsonInclude.class).value()).as(accessor.toString())
                        .isEqualTo(JsonInclude.Include.NON_NULL);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(String body) throws Exception {
        when(tokenManager.getAccessToken()).thenReturn("test-access-token");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
    }

    /**
     * The mapper {@link BlinkDebitClient} builds for itself, so the assertions run against its real configuration.
     */
    private static ObjectMapper sdkObjectMapper(BlinkDebitClient client) throws ReflectiveOperationException {
        Field field = BlinkDebitClient.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        return (ObjectMapper) field.get(client);
    }
}
