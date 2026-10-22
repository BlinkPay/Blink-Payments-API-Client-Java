# Blink Debit API Client - Technical Documentation

This document provides technical context for AI assistants working with this codebase.

## Project Overview

This repository contains Java SDK implementations for the Blink Debit API, enabling merchants to integrate **Blink PayNow** (one-off payments) and **Blink AutoPay** (recurring payments).

## Repository Structure

### SDK Versions

This repository contains two independent SDK modules:

#### java-v2 (Plain Java SDK - **RECOMMENDED**)
- **Location**: `java-v2/`
- **Technology**: Java 11+ HttpClient (synchronous blocking)
- **Dependencies**: Jackson (JSON), Auth0-JWT, SLF4J API (minimal dependencies)
- **API Style**: Synchronous blocking calls
- **Target Users**: Plain Java applications, serverless functions, memory-constrained environments
- **Documentation**: See `java-v2/README.md` and `java-v2/CLAUDE.md` for details

#### java-spring6 (Spring SDK)
- **Location**: `java-spring6/`
- **Technology**: Spring WebClient with Reactor (Mono/Flux)
- **Dependencies**: Spring Framework 6, Netty, Reactor
- **API Style**: Reactive/async with Mono and Flux
- **Target Users**: Spring Boot 3.x / Spring Framework 6+ applications

## Plain Java SDK Architecture

### Design Principles
1. **Minimal Dependencies**: Only essential runtime dependencies (Jackson, java-jwt, slf4j-api)
2. **Synchronous API**: Simple blocking calls using Java 11+ HttpClient
3. **No Framework Lock-in**: No Spring, Netty, or Reactor dependencies
4. **Immutable Configuration**: Builder pattern for configuration
5. **Thread-safe Token Management**: Automatic OAuth2 token refresh
6. **AutoCloseable Resources**: Proper resource management

### Core Components

#### 1. Configuration Layer
- **BlinkDebitConfig**: Immutable configuration with builder pattern
  - Environment variable support (`BLINKPAY_*`)
  - Validation of required fields
  - Timeout configuration (default: 10 seconds)
  - File: `java-v2/src/main/java/nz/co/blink/debit/config/BlinkDebitConfig.java`

#### 2. Authentication Layer
- **OAuthApiClient**: OAuth2 client credentials flow
  - File: `java-v2/src/main/java/nz/co/blink/debit/client/v1/OAuthApiClient.java`

- **AccessTokenManager**: Thread-safe token management
  - Automatic token refresh 60 seconds before expiry
  - Uses Auth0-JWT for token decoding
  - Double-checked locking for thread safety
  - File: `java-v2/src/main/java/nz/co/blink/debit/service/AccessTokenManager.java`

#### 3. HTTP Client Layer
- **HttpClientHelper**: Centralized HTTP operations
  - GET, POST, DELETE, and List response methods
  - Automatic authentication header injection
  - JSON serialization/deserialization via Jackson
  - Request ID generation (UUID)
  - Comprehensive error handling
  - File: `java-v2/src/main/java/nz/co/blink/debit/helpers/HttpClientHelper.java`

#### 4. API Clients
All API clients follow the same pattern:
- Constructor accepts HttpClientHelper
- Methods with and without custom request ID
- Input validation with BlinkInvalidValueException
- Synchronous blocking calls

**API Clients**:
- **SingleConsentsApiClient**: Single consent operations (create, get, revoke)
- **EnduringConsentsApiClient**: Enduring consent operations
- **QuickPaymentsApiClient**: Quick payment operations
- **PaymentsApiClient**: Payment operations (create, get)
- **RefundsApiClient**: Refund operations (create, get)
- **MetaApiClient**: Bank metadata operations

Files: `java-v2/src/main/java/nz/co/blink/debit/client/v1/*ApiClient.java`

#### 5. Main Facade
- **BlinkDebitClient**: Main entry point (implements AutoCloseable)
  - Creates and manages HttpClient instance
  - Initializes ObjectMapper with JavaTimeModule
  - Creates token manager and API clients
  - Provides getter methods for all API clients
  - File: `java-v2/src/main/java/nz/co/blink/debit/client/v1/BlinkDebitClient.java`

### DTOs and Model Classes

#### Generation
- DTOs are generated from OpenAPI spec using openapi-generator
- Location: `java-v2/src/main/java/nz/co/blink/debit/dto/v1/`
- **IMPORTANT**: Minimize manual edits to DTOs as they can be regenerated

#### Polymorphic DTOs
Three parent classes use TypeEnum discriminator for polymorphism:
- **AuthFlowDetail**: Discriminates GatewayFlow, RedirectFlow, DecoupledFlow
- **ConsentDetail**: Discriminates SingleConsentRequest, EnduringConsentRequest
- **RefundRequest**: Discriminates PartialRefundRequest, FullRefundRequest

Files:
- `java-v2/src/main/java/nz/co/blink/debit/dto/v1/AuthFlowDetail.java`
- `java-v2/src/main/java/nz/co/blink/debit/dto/v1/ConsentDetail.java`
- `java-v2/src/main/java/nz/co/blink/debit/dto/v1/RefundRequest.java`

### Exception Handling
- **BlinkServiceException**: API call failures, HTTP errors
- **BlinkInvalidValueException**: Invalid input parameters
- Files: `java-v2/src/main/java/nz/co/blink/debit/exception/`

### Testing

#### Integration Tests
- Location: `java-v2/src/integrationTest/java/`
- Uses JUnit 5 with @Tag("integration")
- Requires environment variables: BLINKPAY_CLIENT_ID, BLINKPAY_CLIENT_SECRET, BLINKPAY_DEBIT_URL
- Test file: `BlinkDebitClientIntegrationTest.java`

**Note**: The maven-surefire-plugin excludes integration tests by default. To run:
```bash
mvn test -DexcludedGroups=""
```

## Dependencies

### Runtime Dependencies (compile scope)
- **slf4j-api**: Logging facade
- **jackson-core, jackson-databind, jackson-datatype-jsr310**: JSON processing
- **java-jwt**: JWT token handling
- **validation-api**: Optional - bean validation annotations
- **javax.annotation-api**: Optional - @Generated and @Nonnull annotations

Versions are declared as properties in each module's `pom.xml`; check for updates with
`mvn versions:display-property-updates` (rules in `version-rules.xml`).

### Test Dependencies
- JUnit 5, AssertJ, Mockito
- Logback Classic - logging implementation for tests only

### Dependency Comparison
- **Spring SDK**: Heavier runtime dependencies (Spring, Netty, Reactor, Commons, Resilience4j, Hibernate Validator)
- **Plain Java SDK**: Minimal runtime dependencies (Jackson, java-jwt, slf4j-api)

Use `mvn dependency:tree` in each module directory to view full dependency trees.

## Build and Package

### Maven Commands
```bash
# Compile
mvn clean compile

# Run tests (unit only, integration excluded by default)
mvn test

# Run integration tests
BLINKPAY_CLIENT_ID="..." BLINKPAY_CLIENT_SECRET="..." mvn test -DexcludedGroups=""

# Package JAR
mvn package

# Skip tests
mvn package -DskipTests
```

### Output
- JAR: `target/blink-debit-api-client-java-<version>.jar`
- Sources JAR: `target/blink-debit-api-client-java-<version>-sources.jar`
- Javadoc JAR: `target/blink-debit-api-client-java-<version>-javadoc.jar`

## Usage Example

```java
import nz.co.blink.debit.client.v1.BlinkDebitClient;
import nz.co.blink.debit.config.BlinkDebitConfig;

// Option 1: Environment variables
BlinkDebitClient client = new BlinkDebitClient();

// Option 2: Builder configuration
BlinkDebitConfig config = BlinkDebitConfig.builder()
        .debitUrl("https://sandbox.debit.blinkpay.co.nz")
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .build();
BlinkDebitClient client = new BlinkDebitClient(config);

// Make API calls (synchronous blocking)
List<BankMetadata> banks = client.getMetaApi().getMeta();

// Close resources
client.close();
```

## Git Branch Strategy

- **Main branch**: `master`
- **Development branches**: Feature branches like `feature/v2-lightweight-sdk`
- Always commit with descriptive messages following conventional commits (feat:, fix:, docs:, refactor:, etc.)

## Common Tasks

### Regenerating DTOs
1. Update OpenAPI spec if needed
2. Run openapi-generator
3. Add TypeEnum to polymorphic parent DTOs (AuthFlowDetail, ConsentDetail, RefundRequest)
4. Import only the required classes (avoid unnecessary imports)
5. Compile and test

### Adding a New API Client
1. Create client class in `java-v2/src/main/java/nz/co/blink/debit/client/v1/`
2. Follow the pattern from existing API clients
3. Add field and getter in BlinkDebitClient facade
4. Initialize in BlinkDebitClient constructor
5. Create integration test if applicable

### Updating Dependencies
- Only add dependencies that are truly necessary
- Prefer standard Java libraries over third-party
- Document the reason for adding any new dependency
- Run dependency:tree to verify no transitive bloat

## Key Design Decisions

### Why Java 11+ HttpClient?
- Built into Java 11+ (no external dependency)
- Supports HTTP/1.1 and HTTP/2
- Connection pooling built-in
- Simple synchronous API for blocking calls

### Why Synchronous API?
- Simpler mental model for developers
- No need to understand Mono/Flux/reactive streams
- Lower memory overhead
- Better fit for serverless/short-lived processes
- Most payment API calls are naturally sequential anyway

### Why Minimal Dependencies?
- Faster cold starts (Lambda, Cloud Functions)
- Reduced security vulnerability surface
- Smaller deployment artifacts
- Easier dependency management
- Fewer version conflicts

### Why Builder Pattern for Config?
- Immutable configuration (thread-safe)
- Clear API for setting optional parameters
- Validation at build time
- Fluent/readable code

## Future Enhancements

Potential improvements for future iterations:
- Async API variant using CompletableFuture
- Retry logic with exponential backoff (optional)
- Circuit breaker pattern (optional, lightweight implementation)
- Request/response logging hooks
- Metrics collection hooks
- Custom serialization/deserialization support

## Notes for AI Assistants

- **DTOs**: Minimize edits to generated DTOs. Only add TypeEnum to polymorphic parents and necessary imports.
- **Error Handling**: Always use BlinkServiceException for API errors and BlinkInvalidValueException for validation errors.
- **Thread Safety**: AccessTokenManager is thread-safe. HttpClient is thread-safe. BlinkDebitClient is thread-safe for read operations.
- **Resource Management**: BlinkDebitClient implements AutoCloseable. Use try-with-resources or call close() explicitly.
- **Testing**: Integration tests require real API credentials. Unit tests should mock HTTP calls.
- **Commit Messages**: Use conventional commit format (feat:, fix:, docs:, refactor:, test:, chore:)
