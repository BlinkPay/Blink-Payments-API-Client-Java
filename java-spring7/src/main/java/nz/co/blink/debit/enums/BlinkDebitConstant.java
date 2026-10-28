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
package nz.co.blink.debit.enums;

import lombok.Getter;

/**
 * The enumeration of constants.
 */
@Getter
public enum BlinkDebitConstant {

    REQUEST_ID("request-id"),

    CORRELATION_ID("x-correlation-id"),

    IDEMPOTENCY_KEY("idempotency-key"),

    CUSTOMER_IP("x-customer-ip"),

    CUSTOMER_USER_AGENT("x-customer-user-agent"),

    BEARER("Bearer "),

    AUTHORIZATION("Authorization"),

    TOKEN_PATH("/oauth2/token"),

    METADATA_PATH("/payments/v1/meta"),

    SINGLE_CONSENTS_PATH("/payments/v1/single-consents"),

    ENDURING_CONSENTS_PATH("/payments/v1/enduring-consents"),

    QUICK_PAYMENTS_PATH("/payments/v1/quick-payments"),

    PAYMENTS_PATH("/payments/v1/payments"),

    REFUNDS_PATH("/payments/v1/refunds"),

    USER_AGENT_VALUE("Java/Blink SDK 1.4");

    private final String value;

    /**
     * Default constructor.
     *
     * @param value the value
     */
    BlinkDebitConstant(String value) {
        this.value = value;
    }
}
