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
import nz.co.blink.debit.dto.v1.Amount;
import nz.co.blink.debit.dto.v1.AuthFlow;
import nz.co.blink.debit.dto.v1.Bank;
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
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * The integration test for {@link PaymentsApiClient}.
 */
@SpringBootTest(classes = {AccessTokenHandler.class, OAuthApiClient.class, PaymentsApiClient.class,
        SingleConsentsApiClient.class, EnduringConsentsApiClient.class})
@Import(BlinkDebitConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentsApiClientIntegrationTest {

    private static final String CALLBACK_URL = "https://www.mymerchant.co.nz/callback";

    private static final ZoneId ZONE_ID = ZoneId.of("Pacific/Auckland");

    @Autowired
    private EnduringConsentsApiClient enduringConsentsApiClient;

    @Autowired
    private SingleConsentsApiClient singleConsentsApiClient;

    @Autowired
    private PaymentsApiClient client;

    private static UUID consentId;

    private static UUID paymentId;

    @Test
    @DisplayName("Verify that payment for single consent with decoupled flow is created")
    @Order(1)
    void createPaymentForSingleConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
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

                Mono<PaymentResponse> paymentResponseMono = client.createPayment(paymentRequest);

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
    }

    @Test
    @DisplayName("Verify that payment for single consent with decoupled flow is retrieved")
    @Order(2)
    void getPaymentForSingleConsentWithDecoupledFlow() throws BlinkServiceException {
        if (paymentId == null) {
            fail("Payment ID from single consent with decoupled flow is null");
        }

        Mono<Payment> paymentMono = client.getPayment(paymentId);

        assertThat(paymentMono).isNotNull();
        Payment actual = paymentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.SINGLE, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        PaymentRequest paymentRequest = actual.getDetail();
        assertThat(paymentRequest)
                .isNotNull()
                .extracting(PaymentRequest::getConsentId, PaymentRequest::getPcr, PaymentRequest::getAmount)
                .containsExactly(consentId, null, new Amount().currency(Amount.CurrencyEnum.NZD).total("50.00"));
    }

    @Test
    @DisplayName("Verify that payment for enduring consent with decoupled flow is created")
    @Order(3)
    void createPaymentForEnduringConsentWithDecoupledFlow() throws InterruptedException, BlinkServiceException {
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

                Mono<PaymentResponse> paymentResponseMono = client.createPayment(paymentRequest);

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
    }

    @Test
    @DisplayName("Verify that payment for enduring consent with decoupled flow is retrieved")
    @Order(4)
    void getPaymentForEnduringConsentWithDecoupledFlow() throws BlinkServiceException {
        if (paymentId == null) {
            fail("Payment ID from enduring consent with decoupled flow is null");
        }

        Mono<Payment> paymentMono = client.getPayment(paymentId);

        assertThat(paymentMono).isNotNull();
        Payment actual = paymentMono.block();
        assertThat(actual)
                .isNotNull()
                .extracting(Payment::getPaymentId, Payment::getType, Payment::getStatus, Payment::getRefunds)
                .containsExactly(paymentId, Payment.TypeEnum.ENDURING, Payment.StatusEnum.ACCEPTEDSETTLEMENTCOMPLETED,
                        Collections.emptyList());
        assertThat(actual.getCreationTimestamp()).isNotNull();
        assertThat(actual.getStatusUpdatedTimestamp()).isNotNull();
        PaymentRequest paymentRequest = actual.getDetail();
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
                .containsExactly("particulars", "code", "reference");
    }
}
