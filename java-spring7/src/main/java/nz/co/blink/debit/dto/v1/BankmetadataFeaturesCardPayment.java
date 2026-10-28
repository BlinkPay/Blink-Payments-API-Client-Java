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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The card payment feature
 */
@Schema(description = "The card payment feature")
@Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2025-03-24T00:59:29.582625198Z[GMT]")
public class BankmetadataFeaturesCardPayment {

    @JsonProperty("enabled")
    private Boolean enabled = null;

    @JsonProperty("allowed_card_payment_types")
    private List<CardPaymentType> allowedCardPaymentTypes = new ArrayList<CardPaymentType>();

    @JsonProperty("allowed_card_networks")
    private List<CardNetwork> allowedCardNetworks = new ArrayList<CardNetwork>();

    public BankmetadataFeaturesCardPayment enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Whether card payment is enabled.
     *
     * @return enabled
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Whether card payment is enabled.")
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BankmetadataFeaturesCardPayment allowedCardPaymentTypes(List<CardPaymentType> allowedCardPaymentTypes) {
        this.allowedCardPaymentTypes = allowedCardPaymentTypes;
        return this;
    }

    public BankmetadataFeaturesCardPayment addAllowedCardPaymentTypesItem(CardPaymentType allowedCardPaymentTypesItem) {
        this.allowedCardPaymentTypes.add(allowedCardPaymentTypesItem);
        return this;
    }

    /**
     * The allowed card payment types.
     *
     * @return allowedCardPaymentTypes
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The allowed card payment types.")
    public List<CardPaymentType> getAllowedCardPaymentTypes() {
        return allowedCardPaymentTypes;
    }

    public void setAllowedCardPaymentTypes(List<CardPaymentType> allowedCardPaymentTypes) {
        this.allowedCardPaymentTypes = allowedCardPaymentTypes;
    }

    public BankmetadataFeaturesCardPayment allowedCardNetworks(List<CardNetwork> allowedCardNetworks) {
        this.allowedCardNetworks = allowedCardNetworks;
        return this;
    }

    public BankmetadataFeaturesCardPayment addAllowedCardNetworksItem(CardNetwork allowedCardNetworksItem) {
        this.allowedCardNetworks.add(allowedCardNetworksItem);
        return this;
    }

    /**
     * The allowed card networks.
     *
     * @return allowedCardNetworks
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The allowed card networks.")
    public List<CardNetwork> getAllowedCardNetworks() {
        return allowedCardNetworks;
    }

    public void setAllowedCardNetworks(List<CardNetwork> allowedCardNetworks) {
        this.allowedCardNetworks = allowedCardNetworks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankmetadataFeaturesCardPayment bankmetadataFeaturesCardPayment = (BankmetadataFeaturesCardPayment) o;
        return Objects.equals(this.enabled, bankmetadataFeaturesCardPayment.enabled)
               && Objects.equals(this.allowedCardPaymentTypes, bankmetadataFeaturesCardPayment.allowedCardPaymentTypes)
               && Objects.equals(this.allowedCardNetworks, bankmetadataFeaturesCardPayment.allowedCardNetworks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, allowedCardPaymentTypes, allowedCardNetworks);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BankmetadataFeaturesCardPayment {\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    allowedCardPaymentTypes: ").append(toIndentedString(allowedCardPaymentTypes)).append("\n");
        sb.append("    allowedCardNetworks: ").append(toIndentedString(allowedCardNetworks)).append("\n");
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
