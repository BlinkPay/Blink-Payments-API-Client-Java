![BlinkPay](https://raw.githubusercontent.com/BlinkPay/Blink-Payments-API-Client-Java/master/.github/assets/banner.png)

# Blink Payments API Client for Java
[![CI](https://github.com/BlinkPay/Blink-Payments-API-Client-Java/actions/workflows/maven-build.yml/badge.svg)](https://github.com/BlinkPay/Blink-Payments-API-Client-Java/actions/workflows/maven-build.yml)

---
## blink-debit-api-client-java
[![Maven Central](https://img.shields.io/maven-central/v/nz.co.blinkpay/blink-debit-api-client-java.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/nz.co.blinkpay/blink-debit-api-client-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=blink-debit-api-client-java&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=blink-debit-api-client-java)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=blink-debit-api-client-java&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=blink-debit-api-client-java)
[![Snyk security](https://snyk.io/test/github/BlinkPay/Blink-Payments-API-Client-Java/badge.svg)](https://security.snyk.io/package/maven/nz.co.blinkpay:blink-debit-api-client-java)

---
## blink-debit-api-client-java-spring6
[![Maven Central](https://img.shields.io/maven-central/v/nz.co.blinkpay/blink-debit-api-client-java-spring6.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/nz.co.blinkpay/blink-debit-api-client-java-spring6)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=blink-debit-api-client-java-spring6&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=blink-debit-api-client-java-spring6)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=blink-debit-api-client-java-spring6&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=blink-debit-api-client-java-spring6)
[![Snyk security](https://snyk.io/test/github/BlinkPay/Blink-Payments-API-Client-Java/badge.svg)](https://security.snyk.io/package/maven/nz.co.blinkpay:blink-debit-api-client-java-spring6)

# Table of Contents
1. [Introduction](#introduction)
2. [Minimum Requirements](#minimum-requirements)
3. [Adding the Dependency](#adding-the-dependency)
4. [Quick Start](#quick-start)
5. [Configuration](#configuration)
6. [Client Creation](#client-creation)
7. [Error Handling](#error-handling)
8. [Resource Management](#resource-management)
9. [Polling and Timeout Behavior](#polling-and-timeout-behavior)
10. [Correlation ID / Request ID](#correlation-id--request-id)
11. [Full Examples](#full-examples)
12. [Individual API Call Examples](#individual-api-call-examples)
13. [Handling Payment Settlement](#handling-payment-settlement)
14. [Dependencies](#dependencies)
15. [Support](#support)
16. [Contributing](#contributing)
17. [Running Tests](#running-tests)

## Introduction
This SDK allows merchants with Java-based e-commerce sites to integrate with **Blink PayNow** (for one-off payments) and **Blink AutoPay** (for recurring payments).

### SDK Versions
This repository provides two SDK implementations:

- **Plain Java SDK** (`java-v2/` module - **Recommended**): Lightweight synchronous SDK using Java 11+ HttpClient with minimal dependencies, ideal for plain Java applications, serverless functions, and memory-constrained environments.
- **Spring SDK** (`java-spring6/` module): Reactive SDK using Spring WebClient with Mono/Flux async programming. Suitable for Spring Boot 3.x / Spring Framework 6+ applications.

The Plain Java SDK uses the standard Java 11+ HttpClient for synchronous blocking calls with minimal dependencies. The Spring SDK internally uses WebClient for making reactive API calls.

## Contributing
We welcome contributions from the community. Your pull request will be reviewed by our team.

This project is licensed under the MIT License.

## Minimum Requirements
- Maven 3 or Gradle 7
- Java 11 or higher (for Plain Java SDK - `blink-debit-api-client-java`)
- Java 21 or higher (for Spring SDK - `blink-debit-api-client-java-spring6`)
- Lombok 1.18 (for Spring SDK development only)

## Adding the dependency

This SDK is available in two versions to support different Java and Spring Framework configurations:

### For Plain Java 11+ Applications (Recommended)
Use `blink-debit-api-client-java` for:
- **Plain Java 11+** applications (non-Spring)
- **Serverless functions** (Lambda, Cloud Functions, etc.)
- **Memory-constrained environments**
- Applications that prefer **synchronous blocking API** calls

This lightweight version has minimal runtime dependencies.

#### Maven
```xml
<dependency>
    <groupId>nz.co.blinkpay</groupId>
    <artifactId>blink-debit-api-client-java</artifactId>
    <version>${version}</version>
</dependency>
```

#### Gradle
```groovy
implementation "nz.co.blinkpay:blink-debit-api-client-java:$version"
```

### For Spring Boot 3.x / Spring Framework 6+ Applications
Use `blink-debit-api-client-java-spring6` for:
- **Spring Boot 3.x+** applications
- **Spring Framework 6.x+** applications
- Applications that prefer **reactive programming** with Mono/Flux

**Important:** This SDK uses `provided` scope for Spring dependencies - you must provide compatible Spring Framework dependencies in your application. The SDK is compatible with Spring Framework 6.x+ (Spring Boot 3.x+) and has been tested with Spring Framework 6.2.12 (Spring Boot 3.5.7). Future Spring versions should work as long as they maintain API compatibility.

#### Maven
```xml
<!-- Blink Debit API Client for Spring -->
<dependency>
    <groupId>nz.co.blinkpay</groupId>
    <artifactId>blink-debit-api-client-java-spring6</artifactId>
    <version>${version}</version>
</dependency>

<!-- Required: Your Spring Boot dependencies (3.0.0 or higher) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <version>3.5.7</version> <!-- Or any Spring Boot 3.x version -->
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
    <version>3.5.7</version>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    <version>3.3.0</version> <!-- Or compatible version -->
</dependency>
```

#### Gradle
```groovy
// Blink Debit API Client for Spring
implementation "nz.co.blinkpay:blink-debit-api-client-java-spring6:$version"

// Required: Your Spring Boot dependencies (3.0.0 or higher)
implementation "org.springframework.boot:spring-boot-starter-webflux:3.5.7"
implementation "org.springframework.boot:spring-boot-starter-validation:3.5.7"
implementation "org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j:3.3.0"
```

**Spring Version Compatibility:**
- **Tested with**: Spring Framework 6.2.12 (Spring Boot 3.5.7)
- **Compatible with**: Spring Framework 6.x+ and future releases (Spring Boot 3.x+ and future releases)
- **Minimum suggested**: Spring Framework 6.0.0+ (Spring Boot 3.0.0+ if using Boot)

The SDK uses `provided` scope and standard Spring APIs, so it should work with any Spring Framework 6.x+ version (or Spring Boot 3.x+). You have flexibility to upgrade Spring independently of the SDK.

## Quick Start

### Plain Java SDK (Recommended)
```java
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.config.BlinkDebitConfig;

// Option 1: Using environment variables (BLINKPAY_DEBIT_URL, BLINKPAY_CLIENT_ID, BLINKPAY_CLIENT_SECRET)
BlinkDebitClient client = new BlinkDebitClient();

// Option 2: Using configuration builder
BlinkDebitConfig config = BlinkDebitConfig.builder()
        .debitUrl("https://sandbox.debit.blinkpay.co.nz")
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .build();
BlinkDebitClient client = new BlinkDebitClient(config);

// Option 3: Using simple constructor
BlinkDebitClient client = new BlinkDebitClient(
        "https://sandbox.debit.blinkpay.co.nz",
        "your-client-id",
        "your-client-secret");

// Create a quick payment (synchronous blocking call)
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

// Create the quick payment
CreateQuickPaymentResponse response = client.createQuickPayment(request);

// Redirect the consumer to response.getRedirectUri()

// Wait for payment completion (optional)
UUID quickPaymentId = response.getQuickPaymentId();
QuickPaymentResponse qpResponse = client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 300);
```

### Spring SDK (Reactive)
```java
String blinkpayUrl = "https://sandbox.debit.blinkpay.co.nz";
String clientId = "...";
String clientSecret = "...";
String profile = "local";
BlinkDebitClient client = new BlinkDebitClient(blinkpayUrl, clientId, clientSecret, profile);

QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
                .detail(new GatewayFlow()
                        .redirectUri("https://www.blinkpay.co.nz/sample-merchant-return-page")))
        .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("0.01"))
        .pcr(new Pcr()
                .particulars("particulars")
                .code("code")
                .reference("reference"));

CreateQuickPaymentResponse qpCreateResponse = client.createQuickPayment(request);
logger.info("Redirect URL: {}", qpCreateResponse.getRedirectUri()); // Redirect the consumer to this URL
UUID qpId = qpCreateResponse.getQuickPaymentId();
QuickPaymentResponse qpResponse = client.awaitSuccessfulQuickPaymentOrThrowException(qpId, 300); // Will throw an exception if the payment was not successful after 5min
```

## Configuration
Configuration differs between SDK versions:

### Plain Java SDK Configuration
The Plain Java SDK uses **simple configuration** via:
1. Environment variables (recommended): `BLINKPAY_DEBIT_URL`, `BLINKPAY_CLIENT_ID`, `BLINKPAY_CLIENT_SECRET`, `BLINKPAY_TIMEOUT` (optional, ISO-8601 duration like "PT30S")
2. Configuration builder: `BlinkDebitConfig.builder()...build()`
3. Simple constructor: `new BlinkDebitClient(url, clientId, clientSecret)`

No properties files required. See [Quick Start](#quick-start) for examples.

### Spring SDK Configuration
The Spring SDK uses **Spring-based configuration** from:
- `blinkdebit.yaml` or `blinkdebit.properties` files in classpath
- Environment variables
- System properties

See detailed configuration examples below.

### Common Configuration Values
- The BlinkPay **Sandbox** debit URL is `https://sandbox.debit.blinkpay.co.nz` and the **production** debit URL is `https://debit.blinkpay.co.nz`.
- The client credentials will be provided to you by BlinkPay as part of your on-boarding process.
> **Warning** Take care not to check in your client ID and secret to your source control.

### Spring SDK Property Precedence
For Spring SDK, properties are loaded in this order:
1. As provided directly to client constructor
2. Environment variables e.g. `export BLINKPAY_CLIENT_SECRET=...`
3. System properties e.g. `-Dblinkpay.client.secret=...`
4. `blinkdebit.properties`
5. `blinkdebit.yaml`
6. Default values

### Spring SDK Property Configuration Examples

#### Environment variables
```shell
export BLINKPAY_DEBIT_URL=<BLINKPAY_DEBIT_URL>
export BLINKPAY_CLIENT_ID=<BLINKPAY_CLIENT_ID>
export BLINKPAY_CLIENT_SECRET=<BLINKPAY_CLIENT_SECRET>
# for non-Spring consumer as an alternative to spring.profiles.active property. Debugging profiles are local, dev or test. Any other value will behave in a production-like manner.
export BLINKPAY_ACTIVE_PROFILE=test

# Optional configuration values below
export BLINKPAY_MAX_CONNECTIONS=10
export BLINKPAY_MAX_IDLE_TIME=PT20S
export BLINKPAY_MAX_LIFE_TIME=PT60S
export BLINKPAY_PENDING_ACQUIRE_timeout=PT10S
export BLINKPAY_EVICTION_INTERVAL=PT60S
export BLINKPAY_RETRY_ENABLED=true
```

### Properties file 
Substitute the correct values to your `blinkdebit.properties` file.
```properties
blinkpay.debit.url=<BLINKPAY_DEBIT_URL>
blinkpay.client.id=<BLINKPAY_CLIENT_ID>
blinkpay.client.secret=<BLINKPAY_CLIENT_SECRET>
# for non-Spring consumer as an alternative to spring.profiles.active property. Debugging profiles are local, dev or test. Any other value will behave in a production-like manner.
blinkpay.active.profile=test

# Optional configuration values below
blinkpay.max.connections=10
blinkpay.max.idle.time=PT20S
blinkpay.max.life.time=PT60S
blinkpay.pending.acquire.timeout=PT10S
blinkpay.eviction.interval=PT60S
blinkpay.retry.enabled=true
```

#### Properties file - Spring
The property placeholders below will **only work for Spring consumers** by substituting the corresponding environment variables.
```properties
blinkpay.debit.url=${BLINKPAY_DEBIT_URL}
blinkpay.client.id=${BLINKPAY_CLIENT_ID}
blinkpay.client.secret=${BLINKPAY_CLIENT_SECRET}
# for non-Spring consumer as an alternative to spring.profiles.active property. Debugging profiles are local, dev or test. Any other value will behave in a production-like manner.
blinkpay.active.profile=${BLINKPAY_ACTIVE_PROFILE:test}

# Optional configuration values below
blinkpay.max.connections=${BLINKPAY_MAX_CONNECTIONS:10}
blinkpay.max.idle.time=${BLINKPAY_MAX_IDLE_TIME:PT20S}
blinkpay.max.life.time=${BLINKPAY_MAX_LIFE_TIME:PT60S}
blinkpay.pending.acquire.timeout=${BLINKPAY_PENDING_ACQUIRE_TIMEOUT:PT10S}
blinkpay.eviction.interval=${BLINKPAY_EVICTION_INTERVAL:PT60S}
blinkpay.retry.enabled=${BLINKPAY_RETRY_ENABLED:true}
```

#### YAML properties file - Spring
The property placeholders below will **only work for Spring consumers** by substituting the corresponding environment variables.
```yaml
blinkpay:
  debit:
    url: ${BLINKPAY_DEBIT_URL}
  client:
    id: ${BLINKPAY_CLIENT_ID}
    secret: ${BLINKPAY_CLIENT_SECRET}
  
  # Optional configuration values below
  max:
    connections: ${BLINKPAY_MAX_CONNECTIONS:10}
    idle:
      time: ${BLINKPAY_MAX_IDLE_TIME:PT20S}
    life:
      time: ${BLINKPAY_MAX_LIFE_TIME:PT60S}
  pending:
    acquire:
      timeout: ${BLINKPAY_PENDING_ACQUIRE_TIMEOUT:PT10S}
  eviction:
    interval: ${BLINKPAY_EVICTION_INTERVAL:PT60S}
  retry:
    enabled: ${BLINKPAY_RETRY_ENABLED:true}
```

## Client Creation

### Plain Java SDK Client Creation
The Plain Java SDK provides multiple initialization options:

```java
// Option 1: Using environment variables (BLINKPAY_DEBIT_URL, BLINKPAY_CLIENT_ID, BLINKPAY_CLIENT_SECRET)
BlinkDebitClient client = new BlinkDebitClient();

// Option 2: Using configuration builder
BlinkDebitConfig config = BlinkDebitConfig.builder()
        .debitUrl("https://sandbox.debit.blinkpay.co.nz")
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .timeout(Duration.ofSeconds(30))  // Optional, defaults to 10 seconds
        .build();
BlinkDebitClient client = new BlinkDebitClient(config);

// Option 3: Simple constructor
BlinkDebitClient client = new BlinkDebitClient(
        "https://sandbox.debit.blinkpay.co.nz",
        "your-client-id",
        "your-client-secret");

// Always close the client when done (or use try-with-resources)
client.close();
```

The Plain Java SDK client implements `AutoCloseable`, so you can use try-with-resources:
```java
try (BlinkDebitClient client = new BlinkDebitClient()) {
    // Use client...
} // Automatically closed
```

### Spring SDK Client Creation
Plain Java client code:
```java
// No-arg constructor uses property hierarchy
BlinkDebitClient client = new BlinkDebitClient();

// Or supply properties directly
BlinkDebitClient client = new BlinkDebitClient(blinkpayUrl, clientId, clientSecret, "production");
```

Spring-based code (autowiring):
```java
@Autowired
BlinkDebitClient client;
```

## Error Handling

### Plain Java SDK

```java
try {
    Consent consent = client.getSingleConsentsApi().getConsent(consentId);
    // Process consent
} catch (BlinkServiceException e) {
    // API error (HTTP 4xx/5xx)
    System.err.println("API error: " + e.getMessage());
} catch (BlinkInvalidValueException e) {
    // Invalid input parameter
    System.err.println("Invalid input: " + e.getMessage());
}
```

### Spring SDK

```java
client.getConsent(consentId)
    .doOnError(BlinkServiceException.class, e ->
        System.err.println("API error: " + e.getMessage()))
    .doOnError(BlinkInvalidValueException.class, e ->
        System.err.println("Invalid input: " + e.getMessage()))
    .subscribe();
```

## Resource Management

### Plain Java SDK

The Plain Java SDK client implements `AutoCloseable` for proper resource cleanup:

```java
// Automatic cleanup with try-with-resources (recommended)
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

### Spring SDK

The Spring SDK uses WebClient which manages its own resources. No explicit cleanup needed when using dependency injection or when the client is managed by Spring's application context.

## Polling and Timeout Behavior

Both SDKs provide helper methods to wait for consent authorization and payment completion. These methods poll the API until the resource reaches a terminal state or the timeout expires.

### Auto-Revoke on Timeout

**Important**: Different consent types have different timeout behaviors for security and usability reasons:

| Method | Auto-Revokes on Timeout? | Reason |
|--------|-------------------------|--------|
| Quick Payment helpers | ✅ **YES** | Quick payments combine consent + payment in one step - should complete immediately or be cancelled to prevent abandoned authorizations |
| Single Consent helpers | ❌ **NO** | Single consents require a separate payment API call - no funds are processed if abandoned, so the consent can remain for the customer to return later |
| Enduring Consent helpers | ✅ **YES** | Enduring consents grant ongoing recurring access to customer accounts - automatically cleaned up if abandoned for security |
| Payment helpers | ❌ N/A | Payments cannot be revoked once initiated |

### Plain Java SDK Examples

```java
// Quick payment - auto-revokes on timeout
QuickPaymentResponse qp = client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 300);

// Single consent - does NOT auto-revoke (manually revoke if needed)
Consent singleConsent = client.awaitAuthorisedSingleConsentOrThrowException(consentId, 300);

// Enduring consent - auto-revokes on timeout
Consent enduringConsent = client.awaitAuthorisedEnduringConsentOrThrowException(consentId, 300);

// Payment - waits for settlement
Payment payment = client.awaitSuccessfulPaymentOrThrowException(paymentId, 300);
```

### Spring SDK Examples

```java
// Similar methods returning Mono<T>
Mono<QuickPaymentResponse> qp = client.awaitSuccessfulQuickPaymentOrThrowException(quickPaymentId, 300);
Mono<Consent> consent = client.awaitAuthorisedSingleConsentOrThrowException(consentId, 300);
```

### Best Practices

- **Quick Payments**: Use polling helpers - they handle cleanup automatically
- **Single Consents**: Manually revoke if you determine the customer has permanently abandoned the authorization flow (e.g., after 24 hours)
- **Enduring Consents**: Polling helpers automatically revoke on timeout, but consider manual revocation earlier if customer abandons the flow for improved security
- **Payments**: Set appropriate timeout values based on expected settlement times (usually a few minutes)

## Correlation ID / Request ID
An optional request ID can be added as the last argument to API calls. This serves as:
- **Correlation ID** for tracing requests across systems
- **Idempotency key** for Blink API calls

It will be generated automatically (UUID) if not provided.

### Plain Java SDK Example
```java
// Auto-generated request ID
CreateQuickPaymentResponse response = client.createQuickPayment(request);

// Custom request ID
String requestId = "my-custom-id-123";
CreateQuickPaymentResponse response = client.createQuickPayment(request, requestId);
```

### Spring SDK Example
```java
Mono<CreateQuickPaymentResponse> response = client.createQuickPayment(request, "my-custom-id-123");
```

## Full Examples

> **Note:** The examples below work with **both Plain Java SDK and Spring SDK**.
> - **Plain Java SDK**: Returns `T` directly (synchronous blocking)
> - **Spring SDK**: Returns `Mono<T>` (reactive, requires `.block()` or subscription)
### Quick payment (one-off payment), using Gateway flow
A quick payment is a one-off payment that combines the API calls needed for both the consent and the payment.
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
                .detail(new GatewayFlow()
                        .redirectUri("https://www.blinkpay.co.nz/sample-merchant-return-page")))
        .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("0.01"))
        .pcr(new Pcr()
                .particulars("particulars")
                .code("code")
                .reference("reference"));

CreateQuickPaymentResponse qpCreateResponse = client.createQuickPayment(request);
logger.info("Redirect URL: {}", qpCreateResponse.getRedirectUri()); // Redirect the consumer to this URL
UUID qpId = qpCreateResponse.getQuickPaymentId();
QuickPaymentResponse qpResponse = client.awaitSuccessfulQuickPaymentOrThrowException(qpId, 300); // Will throw an exception if the payment was not successful after 5min
```

### Single consent followed by one-off payment, using Gateway flow
```java
SingleConsentRequest consent = new SingleConsentRequest()
        .flow(new AuthFlow()
                .detail(new GatewayFlow()
                        .redirectUri("https://www.blinkpay.co.nz/sample-merchant-return-page")
                        .flowHint(new RedirectFlowHint()
                                .bank(Bank.BNZ)))) // Optional, bank will be preselected
        .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total("0.01")) 
        .pcr(new Pcr()
                .particulars("particulars"));
CreateConsentResponse createConsentResponse = client.createSingleConsent(consent);
String redirectUri = createConsentResponse.getRedirectUri(); // Redirect the consumer to this URL
PaymentRequest payment = new PaymentRequest().consentId(createConsentResponse.getConsentId());
PaymentResponse paymentResponse = client.createPayment(payment);
logger.info("Payment Status: {}", client.getPayment(paymentResponse.getPaymentId()).getStatus());
// TODO inspect the payment result status
```

## Individual API Call Examples

> **Note:** All examples below work with **both Plain Java SDK and Spring SDK**.
> - **Plain Java SDK**: All methods return values directly (synchronous)
> - **Spring SDK**: All methods return `Mono<T>` (reactive, call `.block()` to get value)

### Bank Metadata
Supplies the supported banks and supported flows on your account.
```java
List<BankMetadata> bankMetadataList = client.getMeta();
```

### Quick Payments
#### Gateway Flow
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateQuickPaymentResponse createQuickPaymentResponse = client.createQuickPayment(request);
```
#### Gateway Flow - Redirect Flow Hint
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new RedirectFlowHint()
                    .bank(bank))))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateQuickPaymentResponse createQuickPaymentResponse = client.createQuickPayment(request);
```
#### Gateway Flow - Decoupled Flow Hint
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new DecoupledFlowHint()
                    .identifierType(identifierType)
                    .identifierValue(identifierValue)
                    .bank(bank))))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateQuickPaymentResponse createQuickPaymentResponse = client.createQuickPayment(request);
```
#### Redirect Flow
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
            .detail(new RedirectFlow()
                .bank(bank)
                .redirectUri(redirectUri)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateQuickPaymentResponse createQuickPaymentResponse = client.createQuickPayment(request);
```
#### Decoupled Flow
```java
QuickPaymentRequest request = (QuickPaymentRequest) new QuickPaymentRequest()
        .flow(new AuthFlow()
            .detail(new RedirectFlow()
                .bank(bank)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .callbackUrl(callbackUrl)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateQuickPaymentResponse createQuickPaymentResponse = client.createQuickPayment(request);
```
#### Retrieval
```java
QuickPaymentResponse quickPaymentResponse = client.getQuickPayment(quickPaymentId);
```
#### Revocation
```java
client.revokeQuickPayment(quickPaymentId);
```

### Single/One-Off Consents
#### Gateway Flow
```java
SingleConsentRequest request = new SingleConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateConsentResponse createConsentResponse = client.createSingleConsent(request);
```
#### Gateway Flow - Redirect Flow Hint
```java
SingleConsentRequest request = new SingleConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new RedirectFlowHint()
                    .bank(bank))))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateConsentResponse createConsentResponse = client.createSingleConsent(request);
```
#### Gateway Flow - Decoupled Flow Hint
```java
SingleConsentRequest request = new SingleConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new DecoupledFlowHint()
                    .identifierType(identifierType)
                    .identifierValue(identifierValue)
                    .bank(bank))))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateConsentResponse createConsentResponse = client.createSingleConsent(request);
```
#### Redirect Flow
Suitable for most consents.
```java
SingleConsentRequest request = new SingleConsentRequest()
        .flow(new AuthFlow()
            .detail(new RedirectFlow()
                .bank(bank)
                .redirectUri(redirectUri)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateConsentResponse createConsentResponse = client.createSingleConsent(request);
```
#### Decoupled Flow
This flow type allows better support for mobile by allowing the supply of a mobile number or previous consent ID to identify the customer with their bank.

The customer will receive the consent request directly to their online banking app. This flow does not send the user through a web redirect flow.
```java
SingleConsentRequest request = new SingleConsentRequest()
        .flow(new AuthFlow()
            .detail(new DecoupledFlow()
                .bank(bank)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .callbackUrl(callbackUrl)))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference));

CreateConsentResponse createConsentResponse = client.createSingleConsent(request);
```
#### Retrieval
Get the consent including its status
```java
Consent consent = client.getSingleConsent(consentId);
```
#### Revocation
```java
client.revokeSingleConsent(consentId);
```

### Blink AutoPay - Enduring/Recurring Consents
Request an ongoing authorisation from the customer to debit their account on a recurring basis.

Note that such an authorisation can be revoked by the customer in their mobile banking app.
#### Gateway Flow
```java
EnduringConsentRequest request = new EnduringConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)))
        .maximumAmountPeriod(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPeriod))
        .maximumAmountPayment(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total(totalPerPayment))
        .period(period)
        .fromTimestamp(startDate)
        .expiryTimestamp(endDate);

CreateConsentResponse createConsentResponse = client.createEnduringConsent(request);
```
#### Gateway Flow - Redirect Flow Hint
```java
EnduringConsentRequest request = new EnduringConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new RedirectFlowHint()
                    .bank(bank))))
        .maximumAmountPeriod(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPeriod))
        .maximumAmountPayment(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPayment))
        .period(period)
        .fromTimestamp(startDate)
        .expiryTimestamp(endDate);

CreateConsentResponse createConsentResponse = client.createEnduringConsent(request);
```
#### Gateway Flow - Decoupled Flow Hint
```java
EnduringConsentRequest request = new EnduringConsentRequest()
        .flow(new AuthFlow()
            .detail(new GatewayFlow()
                .redirectUri(redirectUri)
                .flowHint(new DecoupledFlowHint()
                    .identifierType(identifierType)
                    .identifierValue(identifierValue)
                    .bank(bank))))
        .maximumAmountPeriod(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPeriod))
        .maximumAmountPayment(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPayment))
        .period(period)
        .fromTimestamp(startDate)
        .expiryTimestamp(endDate);

CreateConsentResponse createConsentResponse = client.createEnduringConsent(request);
```
#### Redirect Flow
```java
EnduringConsentRequest request = new EnduringConsentRequest()
        .flow(new AuthFlow()
            .detail(new RedirectFlow()
                .bank(bank)
                .redirectUri(redirectUri)))
        .maximumAmountPeriod(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPeriod))
        .maximumAmountPayment(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPayment))
        .period(period)
        .fromTimestamp(startDate)
        .expiryTimestamp(endDate);

CreateConsentResponse createConsentResponse = client.createEnduringConsent(request);
```
#### Decoupled Flow
```java
EnduringConsentRequest request = new EnduringConsentRequest()
        .flow(new AuthFlow()
            .detail(new DecoupledFlow()
                .bank(bank)
                .identifierType(identifierType)
                .identifierValue(identifierValue)
                .callbackUrl(callbackUrl)))
        .maximumAmountPeriod(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPeriod))
        .maximumAmountPayment(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(totalPerPayment))
        .period(period)
        .fromTimestamp(startDate)
        .expiryTimestamp(endDate);

CreateConsentResponse createConsentResponse = client.createEnduringConsent(request);
```
#### Retrieval
```java
Consent consent = client.getEnduringConsent(consentId);
```
#### Revocation
```java
client.revokeEnduringConsent(consentId);
```

### Payments
The completion of a payment requires a consent to be in the Authorised status.
#### Single/One-Off
```java
PaymentRequest request = new PaymentRequest()
        .consentId(consentId);

PaymentResponse paymentResponse = client.createPayment(request);
```
#### Enduring/Recurring
If you already have an approved consent, you can run a Payment against that consent at the frequency as authorised in the consent.
```java
PaymentRequest request = new PaymentRequest()
        .consentId(consentId)
        .enduringPayment(new EnduringPaymentRequest()
            .amount(new Amount()
                .currency(Amount.CurrencyEnum.NZD)
                .total(total))
            .pcr(new Pcr()
                .particulars(particulars)
                .code(code)
                .reference(reference)));

PaymentResponse paymentResponse = client.createPayment(request);
```
#### Retrieval
```java
Payment payment = client.getPayment(paymentId);
```

### Refunds
#### Account Number Refund
```java
AccountNumberRefundRequest request = (AccountNumberRefundRequest) new AccountNumberRefundRequest()
        .paymentId(paymentId);

RefundResponse refundResponse = client.createRefund(request);
```
#### Full Refund (Not yet implemented)
```java
FullRefundRequest request = (FullRefundRequest) new FullRefundRequest()
        .consentRedirect(redirectUri)
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference))
        .paymentId(paymentId);

RefundResponse refundResponse = client.createRefund(request);
```
#### Partial Refund (Not yet implemented)
```java
PartialRefundRequest request = (PartialRefundRequest) new PartialRefundRequest()
        .consentRedirect(redirectUri)
        .pcr(new Pcr()
            .particulars(particulars)
            .code(code)
            .reference(reference))
        .amount(new Amount()
            .currency(Amount.CurrencyEnum.NZD)
            .total(total))
        .paymentId(paymentId);

RefundResponse refundResponse = client.createRefund(request);
```
#### Retrieval
```java
Refund refund = client.getRefund(refundId);
```

## Handling Payment Settlement

### Important: Asynchronous Payment Settlement

Payment settlement is asynchronous - payments transition from `AcceptedSettlementInProcess` to `AcceptedSettlementCompleted` as the bank processes them. Only `AcceptedSettlementCompleted` status confirms that funds have been successfully sent from the payer's bank to your account. In rare circumstances, payments may remain in `AcceptedSettlementInProcess` status for extended periods. The SDK's polling methods (`awaitSuccessfulPayment`) will timeout if you set a wait period shorter than the settlement time.

**Your application is responsible for:**

1. **Persisting payment state**: Save payment IDs and status to your database when timeouts occur
2. **Re-polling incomplete payments**: Implement a background process to periodically check payments still in `AcceptedSettlementInProcess` status
3. **User communication**: Display "Payment Pending" messages and update your order status accordingly
4. **Reaching terminal state**: Continue polling until payment reaches `AcceptedSettlementCompleted` or `Rejected`

**The SDK provides:**
- `getPayment(paymentId)` - Retrieve current payment status
- `getConsent(consentId)` - Quick payments return consent with embedded payment status
- Payment IDs in all create responses for tracking

**Example wash-up process:**
```java
// Periodically check pending payments in your database
Payment payment = client.getPayment(pendingPaymentId);
if (payment.getStatus() == Payment.StatusEnum.ACCEPTED_SETTLEMENT_COMPLETED) {
    // Mark order as paid in your database
}
```

## Dependencies

### Plain Java SDK Dependencies

The Plain Java SDK has minimal runtime dependencies:

**Runtime:**
- `jackson-databind` (2.20.1) - JSON serialization
- `jackson-datatype-jsr310` (2.20.1) - Java 8 date/time support
- `java-jwt` (4.5.0) - JWT token handling
- `slf4j-api` (2.0.17) - Logging facade

**Optional:**
- `validation-api` (2.0.1.Final) - Bean validation annotations
- `javax.annotation-api` (1.3.2) - @Generated annotations

### Spring SDK Dependencies

The Spring SDK uses `provided` scope for Spring dependencies - you must provide compatible Spring Framework dependencies in your application.

**Provided (required in your application):**
- Spring Boot 3.x / Spring Framework 6.x
- Spring WebFlux
- Reactor
- Netty

**Runtime:**
- `java-jwt` (4.5.0) - JWT token handling
- `swagger-annotations` (2.2.40) - API documentation
- `slf4j-api` (2.0.17) - Logging facade
- `commons-lang3` (3.19.0) - Utility functions

Use `mvn dependency:tree` in each module directory to view full dependency trees.

## Support

- **Documentation**: See [CLAUDE.md](./CLAUDE.md) for repository-level technical details, or module-specific CLAUDE.md files in `java-v2/` and `java-spring6/` directories
- **Issues**: [GitHub Issues](https://github.com/BlinkPay/Blink-Payments-API-Client-Java/issues)
- **Contact**: sysadmin@blinkpay.co.nz

## Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute to this project.

### Running Tests

The project includes unit and integration tests for both SDK versions.

#### Test Suite Overview

Both SDK versions include comprehensive test suites covering all API operations and flow types.

#### Running Spring SDK Tests

The Spring SDK uses `provided` scope for Spring dependencies, so tests will use the Spring dependencies declared in `test` scope (`spring-boot-starter-test`). No additional configuration is needed.

To run tests, you need to set the required environment variables:

```bash
export BLINKPAY_CLIENT_ID="your-client-id"
export BLINKPAY_CLIENT_SECRET="your-client-secret"
```

Unit tests live in `src/test`; integration tests live in `src/integrationTest` and are run by
failsafe at the `verify` phase. `-Dgpg.skip` avoids the artifact signing that `verify` would
otherwise trigger.

Then run the tests:

```bash
# Run unit tests only
mvn -B -ntp test

# Run integration tests (runs the unit tests first)
mvn -B -ntp verify -Dgpg.skip
```

Or combine the environment variables in a single command:

```bash
# Unit tests
BLINKPAY_CLIENT_ID="your-client-id" BLINKPAY_CLIENT_SECRET="your-client-secret" mvn -B -ntp test

# Integration tests
BLINKPAY_CLIENT_ID="your-client-id" BLINKPAY_CLIENT_SECRET="your-client-secret" mvn -B -ntp verify -Dgpg.skip
```

For the Spring SDK module specifically, navigate to the `java-spring6` directory first:

```bash
cd java-spring6
BLINKPAY_CLIENT_ID="your-client-id" BLINKPAY_CLIENT_SECRET="your-client-secret" mvn -B -ntp verify -Dgpg.skip
```

#### Running Plain Java SDK Tests

The Plain Java SDK integration tests require three environment variables:

```bash
export BLINKPAY_DEBIT_URL="https://sandbox.debit.blinkpay.co.nz"
export BLINKPAY_CLIENT_ID="your-client-id"
export BLINKPAY_CLIENT_SECRET="your-client-secret"
```

Run the tests:

```bash
# From project root - run v2 integration tests
cd java-v2
mvn verify -Dgpg.skip

# Or combine with environment variables
BLINKPAY_DEBIT_URL="https://sandbox.debit.blinkpay.co.nz" \
BLINKPAY_CLIENT_ID="your-client-id" \
BLINKPAY_CLIENT_SECRET="your-client-secret" \
mvn verify -Dgpg.skip
```