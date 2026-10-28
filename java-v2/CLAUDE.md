# Blink Debit API Client - Plain Java SDK - Technical Documentation

Technical guide for developers and AI assistants working with the Plain Java SDK codebase.

## Architecture Overview

The Plain Java SDK (java-v2 module) is a lightweight, synchronous Java client for the Blink Debit API. It uses Java 11+ HttpClient for HTTP operations and has minimal runtime dependencies (~206KB).

### Design Principles

1. **Minimal Dependencies**: Only Jackson, java-jwt, and slf4j-api
2. **Synchronous Blocking API**: Simple request-response pattern
3. **Thread-Safe**: All components safe for concurrent use
4. **Immutable Configuration**: Builder pattern with validation
5. **AutoCloseable**: Proper resource lifecycle management

## Project Structure

```
java-v2/
├── src/main/java/nz/co/blink/debit/
│   ├── client/v1/          # API client implementations
│   │   ├── BlinkDebitClient.java          # Main facade
│   │   ├── OAuthApiClient.java            # OAuth2 authentication
│   │   ├── SingleConsentsApiClient.java   # Single consent operations
│   │   ├── EnduringConsentsApiClient.java # Enduring consent operations
│   │   ├── QuickPaymentsApiClient.java    # Quick payment operations
│   │   ├── PaymentsApiClient.java         # Payment operations
│   │   ├── RefundsApiClient.java          # Refund operations
│   │   └── MetaApiClient.java             # Bank metadata operations
│   ├── config/             # Configuration
│   │   └── BlinkDebitConfig.java          # Immutable config with builder
│   ├── dto/v1/             # Data transfer objects (generated)
│   │   ├── ApiClient.java                 # Base API client class
│   │   ├── JSON.java                      # JSON serialization utilities
│   │   ├── AbstractOpenApiSchema.java     # Base for polymorphic DTOs
│   │   ├── AccessTokenRequest.java        # OAuth token request
│   │   ├── AccessTokenResponse.java       # OAuth token response
│   │   └── [48 generated DTOs]
│   ├── enums/              # Enumeration types
│   │   ├── Bank.java
│   │   ├── ConsentStatus.java
│   │   ├── PaymentStatus.java
│   │   └── [other enums]
│   ├── exception/          # Exception classes
│   │   ├── BlinkServiceException.java     # API/HTTP errors
│   │   └── BlinkInvalidValueException.java # Validation errors
│   ├── helpers/            # Helper utilities
│   │   └── HttpClientHelper.java          # HTTP operations
│   └── service/            # Business logic
│       └── AccessTokenManager.java        # Token lifecycle management
└── src/integrationTest/java/
    └── nz/co/blink/debit/client/v1/
        └── BlinkDebitClientIntegrationTest.java
```

## Core Components

### 1. BlinkDebitClient (Main Facade)

**Location**: `src/main/java/nz/co/blink/debit/client/v1/BlinkDebitClient.java`

Entry point for all API operations. Implements `AutoCloseable`.

**Responsibilities**:
- Creates and manages HttpClient instance
- Initializes ObjectMapper with JavaTimeModule
- Creates AccessTokenManager for OAuth2
- Instantiates all API clients
- Provides getter methods for API access

**Usage**:
```java
try (BlinkDebitClient client = new BlinkDebitClient(config)) {
    client.getSingleConsentsApi().createSingleConsent(request);
}
```

**Thread Safety**: Thread-safe for read operations. HttpClient and ObjectMapper are thread-safe.

### 2. BlinkDebitConfig (Configuration)

**Location**: `src/main/java/nz/co/blink/debit/config/BlinkDebitConfig.java`

Immutable configuration object with builder pattern.

**Fields**:
- `debitUrl` (required): Base API URL
- `clientId` (required): OAuth2 client ID
- `clientSecret` (required): OAuth2 client secret
- `timeout` (optional): Request timeout, default 10 seconds

**Environment Variables**:
- `BLINKPAY_DEBIT_URL`
- `BLINKPAY_CLIENT_ID`
- `BLINKPAY_CLIENT_SECRET`
- `BLINKPAY_TIMEOUT` (ISO-8601 duration, e.g., "PT30S")

**Validation**: Constructor validates all required fields are present.

### 3. AccessTokenManager (OAuth2 Token Management)

**Location**: `src/main/java/nz/co/blink/debit/service/AccessTokenManager.java`

Thread-safe OAuth2 access token manager with automatic refresh.

**Features**:
- Lazy initialization (token fetched on first use)
- Automatic refresh 60 seconds before expiry
- Thread-safe using double-checked locking
- Uses Auth0-JWT to decode token expiry

**Algorithm**:
```java
public String getAccessToken() {
    if (token expired or not exists) {
        synchronized {
            if (token expired or not exists) {  // Double-check
                fetch new token
                decode expiry using JWT
            }
        }
    }
    return token
}
```

### 4. OAuthApiClient (OAuth2 Client)

**Location**: `src/main/java/nz/co/blink/debit/client/v1/OAuthApiClient.java`

Implements OAuth2 client credentials flow.

**Method**:
```java
public AccessTokenResponse generateAccessToken()
```

**Grant Type**: `client_credentials`

**Endpoint**: `POST /oauth2/token`

### 5. HttpClientHelper (HTTP Operations)

**Location**: `src/main/java/nz/co/blink/debit/helpers/HttpClientHelper.java`

Centralized HTTP client for all API calls.

**Methods**:
- `post(path, body, responseType, requestId)`: POST with JSON body
- `get(path, responseType, requestId)`: GET request
- `delete(path, requestId)`: DELETE request
- `getList(path, elementType, requestId)`: GET returning List

**Features**:
- Automatic Bearer token injection
- JSON serialization/deserialization via Jackson
- Request ID header (`request-id`)
- Comprehensive error handling
- Configurable timeout from BlinkDebitConfig

**Error Handling**:
- HTTP 2xx: Success, deserialize response
- HTTP 4xx/5xx: Throw BlinkServiceException with status code and body
- IOException: Wrap in BlinkServiceException
- InterruptedException: Set thread interrupted flag, wrap in BlinkServiceException

### 6. API Clients Pattern

All API clients follow this pattern:

**Structure**:
```java
public class XxxApiClient {
    private static final String PATH = "/path";
    private final HttpClientHelper httpHelper;

    public XxxApiClient(HttpClientHelper httpHelper) {
        this.httpHelper = httpHelper;
    }

    // Method with auto-generated request ID
    public Response method(Request request) {
        return method(request, UUID.randomUUID().toString());
    }

    // Method with custom request ID
    public Response method(Request request, String requestId) {
        validateInput(request);
        return httpHelper.post(PATH, request, Response.class, requestId);
    }
}
```

**Validation**: Use `BlinkInvalidValueException` for null/invalid inputs.

**API Clients**:
- **SingleConsentsApiClient**: `/consents/single` - create, get, revoke
- **EnduringConsentsApiClient**: `/consents/enduring` - create, get, revoke
- **QuickPaymentsApiClient**: `/quick-payments` - create, get, revoke
- **PaymentsApiClient**: `/payments` - create, get
- **RefundsApiClient**: `/refunds` - create, get
- **MetaApiClient**: `/meta` - get bank metadata (returns List)

## Data Transfer Objects (DTOs)

### Generation

DTOs are based on the Blink Debit API OpenAPI specification but are **manually maintained** in this SDK.

**Important**: DTOs have auto-generated comments from the original OpenAPI spec, but they are not automatically regenerated. Changes to DTOs should be made carefully to maintain API compatibility.

### Manual Modifications

Only these DTOs should be manually edited:

#### 1. Polymorphic Parent Classes

Three parent classes require `TypeEnum` discriminator:

**AuthFlowDetail.java**:
```java
public abstract class AuthFlowDetail {
    public enum TypeEnum {
        GATEWAY, REDIRECT, DECOUPLED
    }
    public abstract TypeEnum getType();
    // ... rest of class
}
```

**ConsentDetail.java**:
```java
public abstract class ConsentDetail {
    public enum TypeEnum {
        SINGLE, ENDURING
    }
    public abstract TypeEnum getType();
    // ... rest of class
}
```

**RefundRequest.java**:
```java
public abstract class RefundRequest {
    public enum TypeEnum {
        PARTIAL_REFUND, FULL_REFUND
    }
    public abstract TypeEnum getType();
    // ... rest of class
}
```

Child classes implement `getType()` to return their discriminator value.

#### 2. OAuth DTOs

**AccessTokenRequest.java** and **AccessTokenResponse.java** are custom POJOs (not generated):
- Manual builder pattern
- No Lombok
- Standard getters/setters/equals/hashCode

#### 3. Import Additions

Some DTOs need `import nz.co.blink.debit.dto.ApiClient;` added for compilation.

#### 4. Optional `status_updated_timestamp`

On `Consent`, `Payment` and `Refund`, `statusUpdatedTimestamp` is `required = false` with
`@JsonInclude(NON_NULL)` on the getter and setter. The API omits the field until the status first
changes, and the generator's `USE_DEFAULTS` behaves like `ALWAYS` with `BlinkDebitClient`'s mapper, which
would write it back as an explicit `null`. Keep `NON_NULL` if these DTOs are regenerated.

### Base Classes

**ApiClient.java**: Base class providing shared utilities for generated DTOs.

**JSON.java**: Jackson-based JSON serialization/deserialization utilities.

**AbstractOpenApiSchema.java**: Base class for polymorphic DTOs. Has abstract `getSchemas()` method implemented by children.

## Exception Handling

### BlinkServiceException

**Location**: `src/main/java/nz/co/blink/debit/exception/BlinkServiceException.java`

Thrown for API call failures:
- HTTP 4xx/5xx responses
- Network/IO errors
- JSON serialization/deserialization errors

**Contains**: Error message with HTTP status code and response body when available.

### BlinkInvalidValueException

**Location**: `src/main/java/nz/co/blink/debit/exception/BlinkInvalidValueException.java`

Thrown for invalid input parameters:
- Null required parameters
- Invalid UUIDs
- Constraint violations

**Usage**: Validate inputs at API client entry points before making HTTP calls.

## Testing

### Test Structure

**Unit Tests**: `src/test/java` (not yet implemented)
- Fast, isolated tests with no external dependencies
- Focus on validation logic, configuration, error handling
- Run with: `mvn test`

**Integration Tests**: `src/integrationTest/java`
- Test against real Blink API (sandbox environment)
- Require environment variables for credentials
- Run with: `mvn verify`

### Integration Tests

**Coverage Summary**: **50 integration tests** across **8 test classes** (62.5% coverage compared to java-spring7 with 80 tests) *updated 2025-11-13*

**Phase 1 Improvements** ✅:
- Added OAuthApiClientIntegrationTest (1 test)
- Added gateway flow tests (3 tests)
- Added 404 Not Found error tests (5 tests)
- Added input validation tests (8 tests)
- Fixed retry logic exception handling (4 test classes)
- Fixed 404 error assertions for multiline messages (5 test classes)
- **Total**: +23 tests (27 → 50)

**Location**: `src/integrationTest/java/nz/co/blink/debit/client/v1/`

**Shared Constants**: `IntegrationTestConstants.java`
- `REDIRECT_URI` - Test merchant return page URL
- `CALLBACK_URL` - Callback URL for decoupled flows
- `CUSTOMER_HASH` - Test customer identifier hash
- `DEFAULT_BANK` - Bank.PNZ (test bank)
- `PHONE_NUMBER` - Test phone number for decoupled flow

**Test Classes**:

1. **BlinkDebitClientIntegrationTest** (3 tests)
   - Client initialization from environment variables
   - API client getter validation
   - Meta API access

2. **MetaApiClientIntegrationTest** (2 tests)
   - Get bank metadata
   - Get metadata with custom request ID
   - **Status**: ✅ Enhanced coverage (exceeds java-spring7)

3. **SingleConsentsApiClientIntegrationTest** (**9 tests** - *improved*)
   - Create single consent with redirect flow
   - Get consent by ID
   - Revoke consent
   - Create with gateway flow *(Phase 1)*
   - Get non-existent consent 404 error *(Phase 1)*
   - Create with null request validation *(Phase 1)*
   - Get with null ID validation *(Phase 1)*
   - Revoke with null ID validation *(Phase 1)*
   - Full lifecycle testing
   - **Status**: ✅ 64.3% coverage (redirect + gateway flow, error handling complete)

4. **EnduringConsentsApiClientIntegrationTest** (**6 tests** - *improved*)
   - Create enduring consent with redirect flow
   - Get consent by ID
   - Revoke consent
   - Create with gateway flow *(Phase 1)*
   - Get non-existent consent 404 error *(Phase 1)*
   - Full lifecycle testing
   - **Status**: ⚠️ 42.9% coverage (redirect + gateway flow, missing validation tests)

5. **QuickPaymentsApiClientIntegrationTest** (**9 tests** - *improved*)
   - Create quick payment with redirect flow
   - Get quick payment by ID
   - Revoke quick payment
   - Create with gateway flow *(Phase 1)*
   - Get non-existent quick payment 404 error *(Phase 1)*
   - Create with null request validation *(Phase 1)*
   - Get with null ID validation *(Phase 1)*
   - Revoke with null ID validation *(Phase 1)*
   - Full lifecycle testing
   - **Status**: ✅ 64.3% coverage (redirect + gateway flow, error handling complete)

6. **PaymentsApiClientIntegrationTest** (**8 tests** - *improved, exceeds java-spring7*)
   - Create payment for single consent (decoupled flow)
   - Get payment for single consent
   - Create payment for enduring consent (decoupled flow)
   - Get payment for enduring consent
   - Get non-existent payment 404 error *(Phase 1)*
   - Create with null request validation *(Phase 1)*
   - Get with null ID validation *(Phase 1)*
   - Retry logic for sandbox authorization (up to 10 attempts)
   - **Status**: ✅ 160% coverage (exceeds java-spring7)

7. **RefundsApiClientIntegrationTest** (**6 tests** - *improved*)
   - Create account number refund for single consent
   - Get refund for single consent
   - Create account number refund for enduring consent
   - Get refund for enduring consent
   - Get non-existent refund 404 error *(Phase 1)*
   - Retry logic for payment authorization
   - **Status**: ✅ 85.7% coverage (excellent coverage)

8. **OAuthApiClientIntegrationTest** (**1 test** - *Phase 1 new*)
   - Generate access token with full validation
   - Validate token type (Bearer)
   - Validate JWT format (starts with "ey")
   - Validate expiry (3600 seconds)
   - Validate scopes
   - **Status**: ✅ 100% coverage (complete parity with java-spring7)

**Test Patterns**:

**Sequential Execution**:
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XxxApiClientIntegrationTest {
    @Test
    @Order(1)
    void createResource() { /* ... */ }

    @Test
    @Order(2)
    void getResource() { /* ... */ }

    @Test
    @Order(3)
    void revokeResource() { /* ... */ }
}
```

**Credential Validation**:
```java
@BeforeAll
static void setUp() throws BlinkInvalidValueException {
    String debitUrl = System.getenv("BLINKPAY_DEBIT_URL");
    String clientId = System.getenv("BLINKPAY_CLIENT_ID");
    String clientSecret = System.getenv("BLINKPAY_CLIENT_SECRET");

    credentialsAvailable = debitUrl != null && clientId != null && clientSecret != null;

    if (credentialsAvailable) {
        client = new BlinkDebitClient(BlinkDebitConfig.builder()
                .debitUrl(debitUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build());
    }
}

@Test
void test() {
    assumeTrue(credentialsAvailable, "Integration test credentials not available");
    // Test executes only if credentials are available
}
```

**Retry Logic for Decoupled Flow** (Payments/Refunds):
```java
for (int i = 1; i <= 10; i++) {
    try {
        PaymentResponse response = client.getPaymentsApi().createPayment(request);
        paymentId = response.getPaymentId();
        break;  // Success
    } catch (RuntimeException e) {
        Thread.sleep(2000L * i);  // Exponential backoff: 2s, 4s, 6s, ..., 20s
    }
}
```

**Resource Cleanup**:
```java
@AfterAll
static void tearDown() {
    if (client != null) {
        client.close();  // Clean up HttpClient resources
    }
}
```

**Flow Type Usage**:
- **Redirect Flow**: Single/Enduring consents, Quick payments (most common for web)
- **Decoupled Flow**: Payments and Refunds (requires phone number authorization in sandbox)
- **Gateway Flow**: Single/Enduring consents, Quick payments *(Phase 1 complete)*

**Gap Analysis**: See `GAP_ANALYSIS.md` for:
- Detailed comparison with java-spring7 test coverage
- ✅ **Phase 1 Complete**: OAuth, Gateway flow, Error scenarios (13 tests)
- Missing test categories: Decoupled flow for consents, validation for enduring consents
- Prioritized recommendations for Phase 2
- Unit test needs (50-60 tests recommended)

**Configuration**:
- Uses JUnit Jupiter with `@Tag("integration")`
- Requires environment variables: `BLINKPAY_DEBIT_URL`, `BLINKPAY_CLIENT_ID`, `BLINKPAY_CLIENT_SECRET`
- Uses `assumeTrue()` to skip gracefully if credentials unavailable
- Logging provided by logback-classic (test scope only)

**Maven Plugin Configuration**:
- `maven-surefire-plugin` - Runs unit tests, excludes `@Tag("integration")`
- `maven-failsafe-plugin` - Runs integration tests (`*IntegrationTest.java`)
- `build-helper-maven-plugin` - Adds `src/integrationTest/java` to test sources

**Execution**:
```bash
# Unit tests only (default)
mvn test

# Integration tests (requires credentials)
mvn verify

# With explicit credentials
BLINKPAY_DEBIT_URL="https://staging.debit.blinkpay.co.nz" \
BLINKPAY_CLIENT_ID="your-client-id" \
BLINKPAY_CLIENT_SECRET="your-client-secret" \
mvn verify

# Skip all tests
mvn package -DskipTests
```

### Test Dependencies

**Test Scope Only** (not in runtime):
- JUnit Jupiter - Test framework
- AssertJ - Fluent assertions
- Mockito - Mocking HTTP calls in unit tests
- Logback Classic - Logging implementation for tests

**Note**: No Spring or other heavyweight test frameworks used, aligning with lightweight SDK design

## Build and Package

### Maven Commands

```bash
# Compile
mvn clean compile

# Run tests (excludes integration by default)
mvn test

# Run with integration tests
mvn test -DexcludedGroups=""

# Package JAR
mvn package

# Skip tests
mvn package -DskipTests

# Generate dependency tree
mvn dependency:tree
```

### Build Output

- Main JAR: `target/blink-debit-api-client-java-<version>.jar`
- Sources JAR: `target/blink-debit-api-client-java-<version>-sources.jar`
- Javadoc JAR: `target/blink-debit-api-client-java-<version>-javadoc.jar`

### Dependencies (Runtime)

**Compile Scope**:
- slf4j-api - Logging facade
- jackson-core - JSON processing
- jackson-databind - JSON object mapping
- jackson-datatype-jsr310 - Java 8 date/time support
- java-jwt - JWT token decoding

**Optional**:
- validation-api - Bean validation annotations
- javax.annotation-api - @Generated and @Nonnull annotations

**Test Scope**:
- junit-jupiter
- assertj-core
- mockito-core, mockito-junit-jupiter
- logback-classic

Versions are declared as properties in `pom.xml`; check for updates with
`mvn versions:display-property-updates` (rules in the root `version-rules.xml`).

## Development Guidelines

### Adding a New API Client

1. Create class in `nz.co.blink.debit.client.v1` package
2. Follow existing API client pattern (see above)
3. Add field to `BlinkDebitClient`
4. Initialize in `BlinkDebitClient` constructor
5. Add getter method to `BlinkDebitClient`
6. Create integration test if applicable

### Modifying DTOs

**DO**:
- Review OpenAPI spec changes carefully before updating DTOs
- Maintain TypeEnum in polymorphic parent classes
- Add required imports as needed
- Test thoroughly after DTO changes

**DON'T**:
- Modify core DTO logic (getters/setters/equals/hashCode)
- Add custom fields to DTOs
- Change Jackson serialization annotations
- Break backward compatibility unnecessarily

### Error Handling Pattern

```java
public Response apiMethod(Request request, String requestId) throws BlinkServiceException {
    // Validate inputs
    if (request == null) {
        throw new BlinkInvalidValueException("Request must not be null");
    }
    if (request.getId() == null) {
        throw new BlinkInvalidValueException("ID must not be null");
    }

    // Make HTTP call (may throw BlinkServiceException)
    return httpHelper.post(PATH, request, Response.class, requestId);
}
```

### Thread Safety

**Thread-Safe Components**:
- BlinkDebitClient (read operations)
- HttpClient (built-in thread safety)
- ObjectMapper (thread-safe after configuration)
- AccessTokenManager (synchronized token refresh)
- All API clients (stateless)

**Not Thread-Safe**:
- BlinkDebitConfig.Builder (don't share builder across threads)

### Logging

The SDK uses SLF4J for logging. Consumers must provide a logging implementation (e.g., Logback, Log4j2).

**Log Levels**:
- **INFO**: Client initialization, high-level operations
- **DEBUG**: HTTP request/response details, token refresh events
- **ERROR**: API failures, HTTP errors, exceptions

**Example Logback Configuration**:
```xml
<configuration>
    <logger name="nz.co.blink.debit" level="DEBUG"/>
</configuration>
```

## Common Patterns

### Request ID Handling

Every API method has two overloads:
```java
public Response method(Request request);  // Auto-generates UUID
public Response method(Request request, String requestId);  // Custom ID
```

Use custom request IDs for distributed tracing.

### Resource Cleanup

Always use try-with-resources:
```java
try (BlinkDebitClient client = new BlinkDebitClient(config)) {
    // Use client
} // Automatically closed
```

Or manual cleanup:
```java
BlinkDebitClient client = new BlinkDebitClient(config);
try {
    // Use client
} finally {
    client.close();
}
```

### Configuration from Environment

```java
// Uses BLINKPAY_* environment variables
BlinkDebitClient client = new BlinkDebitClient();
```

This calls `BlinkDebitConfig.fromEnvironment().build()` internally.

## API Endpoint Reference

| Operation | Client | Method | Path |
|-----------|--------|--------|------|
| Generate token | OAuthApiClient | generateAccessToken() | POST /oauth2/token |
| Create single consent | SingleConsentsApiClient | createSingleConsent() | POST /consents/single |
| Get consent | SingleConsentsApiClient | getConsent() | GET /consents/single/{id} |
| Revoke consent | SingleConsentsApiClient | revokeConsent() | DELETE /consents/single/{id} |
| Create enduring consent | EnduringConsentsApiClient | createEnduringConsent() | POST /consents/enduring |
| Get enduring consent | EnduringConsentsApiClient | getConsent() | GET /consents/enduring/{id} |
| Revoke enduring consent | EnduringConsentsApiClient | revokeConsent() | DELETE /consents/enduring/{id} |
| Create quick payment | QuickPaymentsApiClient | createQuickPayment() | POST /quick-payments |
| Get quick payment | QuickPaymentsApiClient | getQuickPayment() | GET /quick-payments/{id} |
| Revoke quick payment | QuickPaymentsApiClient | revokeQuickPayment() | DELETE /quick-payments/{id} |
| Create payment | PaymentsApiClient | createPayment() | POST /payments |
| Get payment | PaymentsApiClient | getPayment() | GET /payments/{id} |
| Create refund | RefundsApiClient | createRefund() | POST /refunds |
| Get refund | RefundsApiClient | getRefund() | GET /refunds/{id} |
| Get bank metadata | MetaApiClient | getMeta() | GET /meta |

## Notes for AI Assistants

- **DTOs**: Generated code - minimize manual edits
- **Polymorphism**: TypeEnum pattern for discriminated unions
- **Thread Safety**: All components thread-safe except Config.Builder
- **Error Handling**: BlinkServiceException for API, BlinkInvalidValueException for validation
- **Resource Management**: Always use try-with-resources or close() in finally
- **Testing**: Integration tests require real credentials, use @Tag("integration")
- **Commit Messages**: Use conventional commit format (feat:, fix:, docs:, etc.)
