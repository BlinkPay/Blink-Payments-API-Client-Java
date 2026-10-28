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
 * The model for a single consent request, relating to a one-off payment.
 */
@Schema(description = "The model for a single consent request, relating to a one-off payment.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SingleConsentRequest extends ConsentDetail implements OneOfconsentDetail {

    @JsonProperty("flow")
    private AuthFlow flow = null;

    @JsonProperty("pcr")
    private Pcr pcr = null;

    @JsonProperty("amount")
    private Amount amount = null;

    @JsonProperty("hashed_customer_identifier")
    private String hashedCustomerIdentifier = null;

    public SingleConsentRequest flow(AuthFlow flow) {
        this.flow = flow;
        return this;
    }

    /**
     * Get flow
     *
     * @return flow
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Authorization flow must not be null")
    @Valid
    public AuthFlow getFlow() {
        return flow;
    }

    public void setFlow(AuthFlow flow) {
        this.flow = flow;
    }

    public SingleConsentRequest pcr(Pcr pcr) {
        this.pcr = pcr;
        return this;
    }

    /**
     * Get pcr
     *
     * @return pcr
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "PCR must not be null")
    @Valid
    public Pcr getPcr() {
        return pcr;
    }

    public void setPcr(Pcr pcr) {
        this.pcr = pcr;
    }

    public SingleConsentRequest amount(Amount amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Get amount
     *
     * @return amount
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Amount must not be null")
    @Valid
    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public SingleConsentRequest hashedCustomerIdentifier(String hashedCustomerIdentifier) {
        this.hashedCustomerIdentifier = hashedCustomerIdentifier;
        return this;
    }

    /**
     * The hashed unique ID of the customer e.g. customer internal ID. SHA-256 is recommended.
     *
     * @return hashedCustomerIdentifier
     **/
    @Schema(description = "The hashed unique ID of the customer e.g. customer internal ID. SHA-256 is recommended.")
    public String getHashedCustomerIdentifier() {
        return hashedCustomerIdentifier;
    }

    public void setHashedCustomerIdentifier(String hashedCustomerIdentifier) {
        this.hashedCustomerIdentifier = hashedCustomerIdentifier;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.SINGLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingleConsentRequest singleConsentRequest = (SingleConsentRequest) o;
        return Objects.equals(this.flow, singleConsentRequest.flow)
                && Objects.equals(this.pcr, singleConsentRequest.pcr)
                && Objects.equals(this.amount, singleConsentRequest.amount)
                && Objects.equals(this.hashedCustomerIdentifier, singleConsentRequest.hashedCustomerIdentifier)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flow, pcr, amount, hashedCustomerIdentifier, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SingleConsentRequest {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    flow: ").append(toIndentedString(flow)).append("\n");
        sb.append("    pcr: ").append(toIndentedString(pcr)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    hashedCustomerIdentifier: ").append(toIndentedString(hashedCustomerIdentifier)).append("\n");
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
