package nz.co.blink.debit.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.client.v1.SingleConsentsApiClient;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The API keeps {@code identifier_value} but sends {@code null} once the bank has the request, so the SDK must read a
 * null there. Guards against a mapper or DTO change (for example {@code @JsonSetter(nulls = Nulls.FAIL)}) that would
 * turn those responses into errors.
 */
@ExtendWith(MockitoExtension.class)
class IdentifierValueTest {

    private static final UUID CONSENT_ID = UUID.fromString("a5d7c994-1a4a-4061-9987-c07d8ab34e28");

    private static final String CONSENT_FIELDS = "\"consent_id\":\"" + CONSENT_ID + "\","
            + "\"creation_timestamp\":\"2026-10-27T04:10:00.000+13:00\","
            + "\"status_updated_timestamp\":\"2026-10-27T04:10:01.000+13:00\","
            + "\"payments\":[],";

    private static final String SINGLE_DETAIL_FIELDS = "\"pcr\":{\"particulars\":\"particulars\"},"
            + "\"amount\":{\"total\":\"0.01\",\"currency\":\"NZD\"}";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private ReactorClientHttpConnector connector;

    @Mock
    private AccessTokenHandler accessTokenHandler;

    private SingleConsentsApiClient singleConsentsApi;

    private QuickPaymentsApiClient quickPaymentsApi;

    @BeforeEach
    void setUp() {
        JakartaValidationServiceImpl validationService = new JakartaValidationServiceImpl(validator);
        singleConsentsApi = new SingleConsentsApiClient(connector, new BlinkPayProperties(), accessTokenHandler,
                validationService);
        quickPaymentsApi = new QuickPaymentsApiClient(connector, new BlinkPayProperties(), accessTokenHandler,
                validationService);
    }

    @Test
    void getSingleConsentReadsNullIdentifierValueOnDecoupledFlow() throws Exception {
        stubResponse(singleConsentsApi, "{" + CONSENT_FIELDS + "\"status\":\"AwaitingAuthorisation\","
                + "\"detail\":{\"type\":\"single\",\"flow\":{\"detail\":{\"type\":\"decoupled\",\"bank\":\"PNZ\","
                + "\"identifier_type\":\"phone_number\",\"identifier_value\":null,"
                + "\"callback_url\":\"https://www.blinkpay.co.nz/callback\"}}," + SINGLE_DETAIL_FIELDS + "}}");

        Consent consent = singleConsentsApi.getSingleConsent(CONSENT_ID).block();

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.AWAITINGAUTHORISATION);
        assertThat(flowDetail(consent)).isInstanceOf(DecoupledFlow.class);
        DecoupledFlow flow = (DecoupledFlow) flowDetail(consent);
        assertThat(flow.getIdentifierValue()).isNull();
        assertThat(flow.getIdentifierType()).isEqualTo(IdentifierType.PHONE_NUMBER);
        assertThat(flow.getBank()).isEqualTo(Bank.PNZ);
    }

    @Test
    void getQuickPaymentReadsNullIdentifierValueOnGatewayDecoupledFlowHint() throws Exception {
        stubResponse(quickPaymentsApi, "{\"quick_payment_id\":\"" + CONSENT_ID + "\",\"consent\":{" + CONSENT_FIELDS
                + "\"status\":\"Revoked\",\"detail\":{\"type\":\"single\",\"flow\":{\"detail\":{\"type\":\"gateway\","
                + "\"redirect_uri\":\"https://www.blinkpay.co.nz/sample-merchant-return-page\","
                + "\"flow_hint\":{\"type\":\"decoupled\",\"bank\":\"PNZ\",\"identifier_type\":\"phone_number\","
                + "\"identifier_value\":null}}}," + SINGLE_DETAIL_FIELDS + "}}}");

        Consent consent = quickPaymentsApi.getQuickPayment(CONSENT_ID).block().getConsent();

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.REVOKED);
        GatewayFlow flow = (GatewayFlow) flowDetail(consent);
        assertThat(flow.getFlowHint()).isInstanceOf(DecoupledFlowHint.class);
        DecoupledFlowHint flowHint = (DecoupledFlowHint) flow.getFlowHint();
        assertThat(flowHint.getIdentifierValue()).isNull();
        assertThat(flowHint.getIdentifierType()).isEqualTo(IdentifierType.PHONE_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(classes = {DecoupledFlow.class, DecoupledFlowHint.class})
    void identifierValueIsDeclaredRequiredAndNullable(Class<?> dto) throws Exception {
        Method getter = dto.getMethod("getIdentifierValue");
        Schema schema = getter.getAnnotation(Schema.class);

        assertThat(schema.nullable()).isTrue();
        assertThat(schema.requiredMode()).isEqualTo(Schema.RequiredMode.REQUIRED);
        // Request validation reads this; a create request still needs a value.
        assertThat(getter.isAnnotationPresent(NotNull.class)).isTrue();
    }

    private static OneOfauthFlowDetail flowDetail(Consent consent) {
        return ((SingleConsentRequest) consent.getDetail()).getFlow().getDetail();
    }

    private void stubResponse(Object client, String body) {
        when(accessTokenHandler.setAccessToken(any())).thenReturn(ExchangeFilterFunction.ofRequestProcessor(Mono::just));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()));
        ReflectionTestUtils.setField(client, "webClientBuilder", webClientBuilder);
    }
}
