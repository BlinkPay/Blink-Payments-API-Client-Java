/**
 * Copyright (c) 2025 BlinkPay
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

import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.Payment;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.exception.BlinkServiceException;
import org.junit.jupiter.api.AfterAll;

import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import static nz.co.blink.debit.IntegrationTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link PaymentsApiClient}.
 * Tests payment creation from authorized consents using decoupled flow.
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentsApiClientIntegrationTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Pacific/Auckland");

    private static BlinkDebitClient client;
    private static boolean credentialsAvailable;
    private static UUID consentId;
    private static UUID paymentId;

    @BeforeAll
    static void setUp() throws BlinkInvalidValueException {
        String debitUrl = System.getenv("BLINKPAY_DEBIT_URL");
        String clientId = System.getenv("BLINKPAY_CLIENT_ID");
        String clientSecret = System.getenv("BLINKPAY_CLIENT_SECRET");

        credentialsAvailable = debitUrl != null && clientId != null && clientSecret != null;

        if (credentialsAvailable) {
            BlinkDebitConfig config = BlinkDebitConfig.builder()
                    .debitUrl(debitUrl)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            client = new BlinkDebitClient(config);
        }
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName("Verify that payment for single consent with decoupled flow is created")
    @Order(1)
    void createPaymentForSingleConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");

        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(DEFAULT_BANK)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue(PHONE_NUMBER)
                                .callbackUrl(URI.create(CALLBACK_URL))))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .pcr(new Pcr()
                        .particulars(PARTICULARS)
                        .code(CODE)
                        .reference(REFERENCE))
                .hashedCustomerIdentifier(CUSTOMER_HASH);

        CreateConsentResponse createConsentResponse = client.getSingleConsentsApi().createSingleConsent(request);

        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isNull();

        for (int i = 1; i <= 15; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId);

                PaymentResponse paymentResponse = client.getPaymentsApi().createPayment(paymentRequest);

                assertThat(paymentResponse).isNotNull();
                paymentId = paymentResponse.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (Exception e) {
                // sleep incrementally
                Thread.sleep(2000L * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for single consent with decoupled flow failed");
        }
    }

    @Test
    @DisplayName("Verify that payment for single consent with decoupled flow is retrieved")
    @Order(2)
    void getPaymentForSingleConsentWithDecoupledFlow() throws BlinkServiceException {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");
        assumeTrue(paymentId != null, "Payment not created in previous test");

        Payment payment = client.getPaymentsApi().getPayment(paymentId);

        assertThat(payment)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTED_SETTLEMENT_COMPLETED,
                        Collections.emptyList());
        assertThat(payment.getCreationTimestamp()).isNotNull();
        assertThat(payment.getStatusUpdatedTimestamp()).isNotNull();
        PaymentRequest paymentRequest = payment.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr,
                        paymentDetail -> paymentDetail.getAmount().getCurrency(),
                        paymentDetail -> paymentDetail.getAmount().getTotal())
                .containsExactly(consentId, null, Amount.CurrencyEnum.NZD, "50.00");
    }

    @Test
    @DisplayName("Verify that payment for enduring consent with decoupled flow is created")
    @Order(3)
    void createPaymentForEnduringConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");

        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(DEFAULT_BANK)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue(PHONE_NUMBER)
                                .callbackUrl(URI.create(CALLBACK_URL))))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.FORTNIGHTLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .hashedCustomerIdentifier(CUSTOMER_HASH);

        CreateConsentResponse createConsentResponse = client.getEnduringConsentsApi().createEnduringConsent(request);

        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isNull();

        for (int i = 1; i <= 15; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId)
                        .amount(new Amount()
                                .currency(Amount.CurrencyEnum.NZD)
                                .total("45.00"))
                        .pcr(new Pcr()
                                .particulars(PARTICULARS)
                                .code(CODE)
                                .reference(REFERENCE));

                PaymentResponse paymentResponse = client.getPaymentsApi().createPayment(paymentRequest);

                assertThat(paymentResponse).isNotNull();
                paymentId = paymentResponse.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (Exception e) {
                // sleep incrementally
                Thread.sleep(2000L * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for enduring consent with decoupled flow failed");
        }
    }

    @Test
    @DisplayName("Verify that payment for enduring consent with decoupled flow is retrieved")
    @Order(4)
    void getPaymentForEnduringConsentWithDecoupledFlow() throws BlinkServiceException {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");
        assumeTrue(paymentId != null, "Payment not created in previous test");

        Payment payment = client.getPaymentsApi().getPayment(paymentId);

        assertThat(payment)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.ENDURING, Payment.StatusEnum.ACCEPTED_SETTLEMENT_COMPLETED,
                        Collections.emptyList());
        assertThat(payment.getCreationTimestamp()).isNotNull();
        assertThat(payment.getStatusUpdatedTimestamp()).isNotNull();
        PaymentRequest paymentRequest = payment.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId)
                .isEqualTo(consentId);
        assertThat(paymentRequest.getAmount())
                .isNotNull()
                .extracting(Amount::getCurrency, Amount::getTotal)
                .containsExactly(Amount.CurrencyEnum.NZD, "45.00");
        assertThat(paymentRequest.getPcr())
                .isNotNull()
                .extracting(Pcr::getParticulars, Pcr::getCode, Pcr::getReference)
                .containsExactly(PARTICULARS, CODE, REFERENCE);
    }

    @Test
    @DisplayName("Verify that getting non-existent payment returns 404 error")
    @Order(5)
    void getNonExistentPayment() {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");

        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> client.getPaymentsApi().getPayment(nonExistentId))
                .isInstanceOf(BlinkServiceException.class)
                .hasMessageContaining("HTTP 404");
    }

    @Test
    @DisplayName("Verify that creating payment with null request throws validation error")
    @Order(6)
    void createPaymentWithNullRequest() {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");

        assertThatThrownBy(() -> client.getPaymentsApi().createPayment(null))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Verify that getting payment with null ID throws validation error")
    @Order(7)
    void getPaymentWithNullId() {
        assumeTrue(credentialsAvailable, "Integration test credentials not available");

        assertThatThrownBy(() -> client.getPaymentsApi().getPayment(null))
                .isInstanceOf(BlinkInvalidValueException.class)
                .hasMessageContaining("must not be null");
    }
}
