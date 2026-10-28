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

import io.netty.handler.logging.LogLevel;
import jakarta.validation.Validation;
import lombok.extern.slf4j.Slf4j;
import nz.co.blink.debit.config.BlinkPayProperties;
import nz.co.blink.debit.dto.v1.BankMetadata;
import nz.co.blink.debit.dto.v1.Consent;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.Payment;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.QuickPaymentRequest;
import nz.co.blink.debit.dto.v1.QuickPaymentResponse;
import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundResponse;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.enums.BlinkPayProperty;
import nz.co.blink.debit.exception.BlinkConsentRejectedException;
import nz.co.blink.debit.exception.BlinkConsentTimeoutException;
import nz.co.blink.debit.exception.BlinkPaymentRejectedException;
import nz.co.blink.debit.exception.BlinkPaymentTimeoutException;
import nz.co.blink.debit.exception.BlinkRetryableException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import nz.co.blink.debit.helpers.DefaultPropertyProvider;
import nz.co.blink.debit.helpers.EnvironmentVariablePropertyProvider;
import nz.co.blink.debit.helpers.FilePropertyProvider;
import nz.co.blink.debit.helpers.PropertyProvider;
import nz.co.blink.debit.helpers.SystemPropertyProvider;
import nz.co.blink.debit.service.ValidationService;
import nz.co.blink.debit.service.impl.JakartaValidationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import static nz.co.blink.debit.dto.v1.Consent.StatusEnum.AUTHORISED;
import static nz.co.blink.debit.dto.v1.Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED;

/**
 * The facade for accessing all client methods from one place.
 */
@Component
@Slf4j
public class BlinkDebitClient {

    private static final PropertyProvider PROPERTY_PROVIDER = new EnvironmentVariablePropertyProvider(new SystemPropertyProvider(new FilePropertyProvider(new DefaultPropertyProvider())));

    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    private static final String DEFAULT_MAX_IDLE_TIME = "PT20S";
    private static final String DEFAULT_MAX_LIFE_TIME = "PT60S";
    private static final String DEFAULT_PENDING_ACQUIRE_TIMEOUT = "PT10S";
    private static final String DEFAULT_EVICTION_INTERVAL = "PT60S";
    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(1);

    private final SingleConsentsApiClient singleConsentsApiClient;

    private final EnduringConsentsApiClient enduringConsentsApiClient;

    private final QuickPaymentsApiClient quickPaymentsApiClient;

    private final PaymentsApiClient paymentsApiClient;

    private final RefundsApiClient refundsApiClient;

    private final MetaApiClient metaApiClient;

    private final ValidationService validationService;


    /**
     * Default constructor for Spring-based consumer.
     *
     * @param singleConsentsApiClient   the {@link SingleConsentsApiClient}
     * @param enduringConsentsApiClient the {@link EnduringConsentsApiClient}
     * @param quickPaymentsApiClient    the {@link QuickPaymentsApiClient}
     * @param paymentsApiClient         the {@link PaymentsApiClient}
     * @param refundsApiClient          the {@link RefundsApiClient}
     * @param metaApiClient             the {@link MetaApiClient}
     * @param validationService         the {@link ValidationService}
     */
    @Autowired
    public BlinkDebitClient(SingleConsentsApiClient singleConsentsApiClient,
                            EnduringConsentsApiClient enduringConsentsApiClient,
                            QuickPaymentsApiClient quickPaymentsApiClient, PaymentsApiClient paymentsApiClient,
                            RefundsApiClient refundsApiClient, MetaApiClient metaApiClient,
                            ValidationService validationService) {
        this.singleConsentsApiClient = singleConsentsApiClient;
        this.enduringConsentsApiClient = enduringConsentsApiClient;
        this.quickPaymentsApiClient = quickPaymentsApiClient;
        this.paymentsApiClient = paymentsApiClient;
        this.refundsApiClient = refundsApiClient;
        this.metaApiClient = metaApiClient;
        this.validationService = validationService;
    }

    /**
     * Constructor for pure Java application.
     *
     * @param properties the {@link Properties} retrieved from the configuration file
     */
    public BlinkDebitClient(Properties properties) {
        int maxConnections = Integer.parseInt(PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_MAX_CONNECTIONS));
        Duration maxIdleTime = Duration.parse(PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_MAX_IDLE_TIME));
        Duration maxLifeTime = Duration.parse(PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_MAX_LIFE_TIME));
        Duration pendingAcquireTimeout = Duration.parse(PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_PENDING_ACQUIRE_TIMEOUT));
        Duration evictionInterval = Duration.parse(PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_EVICTION_INTERVAL));
        String debitUrl = PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_DEBIT_URL);
        String clientId = PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_CLIENT_ID);
        String clientSecret = PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_CLIENT_SECRET);
        String activeProfile = PROPERTY_PROVIDER.getProperty(properties, BlinkPayProperty.BLINKPAY_ACTIVE_PROFILE);

        BlinkPayProperties blinkPayProperties = new BlinkPayProperties();
        blinkPayProperties.getDebit().setUrl(debitUrl);
        blinkPayProperties.getClient().setId(clientId);
        blinkPayProperties.getClient().setSecret(clientSecret);
        blinkPayProperties.getMax().setConnections(maxConnections);
        blinkPayProperties.getMax().getIdle().setTime(maxIdleTime);
        blinkPayProperties.getMax().getLife().setTime(maxLifeTime);
        blinkPayProperties.getPending().getAcquire().setTimeout(pendingAcquireTimeout);
        blinkPayProperties.getEviction().setInterval(evictionInterval);

        ConnectionProvider provider = ConnectionProvider.builder("blinkpay-conn-provider")
                .maxConnections(maxConnections)
                .maxIdleTime(maxIdleTime)
                .maxLifeTime(maxLifeTime)
                .pendingAcquireTimeout(pendingAcquireTimeout)
                .evictInBackground(evictionInterval)
                .build();

        ReactorClientHttpConnector reactorClientHttpConnector = configureReactorClientHttpConnector(activeProfile,
                provider);

        validationService = new JakartaValidationServiceImpl(Validation.buildDefaultValidatorFactory().getValidator());


        OAuthApiClient oauthApiClient = new OAuthApiClient(reactorClientHttpConnector, blinkPayProperties);
        AccessTokenHandler accessTokenHandler = new AccessTokenHandler(oauthApiClient);
        singleConsentsApiClient = new SingleConsentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        enduringConsentsApiClient = new EnduringConsentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        quickPaymentsApiClient = new QuickPaymentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        paymentsApiClient = new PaymentsApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler,
                validationService);
        refundsApiClient = new RefundsApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler,
                validationService);
        metaApiClient = new MetaApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler);
    }

    /**
     * Constructor for pure Java application. Retry is enabled by default.
     *
     * @param debitUrl      the Blink Debit URL
     * @param clientId      the client ID
     * @param clientSecret  the client secret
     * @param activeProfile the active profile
     */
    public BlinkDebitClient(final String debitUrl, final String clientId, final String clientSecret,
                            final String activeProfile) {
        this(debitUrl, clientId, clientSecret, activeProfile, true);
    }

    /**
     * Constructor for pure Java application.
     *
     * @param debitUrl      the Blink Debit URL
     * @param clientId      the client ID
     * @param clientSecret  the client secret
     * @param activeProfile the active profile
     * @param retryEnabled  whether retry is enabled
     */
    public BlinkDebitClient(final String debitUrl, final String clientId, final String clientSecret,
                            final String activeProfile, final boolean retryEnabled) {
        int maxConnections = DEFAULT_MAX_CONNECTIONS;
        Duration maxIdleTime = Duration.parse(DEFAULT_MAX_IDLE_TIME);
        Duration maxLifeTime = Duration.parse(DEFAULT_MAX_LIFE_TIME);
        Duration pendingAcquireTimeout = Duration.parse(DEFAULT_PENDING_ACQUIRE_TIMEOUT);
        Duration evictionInterval = Duration.parse(DEFAULT_EVICTION_INTERVAL);

        BlinkPayProperties blinkPayProperties = new BlinkPayProperties();
        blinkPayProperties.getDebit().setUrl(debitUrl);
        blinkPayProperties.getClient().setId(clientId);
        blinkPayProperties.getClient().setSecret(clientSecret);
        blinkPayProperties.getMax().setConnections(maxConnections);
        blinkPayProperties.getMax().getIdle().setTime(maxIdleTime);
        blinkPayProperties.getMax().getLife().setTime(maxLifeTime);
        blinkPayProperties.getPending().getAcquire().setTimeout(pendingAcquireTimeout);
        blinkPayProperties.getEviction().setInterval(evictionInterval);

        ConnectionProvider provider = ConnectionProvider.builder("blinkpay-conn-provider")
                .maxConnections(maxConnections)
                .maxIdleTime(maxIdleTime)
                .maxLifeTime(maxLifeTime)
                .pendingAcquireTimeout(pendingAcquireTimeout)
                .evictInBackground(evictionInterval)
                .build();

        ReactorClientHttpConnector reactorClientHttpConnector = configureReactorClientHttpConnector(activeProfile,
                provider);

        validationService = new JakartaValidationServiceImpl(Validation.buildDefaultValidatorFactory().getValidator());


        OAuthApiClient oauthApiClient = new OAuthApiClient(reactorClientHttpConnector, blinkPayProperties);
        AccessTokenHandler accessTokenHandler = new AccessTokenHandler(oauthApiClient);
        singleConsentsApiClient = new SingleConsentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        enduringConsentsApiClient = new EnduringConsentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        quickPaymentsApiClient = new QuickPaymentsApiClient(reactorClientHttpConnector, blinkPayProperties,
                accessTokenHandler, validationService);
        paymentsApiClient = new PaymentsApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler,
                validationService);
        refundsApiClient = new RefundsApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler,
                validationService);
        metaApiClient = new MetaApiClient(reactorClientHttpConnector, blinkPayProperties, accessTokenHandler);
    }

    private static ReactorClientHttpConnector configureReactorClientHttpConnector(String activeProfile,
                                                                                  ConnectionProvider provider) {
        HttpClient client;
        boolean debugMode = Pattern.compile("local|dev|test").matcher(activeProfile).matches();
        if (debugMode) {
            client = HttpClient.create(provider)
                    .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        } else {
            client = HttpClient.create(provider);
        }
        client.warmup().subscribe();

        return new ReactorClientHttpConnector(client);
    }


    /**
     * Returns the {@link List} of {@link BankMetadata}.
     *
     * @return the {@link List} of {@link BankMetadata}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public List<BankMetadata> getMeta() throws BlinkServiceException {
        try {
            return getMetaAsFlux().collectList().block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link List} of {@link BankMetadata}.
     *
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link List} of {@link BankMetadata}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public List<BankMetadata> getMeta(final String requestId) throws BlinkServiceException {
        try {
            return getMetaAsFlux(requestId).collectList().block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link List} of {@link BankMetadata}.
     *
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link List} of {@link BankMetadata}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public List<BankMetadata> getMeta(Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            return getMetaAsFlux(requestHeaders).collectList().block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link BankMetadata} {@link Flux}.
     *
     * @return the {@link BankMetadata} {@link Flux}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Flux<BankMetadata> getMetaAsFlux() throws BlinkServiceException {
        return metaApiClient.getMeta();
    }

    /**
     * Returns the {@link BankMetadata} {@link Flux}.
     *
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link BankMetadata} {@link Flux}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Flux<BankMetadata> getMetaAsFlux(final String requestId) throws BlinkServiceException {
        return metaApiClient.getMeta(requestId);
    }

    /**
     * Returns the {@link BankMetadata} {@link Flux}.
     *
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link BankMetadata} {@link Flux}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Flux<BankMetadata> getMetaAsFlux(Map<String, String> requestHeaders) throws BlinkServiceException {
        return metaApiClient.getMeta(requestHeaders);
    }

    /**
     * Creates a single consent.
     *
     * @param request the {@link SingleConsentRequest}
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createSingleConsent(SingleConsentRequest request)
            throws BlinkServiceException {
        try {
            return createSingleConsentAsMono(request).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a single consent.
     *
     * @param request   the {@link SingleConsentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createSingleConsent(SingleConsentRequest request, final String requestId)
            throws BlinkServiceException {
        try {
            return createSingleConsentAsMono(request, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a single consent.
     *
     * @param request        the {@link SingleConsentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createSingleConsent(SingleConsentRequest request, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return createSingleConsentAsMono(request, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a single consent.
     *
     * @param request the {@link SingleConsentRequest}
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createSingleConsentAsMono(SingleConsentRequest request)
            throws BlinkServiceException {
        return singleConsentsApiClient.createSingleConsent(request);
    }

    /**
     * Creates a single consent with redirect flow.
     *
     * @param request   the {@link SingleConsentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createSingleConsentAsMono(SingleConsentRequest request,
                                                                 final String requestId)
            throws BlinkServiceException {
        return singleConsentsApiClient.createSingleConsent(request, requestId);
    }

    /**
     * Creates a single consent with redirect flow.
     *
     * @param request        the {@link SingleConsentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createSingleConsentAsMono(SingleConsentRequest request,
                                                                 Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return singleConsentsApiClient.createSingleConsent(request, requestHeaders);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getSingleConsent(UUID consentId) throws BlinkServiceException {
        try {
            return getSingleConsentAsMono(consentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getSingleConsent(UUID consentId, final String requestId) throws BlinkServiceException {
        try {
            return getSingleConsentAsMono(consentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getSingleConsent(UUID consentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            return getSingleConsentAsMono(consentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getSingleConsentAsMono(UUID consentId) throws BlinkServiceException {
        return singleConsentsApiClient.getSingleConsent(consentId);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getSingleConsentAsMono(UUID consentId, final String requestId)
            throws BlinkServiceException {
        return singleConsentsApiClient.getSingleConsent(consentId, requestId);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getSingleConsentAsMono(UUID consentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return singleConsentsApiClient.getSingleConsent(consentId, requestHeaders);
    }

    /**
     * Retrieves an authorised single consent by ID within the specified time.
     * Timeout and other exceptions are wrapped in a {@link RuntimeException}.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent awaitAuthorisedSingleConsent(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<Consent> consentMono = getSingleConsentAsMono(consentId);

        try {
            return consentMono
                    .flatMap(consent -> {
                        Consent.StatusEnum status = consent.getStatus();
                        log.debug("The last status polled was: {} \tfor Single Consent ID: {}", status,
                                consentId);

                        if (AUTHORISED == status) {
                            return consentMono;
                        }

                        return Mono.error(new BlinkRetryableException());
                    }).retryWhen(reactor.util.retry.Retry
                            .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                            .filter(BlinkRetryableException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                                throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                            })
                    ).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an authorised single consent by ID within the specified time.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when a Blink Debit service exception occurs
     */
    public Consent awaitAuthorisedSingleConsentOrThrowException(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        try {
            return awaitAuthorisedSingleConsentAsMono(consentId, maxWaitSeconds).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an authorised single consent by ID within the specified time.
     * The consent statuses are handled accordingly.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Mono} containing the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> awaitAuthorisedSingleConsentAsMono(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<Consent> consentMono = getSingleConsentAsMono(consentId);

        return consentMono
                .flatMap(consent -> {
                    Consent.StatusEnum status = consent.getStatus();
                    log.debug("The last status polled was: {} \tfor Single Consent ID: {}", status,
                            consentId);

                    switch (status) {
                        case AUTHORISED, CONSUMED:
                            break;
                        case REJECTED, REVOKED:
                            BlinkConsentRejectedException exception1 =
                                    new BlinkConsentRejectedException("Single consent [" + consentId
                                                                      + "] has been rejected or revoked");
                            return Mono.error(exception1);
                        case GATEWAYTIMEOUT:
                            BlinkConsentTimeoutException exception2 =
                                    new BlinkConsentTimeoutException("Gateway timed out for single consent ["
                                                                     + consentId + "]");
                            return Mono.error(exception2);
                        case GATEWAYAWAITINGSUBMISSION, AWAITINGAUTHORISATION:
                            BlinkRetryableException exception3 = new BlinkRetryableException("Single consent ["
                                                                                             + consentId + "] is waiting for authorisation");
                            return Mono.error(exception3);
                    }

                    log.debug("Single consent completed for ID: {}", consentId);
                    return consentMono;
                })
                .retryWhen(reactor.util.retry.Retry
                        .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                        .filter(BlinkRetryableException.class::isInstance)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                            throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                        }));
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeSingleConsent(UUID consentId) throws BlinkServiceException {
        try {
            revokeSingleConsentAsMono(consentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeSingleConsent(UUID consentId, final String requestId) throws BlinkServiceException {
        try {
            revokeSingleConsentAsMono(consentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeSingleConsent(UUID consentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            revokeSingleConsentAsMono(consentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeSingleConsentAsMono(UUID consentId) throws BlinkServiceException {
        return singleConsentsApiClient.revokeSingleConsent(consentId);
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeSingleConsentAsMono(UUID consentId, final String requestId)
            throws BlinkServiceException {
        return singleConsentsApiClient.revokeSingleConsent(consentId, requestId);
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeSingleConsentAsMono(UUID consentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return singleConsentsApiClient.revokeSingleConsent(consentId, requestHeaders);
    }

    /**
     * Creates an enduring consent.
     *
     * @param request the {@link EnduringConsentRequest}
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createEnduringConsent(EnduringConsentRequest request)
            throws BlinkServiceException {
        try {
            return createEnduringConsentAsMono(request).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates an enduring consent.
     *
     * @param request   the {@link EnduringConsentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createEnduringConsent(EnduringConsentRequest request, final String requestId)
            throws BlinkServiceException {
        try {
            return createEnduringConsentAsMono(request, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates an enduring consent.
     *
     * @param request        the {@link EnduringConsentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateConsentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateConsentResponse createEnduringConsent(EnduringConsentRequest request,
                                                       Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return createEnduringConsentAsMono(request, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates an enduring consent.
     *
     * @param request the {@link EnduringConsentRequest}
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createEnduringConsentAsMono(EnduringConsentRequest request)
            throws BlinkServiceException {
        return enduringConsentsApiClient.createEnduringConsent(request);
    }

    /**
     * Creates an enduring consent.
     *
     * @param request   the {@link EnduringConsentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createEnduringConsentAsMono(EnduringConsentRequest request,
                                                                   final String requestId)
            throws BlinkServiceException {
        return enduringConsentsApiClient.createEnduringConsent(request, requestId);
    }

    /**
     * Creates an enduring consent.
     *
     * @param request        the {@link EnduringConsentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateConsentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateConsentResponse> createEnduringConsentAsMono(EnduringConsentRequest request,
                                                                   Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return enduringConsentsApiClient.createEnduringConsent(request, requestHeaders);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getEnduringConsent(UUID consentId) throws BlinkServiceException {
        try {
            return getEnduringConsentAsMono(consentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getEnduringConsent(UUID consentId, final String requestId) throws BlinkServiceException {
        try {
            return getEnduringConsentAsMono(consentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent getEnduringConsent(UUID consentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            return getEnduringConsentAsMono(consentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getEnduringConsentAsMono(UUID consentId) throws BlinkServiceException {
        return enduringConsentsApiClient.getEnduringConsent(consentId);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getEnduringConsentAsMono(UUID consentId, final String requestId)
            throws BlinkServiceException {
        return enduringConsentsApiClient.getEnduringConsent(consentId, requestId);
    }

    /**
     * Retrieves an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Consent} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> getEnduringConsentAsMono(UUID consentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return enduringConsentsApiClient.getEnduringConsent(consentId, requestHeaders);
    }

    /**
     * Retrieves an authorised enduring consent by ID within the specified time.
     * Timeout and other exceptions are wrapped in a {@link RuntimeException}.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Consent awaitAuthorisedEnduringConsent(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<Consent> consentMono = getEnduringConsentAsMono(consentId);

        try {
            return consentMono
                    .flatMap(consent -> {
                        Consent.StatusEnum status = consent.getStatus();
                        log.debug("The last status polled was: {} \tfor Enduring Consent ID: {}", status,
                                consentId);

                        if (AUTHORISED == status) {
                            return consentMono;
                        }

                        return Mono.error(new BlinkRetryableException());
                    }).retryWhen(reactor.util.retry.Retry
                            .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                            .filter(BlinkRetryableException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                                try {
                                    revokeEnduringConsentAsMono(consentId).then();
                                    log.info("The max wait time was reached while waiting for the enduring consent to complete and the payment has been revoked with the server. Enduring consent ID: {}", consentId);
                                } catch (Throwable revokeException) {
                                    log.error("Waiting for the enduring consent was not successful and it was also not able to be revoked with the server due to: {}. Enduring consent ID: {}", revokeException.getLocalizedMessage(), consentId);
                                    awaitException.addSuppressed(revokeException);
                                }

                                throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                            })
                    ).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an authorised enduring consent by ID within the specified time.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Consent}
     * @throws BlinkServiceException thrown when a Blink Debit service exception occurs
     */
    public Consent awaitAuthorisedEnduringConsentOrThrowException(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        try {
            return awaitAuthorisedEnduringConsentAsMono(consentId, maxWaitSeconds).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an authorised enduring consent by ID within the specified time.
     * The consent statuses are handled accordingly.
     *
     * @param consentId      the consent ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Mono} containing the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Consent> awaitAuthorisedEnduringConsentAsMono(UUID consentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        try {
            Mono<Consent> consentMono = getEnduringConsentAsMono(consentId);

            return consentMono
                    .flatMap(consent -> {
                        Consent.StatusEnum status = consent.getStatus();
                        log.debug("The last status polled was: {} \tfor Enduring Consent ID: {}", status,
                                consentId);

                        switch (status) {
                            case AUTHORISED, CONSUMED:
                                break;
                            case REJECTED, REVOKED:
                                BlinkConsentRejectedException exception1 =
                                        new BlinkConsentRejectedException("Enduring consent [" + consentId
                                                                          + "] has been rejected or revoked");
                                return Mono.error(exception1);
                            case GATEWAYTIMEOUT:
                                BlinkConsentTimeoutException exception2 =
                                        new BlinkConsentTimeoutException("Gateway timed out for enduring consent ["
                                                                         + consentId + "]");
                                return Mono.error(exception2);
                            case GATEWAYAWAITINGSUBMISSION, AWAITINGAUTHORISATION:
                                BlinkRetryableException exception3 = new BlinkRetryableException("Enduring consent ["
                                                                                                 + consentId + "] is waiting for authorisation");
                                return Mono.error(exception3);
                        }

                        log.debug("Enduring consent completed for ID: {}", consentId);
                        return consentMono;
                    })
                    .retryWhen(reactor.util.retry.Retry
                            .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                            .filter(BlinkRetryableException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                                try {
                                    revokeEnduringConsentAsMono(consentId).then();
                                    log.info("The max wait time was reached while waiting for the enduring consent to complete and the payment has been revoked with the server. Enduring consent ID: {}", consentId);
                                } catch (Throwable revokeException) {
                                    log.error("Waiting for the enduring consent was not successful and it was also not able to be revoked with the server due to: {}. Enduring consent ID: {}", revokeException.getLocalizedMessage(), consentId);
                                    awaitException.addSuppressed(revokeException);
                                }

                                throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                            }));
        } catch (Throwable awaitException) {
            try {
                revokeEnduringConsent(consentId);
                log.info("The max wait time was reached while waiting for the enduring consent to complete and the payment has been revoked with the server. Enduring consent ID: {}", consentId);
            } catch (Throwable revokeException) {
                log.error("Waiting for the enduring consent was not successful and it was also not able to be revoked with the server due to: {}. Enduring consent ID: {}", revokeException.getLocalizedMessage(), consentId);
                awaitException.addSuppressed(revokeException);
            }
            throw awaitException;
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeEnduringConsent(UUID consentId) throws BlinkServiceException {
        try {
            revokeEnduringConsentAsMono(consentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeEnduringConsent(UUID consentId, final String requestId) throws BlinkServiceException {
        try {
            revokeEnduringConsentAsMono(consentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeEnduringConsent(UUID consentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            revokeEnduringConsentAsMono(consentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeEnduringConsentAsMono(UUID consentId) throws BlinkServiceException {
        return enduringConsentsApiClient.revokeEnduringConsent(consentId);
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId the consent ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeEnduringConsentAsMono(UUID consentId, final String requestId)
            throws BlinkServiceException {
        return enduringConsentsApiClient.revokeEnduringConsent(consentId, requestId);
    }

    /**
     * Revokes an existing consent by ID.
     *
     * @param consentId      the consent ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeEnduringConsentAsMono(UUID consentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return enduringConsentsApiClient.revokeEnduringConsent(consentId, requestHeaders);
    }

    /**
     * Creates a quick payment.
     *
     * @param request the {@link QuickPaymentRequest}
     * @return the {@link CreateQuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateQuickPaymentResponse createQuickPayment(QuickPaymentRequest request)
            throws BlinkServiceException {
        try {
            return createQuickPaymentAsMono(request).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a quick payment.
     *
     * @param request   the {@link QuickPaymentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateQuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateQuickPaymentResponse createQuickPayment(QuickPaymentRequest request, final String requestId)
            throws BlinkServiceException {
        try {
            return createQuickPaymentAsMono(request, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a quick payment.
     *
     * @param request        the {@link QuickPaymentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateQuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public CreateQuickPaymentResponse createQuickPayment(QuickPaymentRequest request,
                                                         Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return createQuickPaymentAsMono(request, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a quick payment.
     *
     * @param request the {@link QuickPaymentRequest}
     * @return the {@link CreateQuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateQuickPaymentResponse> createQuickPaymentAsMono(QuickPaymentRequest request)
            throws BlinkServiceException {
        return quickPaymentsApiClient.createQuickPayment(request);
    }

    /**
     * Creates a quick payment.
     *
     * @param request   the {@link QuickPaymentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link CreateQuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateQuickPaymentResponse> createQuickPaymentAsMono(QuickPaymentRequest request,
                                                                     final String requestId)
            throws BlinkServiceException {
        return quickPaymentsApiClient.createQuickPayment(request, requestId);
    }

    /**
     * Creates a quick payment.
     *
     * @param request        the {@link QuickPaymentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link CreateQuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<CreateQuickPaymentResponse> createQuickPaymentAsMono(QuickPaymentRequest request,
                                                                     Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return quickPaymentsApiClient.createQuickPayment(request, requestHeaders);
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @return the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public QuickPaymentResponse getQuickPayment(UUID quickPaymentId) throws BlinkServiceException {
        try {
            return getQuickPaymentAsMono(quickPaymentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public QuickPaymentResponse getQuickPayment(UUID quickPaymentId, final String requestId)
            throws BlinkServiceException {
        try {
            return getQuickPaymentAsMono(quickPaymentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public QuickPaymentResponse getQuickPayment(UUID quickPaymentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return getQuickPaymentAsMono(quickPaymentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @return the {@link QuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<QuickPaymentResponse> getQuickPaymentAsMono(UUID quickPaymentId) throws BlinkServiceException {
        return quickPaymentsApiClient.getQuickPayment(quickPaymentId);
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link QuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<QuickPaymentResponse> getQuickPaymentAsMono(UUID quickPaymentId, final String requestId)
            throws BlinkServiceException {
        return quickPaymentsApiClient.getQuickPayment(quickPaymentId, requestId);
    }

    /**
     * Retrieves an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link QuickPaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<QuickPaymentResponse> getQuickPaymentAsMono(UUID quickPaymentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return quickPaymentsApiClient.getQuickPayment(quickPaymentId, requestHeaders);
    }

    /**
     * Retrieves a successful quick payment by ID within the specified time.
     * Timeout and other exceptions are wrapped in a {@link RuntimeException}.
     *
     * @param quickPaymentId the quick payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public QuickPaymentResponse awaitSuccessfulQuickPayment(UUID quickPaymentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<QuickPaymentResponse> quickPaymentResponseMono = getQuickPaymentAsMono(quickPaymentId);

        try {
            return quickPaymentResponseMono
                    .flatMap(quickPaymentResponse -> {
                        Consent.StatusEnum status = quickPaymentResponse.getConsent().getStatus();
                        log.debug("The last status polled was: {} \tfor Quick Payment ID: {}", status, quickPaymentId);

                        if (AUTHORISED == status) {
                            return quickPaymentResponseMono;
                        }

                        return Mono.error(new BlinkRetryableException());
                    }).retryWhen(reactor.util.retry.Retry
                            .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                            .filter(BlinkRetryableException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                // Gateway timed out. Revoke so it can't be used anymore
                                BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                                try {
                                    revokeQuickPaymentAsMono(quickPaymentId).then();
                                    log.info("The max wait time was reached while waiting for the quick payment to complete and the payment has been revoked with the server. Quick payment ID: {}", quickPaymentId);
                                } catch (Throwable revokeException) {
                                    log.error("Waiting for the quick payment was not successful and it was also not able to be revoked with the server due to: {}. Quick payment ID: {}", revokeException.getLocalizedMessage(), quickPaymentId);
                                    awaitException.addSuppressed(revokeException);
                                }

                                throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                            })
                    ).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a successful quick payment by ID within the specified time.
     *
     * @param quickPaymentId the quick payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when a Blink Debit service exception occurs
     */
    public QuickPaymentResponse awaitSuccessfulQuickPaymentOrThrowException(UUID quickPaymentId,
                                                                            final int maxWaitSeconds)
            throws BlinkServiceException {
        try {
            return awaitSuccessfulQuickPaymentAsMono(quickPaymentId, maxWaitSeconds).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a successful quick payment by ID within the specified time.
     * The consent statuses are handled accordingly.
     *
     * @param quickPaymentId the quick payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Mono} containing the {@link QuickPaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<QuickPaymentResponse> awaitSuccessfulQuickPaymentAsMono(UUID quickPaymentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<QuickPaymentResponse> quickPaymentResponseMono = getQuickPaymentAsMono(quickPaymentId);

        return quickPaymentResponseMono
                .flatMap(quickPaymentResponse -> {
                    Consent.StatusEnum status = quickPaymentResponse.getConsent().getStatus();
                    log.debug("The last status polled was: {} \tfor Quick Payment ID: {}", status, quickPaymentId);

                    switch (status) {
                        case AUTHORISED, CONSUMED:
                            break;
                        case REJECTED, REVOKED:
                            BlinkConsentRejectedException exception1 =
                                    new BlinkConsentRejectedException("Quick payment [" + quickPaymentId
                                                                      + "] has been rejected or revoked");
                            return Mono.error(exception1);
                        case GATEWAYTIMEOUT:
                            BlinkConsentTimeoutException exception2 =
                                    new BlinkConsentTimeoutException("Gateway timed out for quick payment ["
                                                                     + quickPaymentId + "]");
                            return Mono.error(exception2);
                        case GATEWAYAWAITINGSUBMISSION, AWAITINGAUTHORISATION:
                            BlinkRetryableException exception3 = new BlinkRetryableException("Quick payment ["
                                                                                             + quickPaymentId + "] is waiting for authorisation");
                            return Mono.error(exception3);
                    }

                    // a successful quick payment will always have the consent status of consumed
                    log.debug("Quick Payment completed for ID: {}", quickPaymentId);
                    return quickPaymentResponseMono;
                })
                .retryWhen(reactor.util.retry.Retry
                        .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                        .filter(BlinkRetryableException.class::isInstance)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            // Gateway timed out. Revoke so it can't be used anymore
                            BlinkConsentTimeoutException awaitException = new BlinkConsentTimeoutException();
                            try {
                                revokeQuickPaymentAsMono(quickPaymentId).then();
                                log.info("The max wait time was reached while waiting for the quick payment to complete and the payment has been revoked with the server. Quick payment ID: {}", quickPaymentId);
                            } catch (Throwable revokeException) {
                                log.error("Waiting for the quick payment was not successful and it was also not able to be revoked with the server due to: {}. Quick payment ID: {}", revokeException.getLocalizedMessage(), quickPaymentId);
                                awaitException.addSuppressed(revokeException);
                            }

                            throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                        }));
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeQuickPayment(UUID quickPaymentId) throws BlinkServiceException {
        try {
            revokeQuickPaymentAsMono(quickPaymentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeQuickPayment(UUID quickPaymentId, final String requestId) throws BlinkServiceException {
        try {
            revokeQuickPaymentAsMono(quickPaymentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public void revokeQuickPayment(UUID quickPaymentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            revokeQuickPaymentAsMono(quickPaymentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeQuickPaymentAsMono(UUID quickPaymentId) throws BlinkServiceException {
        return quickPaymentsApiClient.revokeQuickPayment(quickPaymentId);
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeQuickPaymentAsMono(UUID quickPaymentId, final String requestId)
            throws BlinkServiceException {
        return quickPaymentsApiClient.revokeQuickPayment(quickPaymentId, requestId);
    }

    /**
     * Revokes an existing quick payment by ID.
     *
     * @param quickPaymentId the quick payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Void> revokeQuickPaymentAsMono(UUID quickPaymentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return quickPaymentsApiClient.revokeQuickPayment(quickPaymentId, requestHeaders);
    }

    /**
     * Creates a payment.
     *
     * @param request the {@link PaymentRequest}
     * @return the {@link PaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public PaymentResponse createPayment(PaymentRequest request) throws BlinkServiceException {
        try {
            return createPaymentAsMono(request).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a payment.
     *
     * @param request   the {@link PaymentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link PaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public PaymentResponse createPayment(PaymentRequest request, final String requestId)
            throws BlinkServiceException {
        try {
            return createPaymentAsMono(request, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a payment.
     *
     * @param request        the {@link PaymentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link PaymentResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public PaymentResponse createPayment(PaymentRequest request, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return createPaymentAsMono(request, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a payment.
     *
     * @param request the {@link PaymentRequest}
     * @return the {@link PaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<PaymentResponse> createPaymentAsMono(PaymentRequest request) throws BlinkServiceException {
        return paymentsApiClient.createPayment(request);
    }

    /**
     * Creates a payment.
     *
     * @param request   the {@link PaymentRequest}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link PaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<PaymentResponse> createPaymentAsMono(PaymentRequest request, final String requestId)
            throws BlinkServiceException {
        return paymentsApiClient.createPayment(request, requestId);
    }

    /**
     * Creates a payment.
     *
     * @param request        the {@link PaymentRequest}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link PaymentResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<PaymentResponse> createPaymentAsMono(PaymentRequest request, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return paymentsApiClient.createPayment(request, requestHeaders);
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId the payment ID
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Payment getPayment(UUID paymentId) throws BlinkServiceException {
        try {
            return getPaymentAsMono(paymentId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId the payment ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Payment getPayment(UUID paymentId, final String requestId) throws BlinkServiceException {
        try {
            return getPaymentAsMono(paymentId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId      the payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Payment getPayment(UUID paymentId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            return getPaymentAsMono(paymentId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId the payment ID
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Payment> getPaymentAsMono(UUID paymentId) throws BlinkServiceException {
        return paymentsApiClient.getPayment(paymentId);
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId the payment ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Payment> getPaymentAsMono(UUID paymentId, final String requestId) throws BlinkServiceException {
        return paymentsApiClient.getPayment(paymentId, requestId);
    }

    /**
     * Retrieves an existing payment by ID.
     *
     * @param paymentId      the payment ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Payment> getPaymentAsMono(UUID paymentId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return paymentsApiClient.getPayment(paymentId, requestHeaders);
    }

    /**
     * Retrieves a successful payment by ID within the specified time.
     * Timeout and other exceptions are wrapped in a {@link RuntimeException}.
     *
     * @param paymentId      the payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Payment awaitSuccessfulPayment(UUID paymentId, final int maxWaitSeconds) throws BlinkServiceException {
        Mono<Payment> paymentMono = getPaymentAsMono(paymentId);

        try {
            return paymentMono
                    .flatMap(payment -> {
                        Payment.StatusEnum status = payment.getStatus();
                        log.debug("The last status polled was: {} \tfor Payment ID: {}", status, paymentId);

                        if (ACCEPTEDSETTLEMENTCOMPLETED == status) {
                            return paymentMono;
                        }

                        return Mono.error(new BlinkRetryableException());
                    }).retryWhen(reactor.util.retry.Retry
                            .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                            .filter(BlinkRetryableException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                BlinkPaymentTimeoutException awaitException = new BlinkPaymentTimeoutException();
                                throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                            })
                    ).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a successful payment by ID within the specified time.
     *
     * @param paymentId      the payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when a Blink Debit service exception occurs
     */
    public Payment awaitSuccessfulPaymentOrThrowException(UUID paymentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        try {
            return awaitSuccessfulPaymentAsMono(paymentId, maxWaitSeconds).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a successful payment by ID within the specified time.
     * The payment statuses are handled accordingly.
     *
     * @param paymentId      the payment ID
     * @param maxWaitSeconds the number of seconds to wait
     * @return the {@link Mono} containing the {@link Consent}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Payment> awaitSuccessfulPaymentAsMono(UUID paymentId, final int maxWaitSeconds)
            throws BlinkServiceException {
        Mono<Payment> paymentMono = getPaymentAsMono(paymentId);

        return paymentMono
                .flatMap(payment -> {
                    Payment.StatusEnum status = payment.getStatus();
                    log.debug("The last status polled was: {} \tfor Payment ID: {}", status, paymentId);

                    switch (status) {
                        case ACCEPTEDSETTLEMENTCOMPLETED:
                            break;
                        case REJECTED:
                            BlinkPaymentRejectedException exception1 =
                                    new BlinkPaymentRejectedException("Payment [" + paymentId
                                                                      + "] has been rejected");
                            return Mono.error(exception1);
                        case ACCEPTEDSETTLEMENTINPROCESS, PENDING:
                            BlinkRetryableException exception3 = new BlinkRetryableException("Payment [" + paymentId
                                                                                             + "] is pending or being processed");
                            return Mono.error(exception3);
                    }

                    log.debug("Payment completed for ID: {}", paymentId);
                    return paymentMono;
                })
                .retryWhen(reactor.util.retry.Retry
                        .fixedDelay(maxWaitSeconds, POLLING_INTERVAL)
                        .filter(BlinkRetryableException.class::isInstance)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            BlinkPaymentTimeoutException awaitException = new BlinkPaymentTimeoutException();
                            throw Exceptions.retryExhausted(awaitException.getMessage(), awaitException);
                        }));
    }

    /**
     * Creates a refund.
     *
     * @param request the {@link RefundDetail}
     * @return the {@link RefundResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public RefundResponse createRefund(RefundDetail request) throws BlinkServiceException {
        try {
            return createRefundAsMono(request).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a refund.
     *
     * @param request   the {@link RefundDetail}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link RefundResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public RefundResponse createRefund(RefundDetail request, final String requestId)
            throws BlinkServiceException {
        try {
            return createRefundAsMono(request, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a refund, reusing a caller-supplied idempotency key so a retry does not create a
     * second refund.
     *
     * @param request        the {@link RefundDetail}
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @param idempotencyKey the optional idempotency key. A new one is generated if null; a blank one is rejected.
     * @return the {@link RefundResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public RefundResponse createRefund(RefundDetail request, final String requestId, final String idempotencyKey)
            throws BlinkServiceException {
        try {
            return createRefundAsMono(request, requestId, idempotencyKey).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a refund.
     *
     * @param request        the {@link RefundDetail}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link RefundResponse}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public RefundResponse createRefund(RefundDetail request, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        try {
            return createRefundAsMono(request, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Creates a refund.
     *
     * @param request the {@link RefundDetail}
     * @return the {@link RefundResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<RefundResponse> createRefundAsMono(RefundDetail request) throws BlinkServiceException {
        return refundsApiClient.createRefund(request);
    }

    /**
     * Creates a refund.
     *
     * @param request   the {@link RefundDetail}
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link RefundResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<RefundResponse> createRefundAsMono(RefundDetail request, final String requestId)
            throws BlinkServiceException {
        return refundsApiClient.createRefund(request, requestId);
    }

    /**
     * Creates a refund, reusing a caller-supplied idempotency key so a retry does not create a
     * second refund.
     *
     * @param request        the {@link RefundDetail}
     * @param requestId      the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @param idempotencyKey the optional idempotency key. A new one is generated if null; a blank one is rejected.
     * @return the {@link RefundResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<RefundResponse> createRefundAsMono(RefundDetail request, final String requestId,
                                                   final String idempotencyKey) throws BlinkServiceException {
        return refundsApiClient.createRefund(request, requestId, idempotencyKey);
    }

    /**
     * Creates a refund.
     *
     * @param request        the {@link RefundDetail}
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link RefundResponse} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<RefundResponse> createRefundAsMono(RefundDetail request, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return refundsApiClient.createRefund(request, requestHeaders);
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId the refund ID
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Refund getRefund(UUID refundId) throws BlinkServiceException {
        try {
            return getRefundAsMono(refundId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId  the refund ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Refund getRefund(UUID refundId, final String requestId) throws BlinkServiceException {
        try {
            return getRefundAsMono(refundId, requestId).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId       the refund ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Payment}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Refund getRefund(UUID refundId, Map<String, String> requestHeaders) throws BlinkServiceException {
        try {
            return getRefundAsMono(refundId, requestHeaders).block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BlinkServiceException b) {
                throw b;
            }
            throw new BlinkServiceException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId the refund ID
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Refund> getRefundAsMono(UUID refundId) throws BlinkServiceException {
        return refundsApiClient.getRefund(refundId);
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId  the refund ID
     * @param requestId the optional request ID. If provided, it overrides the interaction ID generated by Blink Debit.
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Refund> getRefundAsMono(UUID refundId, final String requestId) throws BlinkServiceException {
        return refundsApiClient.getRefund(refundId, requestId);
    }

    /**
     * Retrieves an existing refund by ID.
     *
     * @param refundId       the refund ID
     * @param requestHeaders the {@link Map} of optional request headers
     * @return the {@link Payment} {@link Mono}
     * @throws BlinkServiceException thrown when an exception occurs
     */
    public Mono<Refund> getRefundAsMono(UUID refundId, Map<String, String> requestHeaders)
            throws BlinkServiceException {
        return refundsApiClient.getRefund(refundId, requestHeaders);
    }
}
