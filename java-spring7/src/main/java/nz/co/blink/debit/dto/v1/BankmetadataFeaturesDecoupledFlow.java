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

import java.util.List;
import java.util.Objects;

/**
 * The decoupled flow bank feature
 */
@Schema(description = "The decoupled flow bank feature")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankmetadataFeaturesDecoupledFlow {

    @JsonProperty("enabled")
    private Boolean enabled = null;

    @JsonProperty("available_identifiers")
    private List<BankmetadataFeaturesDecoupledFlowAvailableIdentifiers> availableIdentifiers = null;

    @JsonProperty("request_timeout")
    private String requestTimeout = null;

    public BankmetadataFeaturesDecoupledFlow enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Whether the Decoupled Flow is enabled.
     *
     * @return enabled
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Whether the Decoupled Flow is enabled.")
    @NotNull(message = "Enabled flag must not be null")
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BankmetadataFeaturesDecoupledFlow availableIdentifiers(List<BankmetadataFeaturesDecoupledFlowAvailableIdentifiers> availableIdentifiers) {
        this.availableIdentifiers = availableIdentifiers;
        return this;
    }

    /**
     * If enabled, will show the available fields to use to identify the customer with their bank.
     *
     * @return availableIdentifiers
     **/
    @Schema(description = "If enabled, will show the available fields to use to identify the customer with their bank.")
    public List<BankmetadataFeaturesDecoupledFlowAvailableIdentifiers> getAvailableIdentifiers() {
        return availableIdentifiers;
    }

    public void setAvailableIdentifiers(List<BankmetadataFeaturesDecoupledFlowAvailableIdentifiers> availableIdentifiers) {
        this.availableIdentifiers = availableIdentifiers;
    }

    public BankmetadataFeaturesDecoupledFlow requestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * ISO8601 time duration until the decoupled flow request expires/times out
     *
     * @return requestTimeout
     **/
    @Schema(example = "P1DT00H00M00S", requiredMode = Schema.RequiredMode.REQUIRED, description = "ISO8601 time duration until the decoupled flow request expires/times out")
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
        BankmetadataFeaturesDecoupledFlow bankmetadataFeaturesDecoupledFlow = (BankmetadataFeaturesDecoupledFlow) o;
        return Objects.equals(this.enabled, bankmetadataFeaturesDecoupledFlow.enabled)
                && Objects.equals(this.availableIdentifiers, bankmetadataFeaturesDecoupledFlow.availableIdentifiers)
                && Objects.equals(this.requestTimeout, bankmetadataFeaturesDecoupledFlow.requestTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, availableIdentifiers, requestTimeout);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BankmetadataFeaturesDecoupledFlow {\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    availableIdentifiers: ").append(toIndentedString(availableIdentifiers)).append("\n");
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
