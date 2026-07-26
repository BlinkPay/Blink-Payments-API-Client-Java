package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
