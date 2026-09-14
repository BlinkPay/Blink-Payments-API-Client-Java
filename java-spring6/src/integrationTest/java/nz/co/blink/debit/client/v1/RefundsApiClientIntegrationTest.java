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

import nz.co.blink.debit.config.BlinkDebitConfiguration;
import nz.co.blink.debit.dto.v1.AccountNumberRefundRequest;
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.Bank;
import nz.co.blink.debit.dto.v1.CreateConsentResponse;
import nz.co.blink.debit.dto.v1.DecoupledFlow;
import nz.co.blink.debit.dto.v1.EnduringConsentRequest;
import nz.co.blink.debit.dto.v1.FullRefundRequest;
import nz.co.blink.debit.dto.v1.IdentifierType;
import nz.co.blink.debit.dto.v1.PartialRefundRequest;
import nz.co.blink.debit.dto.v1.PaymentRequest;
import nz.co.blink.debit.dto.v1.PaymentResponse;
import nz.co.blink.debit.dto.v1.Pcr;
import nz.co.blink.debit.dto.v1.Period;
import nz.co.blink.debit.dto.v1.Refund;
import nz.co.blink.debit.dto.v1.RefundDetail;
import nz.co.blink.debit.dto.v1.RefundResponse;
import nz.co.blink.debit.dto.v1.SingleConsentRequest;
import nz.co.blink.debit.exception.BlinkClientException;
import nz.co.blink.debit.exception.BlinkServiceException;
import nz.co.blink.debit.helpers.AccessTokenHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * The integration test for {@link RefundsApiClient}.
 */
@SpringBootTest(classes = {AccessTokenHandler.class, OAuthApiClient.class, RefundsApiClient.class,
        PaymentsApiClient.class, SingleConsentsApiClient.class, EnduringConsentsApiClient.class})
@Import(BlinkDebitConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RefundsApiClientIntegrationTest {

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    private static final ZoneId ZONE_ID = ZoneId.of("Pacific/Auckland");

    @Autowired
    private EnduringConsentsApiClient enduringConsentsApiClient;

    @Autowired
    private SingleConsentsApiClient singleConsentsApiClient;

    @Autowired
    private PaymentsApiClient paymentsApiClient;

    @Autowired
    private RefundsApiClient client;

    private static UUID consentId;

    private static UUID paymentId;

    private static UUID refundId;

    @Test
    @DisplayName("Verify that account refund for single consent with decoupled flow is created")
    @Order(1)
    void createAccountNumberRefundForSingleConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono =
                singleConsentsApiClient.createSingleConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse createConsentResponse = createConsentResponseMono.block();
        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isBlank();

        for (int i = 1; i <= 10; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId);

                Mono<PaymentResponse> paymentResponseMono = paymentsApiClient.createPayment(paymentRequest);

                assertThat(paymentResponseMono).isNotNull();
                PaymentResponse actual = paymentResponseMono.block();
                assertThat(actual).isNotNull();
                paymentId = actual.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (RuntimeException e) {
                // sleep incrementally
                Thread.sleep(2000 * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for single consent with decoupled flow failed");
        }

        AccountNumberRefundRequest refundRequest = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(paymentId);

        Mono<RefundResponse> refundResponseMono = client.createRefund(refundRequest);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual).isNotNull();
        refundId = actual.getRefundId();
        assertThat(refundId).isNotNull();
    }

    @Test
    @DisplayName("Verify that account number refund for single consent with decoupled flow is retrieved")
    @Order(2)
    void getAccountNumberRefundForSingleConsentWithDecoupledFlow() throws BlinkServiceException {
        if (refundId == null) {
            fail("Refund ID from single consent with decoupled flow is null");
        }

        Mono<Refund> refundMono = client.getRefund(refundId, UUID.randomUUID().toString());

        assertThat(refundMono).isNotNull();
        Refund actual = refundMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED);
        assertThat(actual.getAccountNumber())
                .isNotBlank()
                .startsWith("99-")
                .endsWith("-00");
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Verify that account number refund for enduring consent with decoupled flow is created")
    @Order(3)
    void createAccountNumberRefundForEnduringConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
        EnduringConsentRequest request = new EnduringConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .maximumAmountPeriod(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .maximumAmountPayment(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .period(Period.FORTNIGHTLY)
                .fromTimestamp(OffsetDateTime.now(ZONE_ID))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono =
                enduringConsentsApiClient.createEnduringConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse createConsentResponse = createConsentResponseMono.block();
        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isBlank();

        for (int i = 1; i <= 10; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId)
                        .amount(new Amount()
                                .currency(Amount.CurrencyEnum.NZD)
                                .total("45.00"))
                        .pcr(new Pcr()
                                .particulars("particulars")
                                .code("code")
                                .reference("reference"));

                Mono<PaymentResponse> paymentResponseMono = paymentsApiClient.createPayment(paymentRequest);

                assertThat(paymentResponseMono).isNotNull();
                PaymentResponse actual = paymentResponseMono.block();
                assertThat(actual).isNotNull();
                paymentId = actual.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (RuntimeException e) {
                // sleep incrementally
                Thread.sleep(2000 * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for enduring consent with decoupled flow failed");
        }

        AccountNumberRefundRequest refundRequest = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
                .paymentId(paymentId);

        Mono<RefundResponse> refundResponseMono = client.createRefund(refundRequest);

        assertThat(refundResponseMono).isNotNull();
        RefundResponse actual = refundResponseMono.block();
        assertThat(actual).isNotNull();
        refundId = actual.getRefundId();
        assertThat(refundId).isNotNull();
    }

    @Test
    @DisplayName("Verify that account number refund for enduring consent with decoupled flow is retrieved")
    @Order(4)
    void getAccountNumberRefundForEnduringConsentWithDecoupledFlow() throws BlinkServiceException {
        if (refundId == null) {
            fail("Refund ID from enduring consent with decoupled flow is null");
        }

        Mono<Refund> refundMono = client.getRefund(refundId, UUID.randomUUID().toString());

        assertThat(refundMono).isNotNull();
        Refund actual = refundMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Refund::getRefundId, Refund::getStatus)
                .containsExactly(refundId, Refund.StatusEnum.COMPLETED);
        assertThat(actual.getAccountNumber())
                .isNotBlank()
                .startsWith("99-")
                .endsWith("-00");
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        RefundDetail refundDetail = (RefundDetail) actual.getDetail();
        assertThat(refundDetail)
                .isNotNull()
                .extracting(RefundDetail::getPaymentId, RefundDetail::getType)
                .containsExactly(paymentId, RefundDetail.TypeEnum.ACCOUNT_NUMBER);
    }

    @Test
    @DisplayName("Verify that full refund for single consent with decoupled flow is handled")
    @Order(5)
    void createFullRefundForSingleConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono =
                singleConsentsApiClient.createSingleConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse createConsentResponse = createConsentResponseMono.block();
        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isBlank();

        for (int i = 1; i <= 10; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId);

                Mono<PaymentResponse> paymentResponseMono = paymentsApiClient.createPayment(paymentRequest);

                assertThat(paymentResponseMono).isNotNull();
                PaymentResponse actual = paymentResponseMono.block();
                assertThat(actual).isNotNull();
                paymentId = actual.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (RuntimeException e) {
                // sleep incrementally
                Thread.sleep(2000 * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for single consent with decoupled flow failed");
        }

        FullRefundRequest refundRequest = (FullRefundRequest) new FullRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .paymentId(paymentId);

        RuntimeException exception = catchThrowableOfType(RuntimeException.class,
                () -> client.createRefund(refundRequest).block());

        assertThat(exception).isNotNull();
        assertThat(exception.getCause())
                .isNotNull()
                .isInstanceOf(BlinkClientException.class)
                .hasMessageContaining("full_refund")
                .hasMessageContaining("not yet supported");
    }

    @Test
    @DisplayName("Verify that partial refund for single consent with decoupled flow is handled")
    @Order(6)
    void createPartialRefundForSingleConsentWithDecoupledFlow()
            throws InterruptedException, BlinkServiceException {
        SingleConsentRequest request = new SingleConsentRequest()
                .flow(new AuthFlow()
                        .detail(new DecoupledFlow()
                                .bank(Bank.PNZ)
                                .identifierType(IdentifierType.PHONE_NUMBER)
                                .identifierValue("+64-259531933")
                                .callbackUrl(CALLBACK_URL)))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("50.00"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .hashedCustomerIdentifier("88df3798e32512ac340164f7ed133343d6dcb4888e4a91b03512dedd9800d12e");

        Mono<CreateConsentResponse> createConsentResponseMono =
                singleConsentsApiClient.createSingleConsent(request);

        assertThat(createConsentResponseMono).isNotNull();
        CreateConsentResponse createConsentResponse = createConsentResponseMono.block();
        assertThat(createConsentResponse).isNotNull();
        consentId = createConsentResponse.getConsentId();
        assertThat(consentId).isNotNull();
        assertThat(createConsentResponse.getRedirectUri()).isBlank();

        for (int i = 1; i <= 10; i++) {
            System.out.println("attempt: " + i);
            try {
                PaymentRequest paymentRequest = new PaymentRequest()
                        .consentId(consentId);

                Mono<PaymentResponse> paymentResponseMono = paymentsApiClient.createPayment(paymentRequest);

                assertThat(paymentResponseMono).isNotNull();
                PaymentResponse actual = paymentResponseMono.block();
                assertThat(actual).isNotNull();
                paymentId = actual.getPaymentId();
                assertThat(paymentId).isNotNull();

                break;
            } catch (RuntimeException e) {
                // sleep incrementally
                Thread.sleep(2000 * i);
            }
        }

        if (paymentId == null) {
            fail("Payment for single consent with decoupled flow failed");
        }

        PartialRefundRequest refundRequest = (PartialRefundRequest) new PartialRefundRequest()
                .consentRedirect("https://www.mymerchant.co.nz")
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference"))
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.50"))
                .paymentId(paymentId);

        RuntimeException exception = catchThrowableOfType(RuntimeException.class,
                () -> client.createRefund(refundRequest).block());

        assertThat(exception).isNotNull();
        assertThat(exception.getCause())
                .isNotNull()
                .isInstanceOf(BlinkClientException.class)
                .hasMessageContaining("partial_refund")
                .hasMessageContaining("not yet supported");
    }
}
