package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link Bank} enum wire values, in particular the Kiwibank fix (BDL-1261).
 */
class BankTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ===== Kiwibank wire value (BDL-1261) =====

    @Test
    void testKiwibankSerialisesToCorrectWireValue() throws Exception {
        assertThat(objectMapper.writeValueAsString(Bank.KIWIBANK)).isEqualTo("\"Kiwibank\"");
    }

    @Test
    void testKiwibankToStringReturnsCorrectWireValue() {
        assertThat(Bank.KIWIBANK.toString()).isEqualTo("Kiwibank");
    }

    @Test
    void testKiwibankDeserialisesFromCorrectWireValue() throws Exception {
        assertThat(objectMapper.readValue("\"Kiwibank\"", Bank.class)).isEqualTo(Bank.KIWIBANK);
    }

    @Test
    void testFromValueResolvesCorrectWireValue() throws BlinkInvalidValueException {
        assertThat(Bank.fromValue("Kiwibank")).isEqualTo(Bank.KIWIBANK);
    }

    @Test
    void testFromValueResolvesConstantName() throws BlinkInvalidValueException {
        // Domain-model (entity) lookups match on the constant name; unaffected by the wire value change
        assertThat(Bank.fromValue("KIWIBANK")).isEqualTo(Bank.KIWIBANK);
    }

    @Test
    void testFromValueRejectsOldWireValue() {
        assertThatThrownBy(() -> Bank.fromValue("KiwiBank"))
                .isInstanceOf(BlinkInvalidValueException.class);
    }
}
