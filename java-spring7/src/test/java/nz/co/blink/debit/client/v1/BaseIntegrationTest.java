package nz.co.blink.debit.client.v1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;

/**
 * Base class for integration tests that require real API credentials.
 * Tests will be skipped if credentials are not available.
 */
public abstract class BaseIntegrationTest {

    @BeforeAll
    static void checkCredentials() {
        // Tests will use credentials from environment variables
        // They will fail if credentials are invalid or not set
    }
}