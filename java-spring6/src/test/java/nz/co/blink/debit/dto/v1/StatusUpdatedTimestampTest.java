package nz.co.blink.debit.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The API omits {@code status_updated_timestamp} until a status first changes, so the SDK must accept its absence.
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

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private ReactorClientHttpConnector connector;

    @Mock
    private AccessTokenHandler accessTokenHandler;

    private QuickPaymentsApiClient client;

    @BeforeEach
    void setUp() {
        client = new QuickPaymentsApiClient(connector, new BlinkPayProperties(), accessTokenHandler,
                new JakartaValidationServiceImpl(validator));
    }

    @Test
    void getQuickPaymentAwaitingGatewaySubmissionHasNoStatusUpdatedTimestamp() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + QUICK_PAYMENT_ID + "\",\"consent\":{" + CONSENT_FIELDS + "}}");

        Consent consent = client.getQuickPayment(QUICK_PAYMENT_ID).block().getConsent();

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.GATEWAYAWAITINGSUBMISSION);
        assertThat(consent.getStatusUpdatedTimestamp()).isNull();
        assertThat(violationsOn(consent, "statusUpdatedTimestamp")).isEmpty();
    }

    @Test
    void getQuickPaymentReadsStatusUpdatedTimestampWhenPresent() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + QUICK_PAYMENT_ID + "\",\"consent\":{" + CONSENT_FIELDS
                + ",\"" + TIMESTAMP_KEY + "\":\"2026-10-19T10:05:00.000+13:00\"}}");

        Consent consent = client.getQuickPayment(QUICK_PAYMENT_ID).block().getConsent();

        assertThat(consent.getStatusUpdatedTimestamp()).isAtSameInstantAs(TIMESTAMP);
    }

    @Test
    void validatorStillRejectsOtherMissingRequiredFields() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + QUICK_PAYMENT_ID + "\",\"consent\":{" + CONSENT_FIELDS + "}}");
        Consent consent = client.getQuickPayment(QUICK_PAYMENT_ID).block().getConsent();

        consent.setStatus(null);

        assertThat(violationsOn(consent, "status")).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {Consent.class, Payment.class, Refund.class})
    void statusUpdatedTimestampIsNotDeclaredRequired(Class<?> dto) throws Exception {
        Method getter = dto.getMethod("getStatusUpdatedTimestamp");

        assertThat(getter.isAnnotationPresent(NotNull.class)).isFalse();
        assertThat(getter.getAnnotation(Schema.class).requiredMode()).isNotEqualTo(Schema.RequiredMode.REQUIRED);
    }

    private void stubResponse(String body) {
        when(accessTokenHandler.setAccessToken(any())).thenReturn(ExchangeFilterFunction.ofRequestProcessor(Mono::just));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()));
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
    }

    private Set<ConstraintViolation<Consent>> violationsOn(Consent consent, String property) {
        return validator.validateProperty(consent, property);
    }
}
