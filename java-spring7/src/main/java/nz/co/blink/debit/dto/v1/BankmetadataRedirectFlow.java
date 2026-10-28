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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * Details about the mandatory redirect flow functionality.
 */
@Schema(description = "Details about the mandatory redirect flow functionality.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankmetadataRedirectFlow {

    @JsonProperty("enabled")
    private Boolean enabled = null;

    @JsonProperty("request_timeout")
    private String requestTimeout = null;

    public BankmetadataRedirectFlow enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Whether the redirect flow is enabled
     *
     * @return enabled
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Whether the redirect flow is enabled")
    @NotNull(message = "Enabled flag must not be null")
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BankmetadataRedirectFlow requestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * ISO8601 time duration until the redirect flow consent request times out
     *
     * @return requestTimeout
     **/
    @Schema(example = "PT01H00M00S", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISO8601 time duration until the redirect flow consent request times out")
    @NotNull(message = "Request timeout must not be null")
    public String getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankmetadataRedirectFlow bankmetadataRedirectFlow = (BankmetadataRedirectFlow) o;
        return Objects.equals(this.enabled, bankmetadataRedirectFlow.enabled)
                && Objects.equals(this.requestTimeout, bankmetadataRedirectFlow.requestTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, requestTimeout);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BankmetadataRedirectFlow {\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    requestTimeout: ").append(toIndentedString(requestTimeout)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
