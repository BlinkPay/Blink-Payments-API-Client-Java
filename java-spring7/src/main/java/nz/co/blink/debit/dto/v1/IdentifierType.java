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
 * The value type used to identify the customer with their bank.
 */
public enum IdentifierType {

    EMAIL("email"),

    PHONE_NUMBER("phone_number"),

    MOBILE_NUMBER("mobile_number"),

    BANKING_USERNAME("banking_username"),

    CONSENT_ID("consent_id");

    private final String value;

    IdentifierType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static IdentifierType fromValue(String identifierType) throws BlinkInvalidValueException {
        return Arrays.stream(IdentifierType.values())
                .filter(type -> String.valueOf(type.value).equals(identifierType))
                .findFirst()
                .orElseThrow(() -> new BlinkInvalidValueException("Unknown identifier type: " + identifierType));
    }
}
