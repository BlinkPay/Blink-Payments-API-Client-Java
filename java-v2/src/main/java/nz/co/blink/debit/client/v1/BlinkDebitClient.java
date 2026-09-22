package nz.co.blink.debit.client.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.helpers.HttpClientHelper;
import nz.co.blink.debit.service.AccessTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Main facade for Blink Debit API client.
 * Provides synchronous access to all API operations.
 * <p>
 * Example usage:
 * <pre>
 * BlinkDebitConfig config = BlinkDebitConfig.builder()
 *     .debitUrl("https://staging.debit.blinkpay.co.nz")
 *     .clientId("your-client-id")
 *     .clientSecret("your-client-secret")
 *     .build();
 *
 * BlinkDebitClient client = new BlinkDebitClient(config);
 * CreateConsentResponse response = client.createSingleConsent(request);
 * </pre>
 */
public class BlinkDebitClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(BlinkDebitClient.class);

    private final BlinkDebitConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AccessTokenManager tokenManager;
    private final HttpClientHelper httpHelper;

    // API clients
    private final OAuthApiClient oauthApi;
    private final SingleConsentsApiClient singleConsentsApi;
    private final EnduringConsentsApiClient enduringConsentsApi;
    private final QuickPaymentsApiClient quickPaymentsApi;
    private final PaymentsApiClient paymentsApi;
    private final RefundsApiClient refundsApi;
    private final MetaApiClient metaApi;

    /**
     * Create a client from environment variables.
     * Looks for: BLINKPAY_DEBIT_URL, BLINKPAY_CLIENT_ID, BLINKPAY_CLIENT_SECRET
     *
     * @throws BlinkInvalidValueException if required config is missing
     */
    public BlinkDebitClient() throws BlinkInvalidValueException {
        this(BlinkDebitConfig.fromEnvironment().build());
    }

    /**
     * Create a client with direct parameters.
     * Compatible with Spring SDK API for easy migration.
     *
     * @param debitUrl the Blink Debit API base URL
     * @param clientId the OAuth2 client ID
     * @param clientSecret the OAuth2 client secret
     * @throws BlinkInvalidValueException if required config is missing
     */
    public BlinkDebitClient(String debitUrl, String clientId, String clientSecret)
            throws BlinkInvalidValueException {
        this(BlinkDebitConfig.builder()
                .debitUrl(debitUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build());
    }

    /**
     * Create a client with the specified configuration.
     *
     * @param config the configuration
     */
    public BlinkDebitClient(BlinkDebitConfig config) {
        this.config = config;

        // Create HTTP client with connection pool
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(config.getTimeout())
                .build();

        // Create Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create OAuth client and token manager
        this.oauthApi = new OAuthApiClient(httpClient, config, objectMapper);
        this.tokenManager = new AccessTokenManager(oauthApi);

        // Create HTTP helper
        this.httpHelper = new HttpClientHelper(httpClient, config, objectMapper, tokenManager);

        // Initialize API clients
        this.singleConsentsApi = new SingleConsentsApiClient(httpHelper);
        this.enduringConsentsApi = new EnduringConsentsApiClient(httpHelper);
        this.quickPaymentsApi = new QuickPaymentsApiClient(httpHelper);
        this.paymentsApi = new PaymentsApiClient(httpHelper);
        this.refundsApi = new RefundsApiClient(httpHelper);
        this.metaApi = new MetaApiClient(httpHelper);

        log.info("BlinkDebitClient initialized for {}", config.getDebitUrl());
    }

    /**
     * Get the OAuth API client.
     */
    public OAuthApiClient getOAuthApi() {
        return oauthApi;
    }

    /**
     * Get the single consents API client.
     */
    public SingleConsentsApiClient getSingleConsentsApi() {
        return singleConsentsApi;
    }

    /**
     * Get the enduring consents API client.
     */
    public EnduringConsentsApiClient getEnduringConsentsApi() {
        return enduringConsentsApi;
    }

    /**
     * Get the quick payments API client.
     */
    public QuickPaymentsApiClient getQuickPaymentsApi() {
        return quickPaymentsApi;
    }

    /**
     * Get the payments API client.
     */
    public PaymentsApiClient getPaymentsApi() {
        return paymentsApi;
    }

    /**
     * Get the refunds API client.
     */
    public RefundsApiClient getRefundsApi() {
        return refundsApi;
    }

    /**
     * Get the metadata API client.
     */
    public MetaApiClient getMetaApi() {
        return metaApi;
    }

    /**
     * Get the configuration.
     */
    public BlinkDebitConfig getConfig() {
        return config;
    }

    // ========================================================================
    // Convenience Methods - Direct API access without requiring getXxxApi()
    // ========================================================================

    /**
     * Get bank metadata.
     * Convenience method equivalent to getMetaApi().getMeta().
     *
     * @return list of bank metadata
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public java.util.List<nz.co.blink.debit.dto.v1.BankMetadata> getMeta()
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return metaApi.getMeta();
    }

    /**
     * Create a single consent.
     * Convenience method equivalent to getSingleConsentsApi().createSingleConsent().
     *
     * @param request the single consent request
     * @return the consent creation response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.CreateConsentResponse createSingleConsent(
            nz.co.blink.debit.dto.v1.SingleConsentRequest request)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return singleConsentsApi.createSingleConsent(request);
    }

    /**
     * Get a single consent by ID.
     * Convenience method equivalent to getSingleConsentsApi().getConsent().
     *
     * @param consentId the consent ID
     * @return the consent
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.Consent getSingleConsent(java.util.UUID consentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return singleConsentsApi.getConsent(consentId);
    }

    /**
     * Create a quick payment.
     * Convenience method equivalent to getQuickPaymentsApi().createQuickPayment().
     *
     * @param request the quick payment request
     * @return the quick payment creation response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.CreateQuickPaymentResponse createQuickPayment(
            nz.co.blink.debit.dto.v1.QuickPaymentRequest request)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return quickPaymentsApi.createQuickPayment(request);
    }

    /**
     * Get a quick payment by ID.
     * Convenience method equivalent to getQuickPaymentsApi().getQuickPayment().
     *
     * @param quickPaymentId the quick payment ID
     * @return the quick payment response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.QuickPaymentResponse getQuickPayment(java.util.UUID quickPaymentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return quickPaymentsApi.getQuickPayment(quickPaymentId);
    }

    /**
     * Revoke a quick payment.
     * Convenience method equivalent to getQuickPaymentsApi().revokeQuickPayment().
     *
     * @param quickPaymentId the quick payment ID
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public void revokeQuickPayment(java.util.UUID quickPaymentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        quickPaymentsApi.revokeQuickPayment(quickPaymentId);
    }

    /**
     * Revoke a single consent.
     * Convenience method equivalent to getSingleConsentsApi().revokeConsent().
     *
     * @param consentId the consent ID
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public void revokeSingleConsent(java.util.UUID consentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        singleConsentsApi.revokeConsent(consentId);
    }

    /**
     * Create an enduring consent.
     * Convenience method equivalent to getEnduringConsentsApi().createEnduringConsent().
     *
     * @param request the enduring consent request
     * @return the consent creation response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.CreateConsentResponse createEnduringConsent(
            nz.co.blink.debit.dto.v1.EnduringConsentRequest request)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return enduringConsentsApi.createEnduringConsent(request);
    }

    /**
     * Get an enduring consent by ID.
     * Convenience method equivalent to getEnduringConsentsApi().getConsent().
     *
     * @param consentId the consent ID
     * @return the consent
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.Consent getEnduringConsent(java.util.UUID consentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return enduringConsentsApi.getConsent(consentId);
    }

    /**
     * Revoke an enduring consent.
     * Convenience method equivalent to getEnduringConsentsApi().revokeConsent().
     *
     * @param consentId the consent ID
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public void revokeEnduringConsent(java.util.UUID consentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        enduringConsentsApi.revokeConsent(consentId);
    }

    /**
     * Create a payment.
     * Convenience method equivalent to getPaymentsApi().createPayment().
     *
     * @param request the payment request
     * @return the payment response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.PaymentResponse createPayment(
            nz.co.blink.debit.dto.v1.PaymentRequest request)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return paymentsApi.createPayment(request);
    }

    /**
     * Get a payment by ID.
     * Convenience method equivalent to getPaymentsApi().getPayment().
     *
     * @param paymentId the payment ID
     * @return the payment
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.Payment getPayment(java.util.UUID paymentId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return paymentsApi.getPayment(paymentId);
    }

    /**
     * Create a refund.
     * Convenience method equivalent to getRefundsApi().createRefund().
     *
     * @param request the refund request
     * @return the refund response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.RefundResponse createRefund(
            nz.co.blink.debit.dto.v1.RefundDetail request)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return refundsApi.createRefund(request);
    }

    /**
     * Create a refund with a caller-supplied idempotency key.
     * Convenience method equivalent to getRefundsApi().createRefund().
     *
     * <p>Reuse the key from the original attempt when retrying a refund, and the API replays that
     * refund rather than creating a second one.</p>
     *
     * @param request        the refund request
     * @param requestId      the request ID for tracing, generated when null or blank
     * @param idempotencyKey the idempotency key, generated when null or blank
     * @return the refund response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.RefundResponse createRefund(
            nz.co.blink.debit.dto.v1.RefundDetail request, String requestId, String idempotencyKey)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return refundsApi.createRefund(request, requestId, idempotencyKey);
    }

    /**
     * Get a refund by ID.
     * Convenience method equivalent to getRefundsApi().getRefund().
     *
     * @param refundId the refund ID
     * @return the refund
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     */
    public nz.co.blink.debit.dto.v1.Refund getRefund(java.util.UUID refundId)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        return refundsApi.getRefund(refundId);
    }

    // ========================================================================
    // Await Helper Methods - Polling utilities for async operations
    // ========================================================================

    /**
     * Poll for a successful quick payment within the specified time.
     * Compatible with Spring SDK API.
     * <p>
     * Polls every second until the quick payment consent is AUTHORISED or CONSUMED,
     * or until the timeout is reached. Attempts to revoke the quick payment on timeout.
     *
     * @param quickPaymentId the quick payment ID
     * @param maxWaitSeconds the maximum number of seconds to wait
     * @return the successful quick payment response
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     * @throws RuntimeException wrapping timeout or rejection exceptions
     */
    public nz.co.blink.debit.dto.v1.QuickPaymentResponse awaitSuccessfulQuickPaymentOrThrowException(
            java.util.UUID quickPaymentId, int maxWaitSeconds)
            throws nz.co.blink.debit.exception.BlinkServiceException {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            nz.co.blink.debit.dto.v1.QuickPaymentResponse response = getQuickPayment(quickPaymentId);
            nz.co.blink.debit.dto.v1.Consent.StatusEnum status = response.getConsent().getStatus();

            log.debug("Polling quick payment {}: consent status = {}", quickPaymentId, status);

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.AUTHORISED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.CONSUMED) {
                return response;
            }

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REJECTED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REVOKED) {
                throw new RuntimeException("Quick payment " + quickPaymentId + " was rejected or revoked");
            }

            try {
                Thread.sleep(1000); // Poll every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new nz.co.blink.debit.exception.BlinkServiceException("Polling interrupted", e);
            }
        }

        // Timeout - attempt to revoke
        log.warn("Quick payment {} timed out after {} seconds, attempting to revoke",
                quickPaymentId, maxWaitSeconds);
        try {
            quickPaymentsApi.revokeQuickPayment(quickPaymentId);
            log.info("Revoked quick payment {} after timeout", quickPaymentId);
        } catch (Exception e) {
            log.error("Failed to revoke quick payment {} after timeout", quickPaymentId, e);
        }

        throw new RuntimeException("Timed out waiting for quick payment " + quickPaymentId +
                " to complete after " + maxWaitSeconds + " seconds");
    }

    /**
     * Poll for an authorised single consent within the specified time.
     * Compatible with Spring SDK API.
     *
     * @param consentId the consent ID
     * @param maxWaitSeconds the maximum number of seconds to wait
     * @return the authorised consent
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     * @throws nz.co.blink.debit.exception.BlinkConsentTimeoutException if polling times out
     * @throws nz.co.blink.debit.exception.BlinkConsentRejectedException if consent is rejected
     */
    public nz.co.blink.debit.dto.v1.Consent awaitAuthorisedSingleConsentOrThrowException(
            java.util.UUID consentId, int maxWaitSeconds)
            throws nz.co.blink.debit.exception.BlinkServiceException,
                   nz.co.blink.debit.exception.BlinkConsentTimeoutException,
                   nz.co.blink.debit.exception.BlinkConsentRejectedException {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            nz.co.blink.debit.dto.v1.Consent consent = getSingleConsent(consentId);
            nz.co.blink.debit.dto.v1.Consent.StatusEnum status = consent.getStatus();

            log.debug("Polling single consent {}: status = {}", consentId, status);

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.AUTHORISED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.CONSUMED) {
                return consent;
            }

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REJECTED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REVOKED) {
                throw new nz.co.blink.debit.exception.BlinkConsentRejectedException(
                        "Single consent " + consentId + " was rejected or revoked");
            }

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.GATEWAY_TIMEOUT) {
                throw new nz.co.blink.debit.exception.BlinkConsentTimeoutException(
                        "Gateway timed out for single consent " + consentId);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new nz.co.blink.debit.exception.BlinkServiceException("Polling interrupted", e);
            }
        }

        throw new nz.co.blink.debit.exception.BlinkConsentTimeoutException(
                "Timed out waiting for single consent " + consentId + " after " + maxWaitSeconds + " seconds");
    }

    /**
     * Poll for an authorised enduring consent within the specified time.
     * Attempts to revoke consent on timeout. Compatible with Spring SDK API.
     *
     * @param consentId the consent ID
     * @param maxWaitSeconds the maximum number of seconds to wait
     * @return the authorised consent
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     * @throws nz.co.blink.debit.exception.BlinkConsentTimeoutException if polling times out
     * @throws nz.co.blink.debit.exception.BlinkConsentRejectedException if consent is rejected
     */
    public nz.co.blink.debit.dto.v1.Consent awaitAuthorisedEnduringConsentOrThrowException(
            java.util.UUID consentId, int maxWaitSeconds)
            throws nz.co.blink.debit.exception.BlinkServiceException,
                   nz.co.blink.debit.exception.BlinkConsentTimeoutException,
                   nz.co.blink.debit.exception.BlinkConsentRejectedException {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            nz.co.blink.debit.dto.v1.Consent consent = getEnduringConsent(consentId);
            nz.co.blink.debit.dto.v1.Consent.StatusEnum status = consent.getStatus();

            log.debug("Polling enduring consent {}: status = {}", consentId, status);

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.AUTHORISED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.CONSUMED) {
                return consent;
            }

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REJECTED ||
                status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.REVOKED) {
                throw new nz.co.blink.debit.exception.BlinkConsentRejectedException(
                        "Enduring consent " + consentId + " was rejected or revoked");
            }

            if (status == nz.co.blink.debit.dto.v1.Consent.StatusEnum.GATEWAY_TIMEOUT) {
                throw new nz.co.blink.debit.exception.BlinkConsentTimeoutException(
                        "Gateway timed out for enduring consent " + consentId);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new nz.co.blink.debit.exception.BlinkServiceException("Polling interrupted", e);
            }
        }

        // Timeout - attempt to revoke
        nz.co.blink.debit.exception.BlinkConsentTimeoutException timeoutException =
                new nz.co.blink.debit.exception.BlinkConsentTimeoutException(
                        "Timed out waiting for enduring consent " + consentId + " after " + maxWaitSeconds + " seconds");

        try {
            revokeEnduringConsent(consentId);
            log.info("Revoked enduring consent {} after timeout", consentId);
        } catch (Exception e) {
            log.error("Failed to revoke enduring consent {} after timeout", consentId, e);
        }

        throw timeoutException;
    }

    /**
     * Poll for a successful payment within the specified time.
     * Compatible with Spring SDK API.
     *
     * @param paymentId the payment ID
     * @param maxWaitSeconds the maximum number of seconds to wait
     * @return the successful payment
     * @throws nz.co.blink.debit.exception.BlinkServiceException if API call fails
     * @throws nz.co.blink.debit.exception.BlinkPaymentTimeoutException if polling times out
     * @throws nz.co.blink.debit.exception.BlinkPaymentRejectedException if payment is rejected
     */
    public nz.co.blink.debit.dto.v1.Payment awaitSuccessfulPaymentOrThrowException(
            java.util.UUID paymentId, int maxWaitSeconds)
            throws nz.co.blink.debit.exception.BlinkServiceException,
                   nz.co.blink.debit.exception.BlinkPaymentTimeoutException,
                   nz.co.blink.debit.exception.BlinkPaymentRejectedException {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            nz.co.blink.debit.dto.v1.Payment payment = getPayment(paymentId);
            nz.co.blink.debit.dto.v1.Payment.StatusEnum status = payment.getStatus();

            log.debug("Polling payment {}: status = {}", paymentId, status);

            if (status == nz.co.blink.debit.dto.v1.Payment.StatusEnum.ACCEPTED_SETTLEMENT_COMPLETED) {
                return payment;
            }

            if (status == nz.co.blink.debit.dto.v1.Payment.StatusEnum.REJECTED) {
                throw new nz.co.blink.debit.exception.BlinkPaymentRejectedException(
                        "Payment " + paymentId + " was rejected");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new nz.co.blink.debit.exception.BlinkServiceException("Polling interrupted", e);
            }
        }

        throw new nz.co.blink.debit.exception.BlinkPaymentTimeoutException(
                "Timed out waiting for payment " + paymentId + " after " + maxWaitSeconds + " seconds");
    }

    /**
     * Close the client and release resources.
     */
    @Override
    public void close() {
        log.info("BlinkDebitClient closed");
        // HttpClient doesn't need explicit closing in Java 11
    }
}
