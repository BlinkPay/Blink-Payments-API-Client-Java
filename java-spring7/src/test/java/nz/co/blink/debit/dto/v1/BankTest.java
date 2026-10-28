package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link Bank} enum wire values, including the Kiwibank fix (BDL-1261) and the
 * NZHL/Card enum reconciliation (BDL-1258).
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

    // ===== NZHL and Card enum reconciliation (BDL-1258) =====

    @Test
    void testNzhlSerialisesToCorrectWireValue() throws Exception {
        assertThat(objectMapper.writeValueAsString(Bank.NZHL)).isEqualTo("\"NZHL\"");
    }

    @Test
    void testCardSerialisesToCorrectWireValue() throws Exception {
        assertThat(objectMapper.writeValueAsString(Bank.CARD)).isEqualTo("\"Card\"");
    }

    @Test
    void testFromValueResolvesNzhlAndCardWireValues() throws BlinkInvalidValueException {
        assertThat(Bank.fromValue("NZHL")).isEqualTo(Bank.NZHL);
        assertThat(Bank.fromValue("Card")).isEqualTo(Bank.CARD);
    }

    @Test
    void testFromValueRejectsRetiredCybersourceValue() {
        assertThatThrownBy(() -> Bank.fromValue("Cybersource"))
                .isInstanceOf(BlinkInvalidValueException.class);
    }
}
