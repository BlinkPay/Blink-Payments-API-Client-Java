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
package nz.co.blink.debit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.time.Duration;

/**
 * The Blink Pay properties for Blink Bills and Blink Debit (Blink AutoPay and Blink PayNow).
 */
@ConfigurationProperties(prefix = "blinkpay")
@ConfigurationPropertiesScan
@Data
public class BlinkPayProperties {

    private Debit debit = new Debit();

    private Client client = new Client();

    private Max max = new Max();

    private Pending pending = new Pending();

    private Eviction eviction = new Eviction();

    private Retry retry = new Retry();

    @Data
    public static class Debit {

        private String url;
    }

    @Data
    public static class Client {

        private String id;

        private String secret;
    }

    @Data
    public static class Max {

        private Integer connections = 50;

        private Idle idle = new Idle();

        private Life life = new Life();

        @Data
        public static class Idle {

            private Duration time = Duration.parse("PT20S");
        }

        @Data
        public static class Life {

            private Duration time = Duration.parse("PT60S");
        }
    }

    @Data
    public static class Pending {

        private Acquire acquire = new Acquire();

        @ConfigurationProperties(prefix = "blinkpay.pending.acquire")
        @Data
        public static class Acquire {

            private Duration timeout = Duration.parse("PT10S");
        }
    }

    @Data
    public static class Eviction {

        private Duration interval = Duration.parse("PT60S");
    }

    @Data
    public static class Retry {

        private Boolean enabled = true;
    }
}
