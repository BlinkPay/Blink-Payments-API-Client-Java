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

/**
 * The enumeration of Blink Pay properties.
 */
public enum BlinkPayProperty {

    BLINKPAY_DEBIT_URL("blinkpay.debit.url", null),

    BLINKPAY_CLIENT_ID("blinkpay.client.id", null),

    BLINKPAY_CLIENT_SECRET("blinkpay.client.secret", null),

    BLINKPAY_MAX_CONNECTIONS("blinkpay.max.connections", "10"),

    BLINKPAY_MAX_IDLE_TIME("blinkpay.max.idle.time", "PT20S"),

    BLINKPAY_MAX_LIFE_TIME("blinkpay.max.life.time", "PT60S"),

    BLINKPAY_PENDING_ACQUIRE_TIMEOUT("blinkpay.pending.acquire.timeout", "PT10S"),

    BLINKPAY_EVICTION_INTERVAL("blinkpay.eviction.interval", "PT60S"),

    BLINKPAY_ACTIVE_PROFILE("blinkpay.active.profile", "test"),

    BLINKPAY_RETRY_ENABLED("blinkpay.retry.enabled", "true");

    private final String propertyName;

    private final String defaultValue;

    /**
     * Default constructor.
     *
     * @param propertyName the dot-separated property name
     * @param defaultValue the default value, if applicable
     */
    BlinkPayProperty(String propertyName, String defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the dot-separated property name
     *
     * @return the dot-separated property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the default value, if applicable
     *
     * @return the default value, if applicable
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
