package nz.co.blink.debit.client.v1;

import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundResponse;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.HttpClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Client for refund operations.
 */
public class RefundsApiClient {

    private static final Logger log = LoggerFactory.getLogger(RefundsApiClient.class);
    private static final String REFUNDS_PATH = "/payments/v1/refunds";

    private final HttpClientHelper httpHelper;

    public RefundsApiClient(HttpClientHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    /**
     * Create a refund.
     *
     * @param request the refund request
     * @return the refund response
     * @throws BlinkServiceException if the request fails
     */
    public RefundResponse createRefund(RefundDetail request) throws BlinkServiceException {
        return createRefund(request, UUID.randomUUID().toString());
    }

    /**
     * Create a refund with custom request ID.
     *
     * @param request   the refund request
     * @param requestId the request ID for tracing
     * @return the refund response
     * @throws BlinkServiceException if the request fails
     */
    public RefundResponse createRefund(RefundDetail request, String requestId) throws BlinkServiceException {
        return createRefund(request, requestId, null);
    }

    /**
     * Create a refund with custom request ID and idempotency key.
     *
     * <p>The idempotency key is the only de-duplication on refund creation. Pass the key from the
     * original attempt when retrying, and the API replays that refund instead of creating a second
     * one. A null or blank key is generated, which is safe within one call (the built-in retry
     * reuses it) but not across calls.</p>
     *
     * @param request        the refund request
     * @param requestId      the request ID for tracing, generated when null or blank
     * @param idempotencyKey the idempotency key, generated when null or blank
     * @return the refund response
     * @throws BlinkServiceException if the request fails
     */
    public RefundResponse createRefund(RefundDetail request, String requestId, String idempotencyKey)
            throws BlinkServiceException {
        if (request == null) {
            throw new BlinkInvalidValueException("Refund request must not be null");
        }

        log.debug("Creating refund with request-id: {}", requestId);
        return httpHelper.post(REFUNDS_PATH, request, RefundResponse.class, requestId, idempotencyKey);
    }

    /**
     * Get a refund by ID.
     *
     * @param refundId the refund ID
     * @return the refund
     * @throws BlinkServiceException if the request fails
     */
    public Refund getRefund(UUID refundId) throws BlinkServiceException {
        return getRefund(refundId, UUID.randomUUID().toString());
    }

    /**
     * Get a refund by ID with custom request ID.
     *
     * @param refundId  the refund ID
     * @param requestId the request ID for tracing
     * @return the refund
     * @throws BlinkServiceException if the request fails
     */
    public Refund getRefund(UUID refundId, String requestId) throws BlinkServiceException {
        if (refundId == null) {
            throw new BlinkInvalidValueException("Refund ID must not be null");
        }

        String path = REFUNDS_PATH + "/" + refundId.toString();
        log.debug("Getting refund {} with request-id: {}", refundId, requestId);
        return httpHelper.get(path, Refund.class, requestId);
    }
}
