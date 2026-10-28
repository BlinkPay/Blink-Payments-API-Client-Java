package nz.co.blink.debit.helpers;

import jakarta.validation.Validation;
import nz.co.blink.debit.client.v1.EnduringConsentsApiClient;
import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.RedirectFlow;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Runs the custom {@link OffsetDateTime} serializer and deserializer through WebClient's default codecs, which on
 * Spring Framework 7 are Jackson 3.
 */
@ExtendWith(MockitoExtension.class)
class CustomOffsetDateTimeCodecTest {

    private static final UUID CONSENT_ID = UUID.fromString("0b9c4bd2-3c5e-4bd3-8a77-1c2f3d4e5f60");

    private static final String REDIRECT_URI = "https://www.blinkpay.co.nz/sample-merchant-return-page";

    @Mock
    private ReactorClientHttpConnector connector;

    @Mock
    private AccessTokenHandler accessTokenHandler;

    private EnduringConsentsApiClient client;

    @BeforeEach
    void setUp() {
        client = new EnduringConsentsApiClient(connector, new BlinkPayProperties(), accessTokenHandler,
                new JakartaValidationServiceImpl(Validation.buildDefaultValidatorFactory().getValidator()));
        when(accessTokenHandler.setAccessToken(any())).thenReturn(ExchangeFilterFunction.ofRequestProcessor(Mono::just));
    }

    @Test
    void createEnduringConsentWritesTimestampsWithTheirOwnOffset() throws Exception {
        AtomicReference<String> sentBody = new AtomicReference<>();
        useExchange(request -> {
            MockClientHttpRequest captured = new MockClientHttpRequest(request.method(), request.url());
            return request.writeTo(captured, ExchangeStrategies.withDefaults())
                    .then(Mono.defer(captured::getBodyAsString))
                    .doOnNext(sentBody::set)
                    .thenReturn(jsonResponse(HttpStatus.CREATED, "{\"consent_id\":\"" + CONSENT_ID + "\"}"));
        });
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow().detail(new RedirectFlow().bank(Bank.PNZ).redirectUri(REDIRECT_URI)))
                .maximumAmountPeriod(new Amount().currency(Amount.CurrencyEnum.NZD).total("50.00"))
                .maximumAmountPayment(new Amount().currency(Amount.CurrencyEnum.NZD).total("50.00"))
                .period(Period.MONTHLY)
                .fromTimestamp(OffsetDateTime.of(2026, 10, 28, 19, 0, 0, 123_000_000, ZoneOffset.ofHours(13)))
                .expiryTimestamp(OffsetDateTime.of(2027, 10, 28, 6, 30, 0, 0, ZoneOffset.UTC));

        client.createEnduringConsent(request).block();

        assertThat(sentBody.get())
                .contains("\"from_timestamp\":\"2026-10-28T19:00:00.123+13:00\"")
                .contains("\"expiry_timestamp\":\"2027-10-28T06:30:00Z\"");
    }

    @Test
    void getEnduringConsentReadsDotSeparatedOffsets() throws Exception {
        useExchange(request -> Mono.just(jsonResponse(HttpStatus.OK, "{\"consent_id\":\"" + CONSENT_ID + "\","
                + "\"status\":\"Authorised\","
                + "\"creation_timestamp\":\"2026-10-28T19:00:00.123+13.00\","
                + "\"detail\":{\"type\":\"enduring\","
                + "\"flow\":{\"detail\":{\"type\":\"gateway\",\"redirect_uri\":\"" + REDIRECT_URI + "\"}},"
                + "\"from_timestamp\":\"2026-10-28T19:00:00+13.00\","
                + "\"period\":\"monthly\","
                + "\"maximum_amount_period\":{\"total\":\"50.00\",\"currency\":\"NZD\"}},"
                + "\"payments\":[]}")));

        Consent consent = client.getEnduringConsent(CONSENT_ID).block();

        assertThat(consent.getCreationTimestamp())
                .isEqualTo(OffsetDateTime.of(2026, 10, 28, 19, 0, 0, 123_000_000, ZoneOffset.ofHours(13)));
        assertThat(((EnduringConsentRequest) consent.getDetail()).getFromTimestamp())
                .isEqualTo(OffsetDateTime.of(2026, 10, 28, 19, 0, 0, 0, ZoneOffset.ofHours(13)));
    }

    private void useExchange(ExchangeFunction exchange) {
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
}
