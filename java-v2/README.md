# Blink Debit API Client for Java (v2)

Lightweight Java SDK for integrating with **Blink PayNow** (one-off payments) and **Blink AutoPay** (recurring payments).

[![Maven Central](https://img.shields.io/maven-central/v/nz.co.blinkpay/blink-debit-api-client-java.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/nz.co.blinkpay/blink-debit-api-client-java)

## Features

- ✅ **Lightweight**: Minimal runtime dependencies
- ✅ **Synchronous API**: Simple blocking calls using Java 11+ HttpClient
- ✅ **No Framework Dependencies**: Works with any Java 11+ application
- ✅ **Thread-Safe**: Automatic OAuth2 token management
- ✅ **AutoCloseable**: Proper resource management

## Requirements

- Java 11 or higher
- Maven 3 or Gradle 7

## Installation

### Maven
```xml
<dependency>
    <groupId>nz.co.blinkpay</groupId>
    <artifactId>blink-debit-api-client-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'nz.co.blinkpay:blink-debit-api-client-java:2.0.0'
```

## Quick Start

```java
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.config.BlinkDebitConfig;
import nz.co.blink.debit.dto.v1.*;

// Create client using environment variables
try (BlinkDebitClient client = new BlinkDebitClient()) {

    // Create a quick payment
    QuickPaymentRequest request = new QuickPaymentRequest()
            .flow(new AuthFlow()
                    .detail(new GatewayFlow()
                            .redirectUri("https://www.example.com/return")))
            .amount(new Amount()
                    .currency(Amount.CurrencyEnum.NZD)
                    .total("10.00"))
            .pcr(new Pcr()
                    .particulars("particulars")
                    .code("code")
                    .reference("reference"));

    CreateQuickPaymentResponse response = client.getQuickPaymentsApi()
            .createQuickPayment(request);

    // Redirect user to response.getRedirectUri()
    System.out.println("Redirect to: " + response.getRedirectUri());
}
```

## Configuration

### Environment Variables (Recommended)

```bash
export BLINKPAY_DEBIT_URL=https://staging.debit.blinkpay.co.nz
export BLINKPAY_CLIENT_ID=your-client-id
export BLINKPAY_CLIENT_SECRET=your-client-secret
```

### Builder Pattern

```java
BlinkDebitConfig config = BlinkDebitConfig.builder()
        .debitUrl("https://staging.debit.blinkpay.co.nz")
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .timeout(Duration.ofSeconds(30))  // Optional, default: 10s
        .build();

BlinkDebitClient client = new BlinkDebitClient(config);
```

### Configuration Options

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| debitUrl | BLINKPAY_DEBIT_URL | - | Blink API base URL (required) |
| clientId | BLINKPAY_CLIENT_ID | - | OAuth2 client ID (required) |
| clientSecret | BLINKPAY_CLIENT_SECRET | - | OAuth2 client secret (required) |
| timeout | BLINKPAY_TIMEOUT | 10s | Request timeout duration |

**URLs**:
- Sandbox: `https://sandbox.debit.blinkpay.co.nz`
- Production: `https://debit.blinkpay.co.nz`

## API Usage

### Single Consents

```java
// Create single consent
SingleConsentRequest consentRequest = new SingleConsentRequest()
        .flow(new AuthFlow()
                .detail(new RedirectFlow()
                        .bank(Bank.PNZ)
                        .redirectUri("https://www.example.com/return")))
        .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("50.00"))
        .pcr(new Pcr()
                .particulars("particulars")
                .code("code")
                .reference("reference"));

CreateConsentResponse consentResponse = client.getSingleConsentsApi()
        .createSingleConsent(consentRequest);

// Get consent
UUID consentId = consentResponse.getConsentId();
Consent consent = client.getSingleConsentsApi().getConsent(consentId);

// Wait for authorization (does NOT auto-revoke on timeout)
Consent authorisedConsent = client.awaitAuthorisedSingleConsentOrThrowException(consentId, 300);

// Revoke consent (manual)
client.getSingleConsentsApi().revokeConsent(consentId);
```

### Enduring Consents

```java
// Create enduring consent
EnduringConsentRequest enduringRequest = new EnduringConsentRequest()
        .flow(new AuthFlow()
                .detail(new RedirectFlow()
                        .bank(Bank.PNZ)
                        .redirectUri("https://www.example.com/return")))
        .maximumAmountPeriod(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("500.00"))
        .period(Period.MONTHLY)
        .fromTimestamp(OffsetDateTime.now())
        .toTimestamp(OffsetDateTime.now().plusYears(1));

CreateConsentResponse enduringResponse = client.getEnduringConsentsApi()
        .createEnduringConsent(enduringRequest);

// Wait for authorization (automatically revokes on timeout for security)
UUID consentId = enduringResponse.getConsentId();
Consent authorisedConsent = client.awaitAuthorisedEnduringConsentOrThrowException(consentId, 300);
```

### Payments

```java
// Create payment from consent
PaymentRequest paymentRequest = new PaymentRequest()
        .consentId(consentId)
        .enduringPayment(new EnduringPaymentRequest()
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("25.00"))
                .pcr(new Pcr()
                        .particulars("particulars")
                        .code("code")
                        .reference("reference")));

PaymentResponse paymentResponse = client.getPaymentsApi()
        .createPayment(paymentRequest);

// Get payment
Payment payment = client.getPaymentsApi()
        .getPayment(paymentResponse.getPaymentId());
```

### Quick Payments

```java
// Create quick payment (combines consent + payment)
QuickPaymentRequest quickRequest = new QuickPaymentRequest()
        .flow(new AuthFlow()
                .detail(new GatewayFlow()
                        .redirectUri("https://www.example.com/return")))
        .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("15.00"))
        .pcr(new Pcr()
                .particulars("particulars")
                .code("code")
                .reference("reference"));

CreateQuickPaymentResponse quickResponse = client.getQuickPaymentsApi()
        .createQuickPayment(quickRequest);

// Get quick payment
QuickPaymentResponse quick = client.getQuickPaymentsApi()
        .getQuickPayment(quickResponse.getQuickPaymentId());

// Revoke quick payment
client.getQuickPaymentsApi()
        .revokeQuickPayment(quickResponse.getQuickPaymentId());
```

### Refunds

```java
// Create full refund
RefundDetail refundRequest = new RefundDetail()
        .paymentId(paymentId)
        .type(RefundDetail.TypeEnum.FULL_REFUND)
        .refundRequest(new FullRefundRequest());

RefundResponse refundResponse = client.getRefundsApi()
        .createRefund(refundRequest);

// Create partial refund
RefundDetail partialRefund = new RefundDetail()
        .paymentId(paymentId)
        .type(RefundDetail.TypeEnum.PARTIAL_REFUND)
        .refundRequest(new PartialRefundRequest()
                .amount(new Amount()
                        .currency(Amount.CurrencyEnum.NZD)
                        .total("5.00"))
                .pcr(new Pcr()
                        .particulars("refund")
                        .code("code")
                        .reference("reference")));

// Get refund
Refund refund = client.getRefundsApi()
        .getRefund(refundResponse.getRefundId());
```

### Bank Metadata

```java
// Get supported banks and features
List<BankMetadata> banks = client.getMetaApi().getMeta();

for (BankMetadata bank : banks) {
    System.out.println("Bank: " + bank.getName());
    System.out.println("Features: " + bank.getFeatures());
}
```

## Polling and Timeout Behavior

The SDK provides helper methods to wait for consent authorization and payment completion:

### Auto-Revoke on Timeout

| Method | Auto-Revokes on Timeout? | Reason |
|--------|-------------------------|--------|
| `awaitSuccessfulQuickPaymentOrThrowException` | ✅ **YES** | Quick payments combine consent + payment - should complete immediately or be cancelled |
| `awaitAuthorisedSingleConsentOrThrowException` | ❌ **NO** | Single consents require separate payment step - no funds processed if abandoned |
| `awaitAuthorisedEnduringConsentOrThrowException` | ✅ **YES** | Enduring consents grant ongoing access - clean up if abandoned for security |
| `awaitSuccessfulPaymentOrThrowException` | ❌ N/A | Payments cannot be revoked once initiated |

**Best Practices**:
- Manually revoke single or enduring consents if you determine the customer has permanently abandoned the authorization flow (before timeout expires)
- Enduring consents will auto-revoke on timeout, but earlier manual revocation improves security

## Request IDs

Every API method has an overload accepting a custom request ID for tracing:

```java
String requestId = UUID.randomUUID().toString();
Consent consent = client.getSingleConsentsApi()
        .getConsent(consentId, requestId);
```

If not provided, a UUID is automatically generated.

## Error Handling

```java
try {
    Consent consent = client.getSingleConsentsApi().getConsent(consentId);
} catch (BlinkServiceException e) {
    // API error (HTTP 4xx/5xx)
    System.err.println("API error: " + e.getMessage());
} catch (BlinkInvalidValueException e) {
    // Invalid input parameter
    System.err.println("Invalid input: " + e.getMessage());
}
```

## Resource Management

The client implements `AutoCloseable` for proper resource cleanup:

```java
// Automatic cleanup with try-with-resources
try (BlinkDebitClient client = new BlinkDebitClient()) {
    // Use client
}

// Manual cleanup
BlinkDebitClient client = new BlinkDebitClient();
try {
    // Use client
} finally {
    client.close();
}
```

## Payment Settlement

**Important**: Payment settlement is asynchronous. Payments transition through these states:

- `Pending` - Payment initiated, not yet settled
- `AcceptedSettlementInProcess` - Settlement in progress
- `AcceptedSettlementCompleted` - ✅ **ONLY THIS STATUS means money has been sent from the payer's bank**
- `Rejected` - Payment failed

**Only `AcceptedSettlementCompleted` confirms funds have been sent from the payer's bank.** In rare cases, payments may remain in `AcceptedSettlementInProcess` for extended periods.

### Polling for Settlement

The SDK provides helper methods to wait for payment completion:

```java
// Wait up to 5 minutes for funds to be sent from payer's bank
try {
    Payment payment = client.awaitSuccessfulPayment(paymentId, 300);
    System.out.println("Funds sent from payer's bank: " + payment.getStatus());
} catch (BlinkPaymentTimeoutException e) {
    // Settlement didn't complete within timeout
    System.err.println("Payment still processing after 5 minutes");
}
```

**Note**: The polling helpers will timeout if the wait period is shorter than the actual settlement time. Always set appropriate timeout values based on your requirements.

## Testing

```bash
# Run unit tests only
mvn test

# Run integration tests (requires environment variables)
export BLINKPAY_DEBIT_URL="https://staging.debit.blinkpay.co.nz"
export BLINKPAY_CLIENT_ID="your-client-id"
export BLINKPAY_CLIENT_SECRET="your-client-secret"
mvn verify

# Skip tests
mvn package -DskipTests
```

## Dependencies

Runtime dependencies:
- `jackson-databind` (2.20.1) - JSON serialization
- `jackson-datatype-jsr310` (2.20.1) - Java 8 date/time support
- `java-jwt` (4.5.0) - JWT token handling
- `slf4j-api` (2.0.17) - Logging facade

Optional dependencies:
- `validation-api` (2.0.1.Final) - Bean validation annotations
- `javax.annotation-api` (1.3.2) - @Generated annotations

## Contributing

We welcome contributions from the community! Your pull requests will be reviewed by our team.

To contribute:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`mvn verify`)
6. Submit a pull request

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed guidelines.

## Support

- **Documentation**: See [CLAUDE.md](./CLAUDE.md) for technical details
- **Issues**: [GitHub Issues](https://github.com/BlinkPay/Blink-Payments-API-Client-Java/issues)
- **Contact**: sysadmin@blinkpay.co.nz

## License

This project is licensed under the MIT License.
