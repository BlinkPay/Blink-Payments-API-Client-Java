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
package nz.co.blink.debit.dto.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nz.co.blink.debit.exception.BlinkInvalidValueException;

import java.util.Arrays;

/**
 * The bank name. Required if not using Blink's hosted gateway.
 */
public enum Bank {

    ASB("ASB"),

    ANZ("ANZ"),

    BNZ("BNZ"),

    WESTPAC("Westpac"),

    KIWIBANK("Kiwibank"),

    NZHL("NZHL"),

    /**
     * The Payments NZ generic sandbox bank powered by Middleware NZ. FOR SANDBOX PURPOSES ONLY.
     */
    PNZ("PNZ"),

    CARD("Card");

    private final String value;

    Bank(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static Bank fromValue(String text) throws BlinkInvalidValueException {
        // `value` comparison is for data transfer object, `name` comparison is for domain model object (entity)
        return Arrays.stream(Bank.values())
                .filter(bank -> bank.value.equals(text) ||  bank.name().equals(text))
                .findFirst()
                .orElseThrow(() -> new BlinkInvalidValueException("Unknown bank: " + text));
    }
}
