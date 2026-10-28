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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * The available bank features
 */
@Schema(description = "The available bank features")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankmetadataFeatures {

    @JsonProperty("enduring_consent")
    private BankmetadataFeaturesEnduringConsent enduringConsent = null;

    @JsonProperty("decoupled_flow")
    private BankmetadataFeaturesDecoupledFlow decoupledFlow = null;

    @JsonProperty("card_payment")
    private BankmetadataFeaturesCardPayment cardPayment = null;

    public BankmetadataFeatures enduringConsent(BankmetadataFeaturesEnduringConsent enduringConsent) {
        this.enduringConsent = enduringConsent;
        return this;
    }

    /**
     * Get enduringConsent
     *
     * @return enduringConsent
     **/
    @Schema(description = "")
    @Valid
    public BankmetadataFeaturesEnduringConsent getEnduringConsent() {
        return enduringConsent;
    }

    public void setEnduringConsent(BankmetadataFeaturesEnduringConsent enduringConsent) {
        this.enduringConsent = enduringConsent;
    }

    public BankmetadataFeatures decoupledFlow(BankmetadataFeaturesDecoupledFlow decoupledFlow) {
        this.decoupledFlow = decoupledFlow;
        return this;
    }

    /**
     * Get decoupledFlow
     *
     * @return decoupledFlow
     **/
    @Schema(description = "")
    @Valid
    public BankmetadataFeaturesDecoupledFlow getDecoupledFlow() {
        return decoupledFlow;
    }

    public void setDecoupledFlow(BankmetadataFeaturesDecoupledFlow decoupledFlow) {
        this.decoupledFlow = decoupledFlow;
    }

    public BankmetadataFeatures cardPayment(BankmetadataFeaturesCardPayment cardPayment) {
        this.cardPayment = cardPayment;
        return this;
    }

    /**
     * Get cardPayment
     *
     * @return cardPayment
     **/
    @Schema(description = "")
    @Valid
    public BankmetadataFeaturesCardPayment getCardPayment() {
        return cardPayment;
    }

    public void setCardPayment(BankmetadataFeaturesCardPayment cardPayment) {
        this.cardPayment = cardPayment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankmetadataFeatures bankmetadataFeatures = (BankmetadataFeatures) o;
        return Objects.equals(this.enduringConsent, bankmetadataFeatures.enduringConsent)
               && Objects.equals(this.decoupledFlow, bankmetadataFeatures.decoupledFlow)
               && Objects.equals(this.cardPayment, bankmetadataFeatures.cardPayment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enduringConsent, decoupledFlow, cardPayment);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BankmetadataFeatures {\n");
        sb.append("    enduringConsent: ").append(toIndentedString(enduringConsent)).append("\n");
        sb.append("    decoupledFlow: ").append(toIndentedString(decoupledFlow)).append("\n");
        sb.append("    cardPayment: ").append(toIndentedString(cardPayment)).append("\n");
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
