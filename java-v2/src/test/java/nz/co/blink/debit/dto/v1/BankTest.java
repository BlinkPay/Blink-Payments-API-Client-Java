package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertThat(objectMapper.writeValueAsString(Bank.KIWI_BANK)).isEqualTo("\"Kiwibank\"");
    }

    @Test
    void testKiwibankGetValueReturnsCorrectWireValue() {
        assertThat(Bank.KIWI_BANK.getValue()).isEqualTo("Kiwibank");
    }

    @Test
    void testKiwibankDeserialisesFromCorrectWireValue() throws Exception {
        assertThat(objectMapper.readValue("\"Kiwibank\"", Bank.class)).isEqualTo(Bank.KIWI_BANK);
    }

    @Test
    void testFromValueResolvesCorrectWireValue() {
        assertThat(Bank.fromValue("Kiwibank")).isEqualTo(Bank.KIWI_BANK);
    }

    @Test
    void testFromValueRejectsOldWireValue() {
        assertThatThrownBy(() -> Bank.fromValue("KiwiBank"))
                .isInstanceOf(IllegalArgumentException.class);
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
    void testFromValueResolvesNzhlAndCardWireValues() {
        assertThat(Bank.fromValue("NZHL")).isEqualTo(Bank.NZHL);
        assertThat(Bank.fromValue("Card")).isEqualTo(Bank.CARD);
    }

    @Test
    void testFromValueRejectsRetiredCybersourceValue() {
        assertThatThrownBy(() -> Bank.fromValue("Cybersource"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
