package nz.co.blink.debit.client.v1;

import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundResponse;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.HttpClientHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundsApiClientUnitTest {

    @Mock
    private HttpClientHelper httpHelper;

    private RefundsApiClient client;

    @BeforeEach
    void setUp() {
        client = new RefundsApiClient(httpHelper);
    }

    // ===== createRefund Tests =====

    @Test
    void testCreateRefundSuccess() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);
        RefundResponse expectedResponse = mock(RefundResponse.class);

        when(httpHelper.post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class), anyString(),
                isNull()))
                .thenReturn(expectedResponse);

        RefundResponse result = client.createRefund(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(httpHelper).post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class), anyString(),
                isNull());
    }

    @Test
    void testCreateRefundWithCustomRequestId() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);
        RefundResponse expectedResponse = mock(RefundResponse.class);
        String customRequestId = "custom-request-id-123";

        when(httpHelper.post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class), eq(customRequestId),
                isNull()))
                .thenReturn(expectedResponse);

        RefundResponse result = client.createRefund(request, customRequestId);

        assertThat(result).isEqualTo(expectedResponse);
        verify(httpHelper).post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class),
                eq(customRequestId), isNull());
    }

    @Test
    void testCreateRefundWithNullRequest() throws BlinkServiceException {
        assertThatThrownBy(() -> client.createRefund(null))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("Refund request must not be null");

        verify(httpHelper, never()).post(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void testCreateRefundWithNullRequestAndCustomRequestId() throws BlinkServiceException {
        assertThatThrownBy(() -> client.createRefund(null, "custom-id"))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("Refund request must not be null");

        verify(httpHelper, never()).post(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void testCreateRefundHttpHelperThrowsException() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);

        when(httpHelper.post(anyString(), any(), any(), anyString(), any()))
                .thenThrow(new BlinkServiceException("API error"));

        assertThatThrownBy(() -> client.createRefund(request))
                .isInstanceOf(BlinkServiceException.class)
                .hasMessageContaining("API error");
    }

    @Test
    void testCreateRefundPassesTheCallerSuppliedIdempotencyKeyThrough() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);
        RefundResponse expectedResponse = mock(RefundResponse.class);
        String idempotencyKey = "caller-supplied-not-a-uuid";

        when(httpHelper.post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class), eq("rid-1"),
                eq(idempotencyKey)))
                .thenReturn(expectedResponse);

        RefundResponse result = client.createRefund(request, "rid-1", idempotencyKey);

        assertThat(result).isEqualTo(expectedResponse);
        verify(httpHelper).post(eq("/payments/v1/refunds"), eq(request), eq(RefundResponse.class), eq("rid-1"),
                eq(idempotencyKey));
    }

    @Test
    void testCreateRefundGeneratesARequestIdWhenNoneIsSupplied() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);
        String idempotencyKey = "caller-supplied-not-a-uuid";

        when(httpHelper.post(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(mock(RefundResponse.class));

        client.createRefund(request, null, idempotencyKey);

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpHelper).post(anyString(), any(), any(), requestIdCaptor.capture(), eq(idempotencyKey));
        assertThatCode(() -> UUID.fromString(requestIdCaptor.getValue())).doesNotThrowAnyException();
    }

    @Test
    void testCreateRefundRejectsABlankIdempotencyKeyWithoutCallingTheApi() throws BlinkServiceException {
        RefundDetail request = mock(RefundDetail.class);

        assertThatThrownBy(() -> client.createRefund(request, "rid-1", "   "))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("Idempotency key must not be blank");

        verify(httpHelper, never()).post(anyString(), any(), any(), anyString(), any());
    }

    // ===== getRefund Tests =====

    @Test
    void testGetRefundSuccess() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        Refund expectedRefund = mock(Refund.class);

        when(httpHelper.get(eq("/payments/v1/refunds/" + refundId.toString()), eq(Refund.class), anyString()))
                .thenReturn(expectedRefund);

        Refund result = client.getRefund(refundId);

        assertThat(result).isEqualTo(expectedRefund);
        verify(httpHelper).get(eq("/payments/v1/refunds/" + refundId.toString()), eq(Refund.class), anyString());
    }

    @Test
    void testGetRefundWithCustomRequestId() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();
        Refund expectedRefund = mock(Refund.class);
        String customRequestId = "get-request-id";

        when(httpHelper.get("/payments/v1/refunds/" + refundId.toString(), Refund.class, customRequestId))
                .thenReturn(expectedRefund);

        Refund result = client.getRefund(refundId, customRequestId);

        assertThat(result).isEqualTo(expectedRefund);
        verify(httpHelper).get("/payments/v1/refunds/" + refundId.toString(), Refund.class, customRequestId);
    }

    @Test
    void testGetRefundWithNullId() throws BlinkServiceException {
        assertThatThrownBy(() -> client.getRefund(null))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("Refund ID must not be null");

        verify(httpHelper, never()).get(anyString(), any(), anyString());
    }

    @Test
    void testGetRefundWithNullIdAndCustomRequestId() throws BlinkServiceException {
        assertThatThrownBy(() -> client.getRefund(null, "custom-id"))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("Refund ID must not be null");

        verify(httpHelper, never()).get(anyString(), any(), anyString());
    }

    @Test
    void testGetRefundPathConstruction() throws BlinkServiceException {
        UUID refundId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Refund mockRefund = mock(Refund.class);

        when(httpHelper.get(anyString(), eq(Refund.class), anyString()))
                .thenReturn(mockRefund);

        client.getRefund(refundId);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpHelper).get(pathCaptor.capture(), eq(Refund.class), anyString());

        assertThat(pathCaptor.getValue()).isEqualTo("/payments/v1/refunds/123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void testGetRefundHttpHelperThrowsException() throws BlinkServiceException {
        UUID refundId = UUID.randomUUID();

        when(httpHelper.get(anyString(), any(), anyString()))
                .thenThrow(new BlinkServiceException("API error"));

        assertThatThrownBy(() -> client.getRefund(refundId))
                .isInstanceOf(BlinkServiceException.class)
                .hasMessageContaining("API error");
    }
}
