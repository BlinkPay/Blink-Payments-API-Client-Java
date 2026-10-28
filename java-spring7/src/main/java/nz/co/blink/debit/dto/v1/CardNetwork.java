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

import java.util.Arrays;

/**
 * The card network.
 */
public enum CardNetwork {

    VISA("VISA", "001"),
    MASTERCARD("MASTERCARD", "002"),
    AMEX("AMEX", "003"),
    DISCOVER("DISCOVER", "004"),
    DINERSCLUB("DINERSCLUB", "005"),
    JCB("JCB", "007"),;

    private final String value;

    private final String type;

    CardNetwork(String value, String type) {
        this.value = value;
        this.type = type;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static CardNetwork fromValue(String text) {
        // `value` comparison is for data transfer object, `name` comparison is for domain model object (entity)
        return Arrays.stream(CardNetwork.values())
                .filter(cardNetwork -> cardNetwork.value.equals(text) || cardNetwork.name().equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown card network: " + text));
    }

    public static CardNetwork fromType(String text) {
        return Arrays.stream(CardNetwork.values())
                .filter(card -> card.type.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown card network type: " + text));
    }
}
