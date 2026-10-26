package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.client.v1.QuickPaymentsApiClient;
import nz.co.blink.debit.client.v1.SingleConsentsApiClient;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.helpers.HttpClientHelper;
import nz.co.blink.debit.service.AccessTokenManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    @Mock
    private HttpClient httpClient;

    @Mock
    private AccessTokenManager tokenManager;

    @Mock
    private HttpResponse<String> httpResponse;

    private BlinkDebitClient blinkDebitClient;

    private SingleConsentsApiClient singleConsentsApi;

    private QuickPaymentsApiClient quickPaymentsApi;

    @BeforeEach
    void setUp() throws Exception {
        BlinkDebitConfig config = BlinkDebitConfig.builder()
                .debitUrl("https://test.blinkpay.co.nz")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();
        blinkDebitClient = new BlinkDebitClient(config);
        HttpClientHelper httpHelper = new HttpClientHelper(httpClient, config, sdkObjectMapper(blinkDebitClient),
                tokenManager);
        singleConsentsApi = new SingleConsentsApiClient(httpHelper);
        quickPaymentsApi = new QuickPaymentsApiClient(httpHelper);
    }

    @AfterEach
    void tearDown() {
        blinkDebitClient.close();
    }

    @Test
    void getSingleConsentReadsNullIdentifierValueOnDecoupledFlow() throws Exception {
        stubResponse("{" + CONSENT_FIELDS + "\"status\":\"AwaitingAuthorisation\","
                + "\"detail\":{\"type\":\"single\",\"flow\":{\"detail\":{\"type\":\"decoupled\",\"bank\":\"PNZ\","
                + "\"identifier_type\":\"phone_number\",\"identifier_value\":null,"
                + "\"callback_url\":\"https://www.blinkpay.co.nz/callback\"}}," + SINGLE_DETAIL_FIELDS + "}}");

        Consent consent = singleConsentsApi.getConsent(CONSENT_ID);

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.AWAITING_AUTHORISATION);
        assertThat(flowDetail(consent)).isInstanceOf(DecoupledFlow.class);
        DecoupledFlow flow = (DecoupledFlow) flowDetail(consent);
        assertThat(flow.getIdentifierValue()).isNull();
        assertThat(flow.getIdentifierType()).isEqualTo(IdentifierType.PHONE_NUMBER);
        assertThat(flow.getBank()).isEqualTo(Bank.PNZ);
    }

    @Test
    void getQuickPaymentReadsNullIdentifierValueOnGatewayDecoupledFlowHint() throws Exception {
        stubResponse("{\"quick_payment_id\":\"" + CONSENT_ID + "\",\"consent\":{" + CONSENT_FIELDS
                + "\"status\":\"Revoked\",\"detail\":{\"type\":\"single\",\"flow\":{\"detail\":{\"type\":\"gateway\","
                + "\"redirect_uri\":\"https://www.blinkpay.co.nz/sample-merchant-return-page\","
                + "\"flow_hint\":{\"type\":\"decoupled\",\"bank\":\"PNZ\",\"identifier_type\":\"phone_number\","
                + "\"identifier_value\":null}}}," + SINGLE_DETAIL_FIELDS + "}}}");

        Consent consent = quickPaymentsApi.getQuickPayment(CONSENT_ID).getConsent();

        assertThat(consent.getStatus()).isEqualTo(Consent.StatusEnum.REVOKED);
        GatewayFlow flow = (GatewayFlow) flowDetail(consent);
        assertThat(flow.getFlowHint()).isInstanceOf(DecoupledFlowHint.class);
        DecoupledFlowHint flowHint = (DecoupledFlowHint) flow.getFlowHint();
        assertThat(flowHint.getIdentifierValue()).isNull();
        assertThat(flowHint.getIdentifierType()).isEqualTo(IdentifierType.PHONE_NUMBER);
    }

    private static AuthFlowDetail flowDetail(Consent consent) {
        return ((SingleConsentRequest) consent.getDetail()).getFlow().getDetail();
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(String body) throws Exception {
        when(tokenManager.getAccessToken()).thenReturn("test-access-token");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
    }

    private static ObjectMapper sdkObjectMapper(BlinkDebitClient client) throws ReflectiveOperationException {
        Field field = BlinkDebitClient.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        return (ObjectMapper) field.get(client);
    }
}
