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
 * The PCR and amount to use in the `partial_refund` request.
 */
@Schema(description = "The PCR and amount to use in the `partial_refund` request.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-02-14T00:59:00.905Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartialRefundRequest extends RefundDetail implements RefundRequest {

    @JsonProperty("amount")
    private Amount amount = null;

    @JsonProperty("pcr")
    private Pcr pcr = null;

    @JsonProperty("consent_redirect")
    private String consentRedirect = null;

    public PartialRefundRequest amount(Amount amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Get amount
     *
     * @return amount
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull
    @Valid
    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public PartialRefundRequest pcr(Pcr pcr) {
        this.pcr = pcr;
        return this;
    }

    /**
     * Get pcr
     *
     * @return pcr
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull
    @Valid
    public Pcr getPcr() {
        return pcr;
    }

    public void setPcr(Pcr pcr) {
        this.pcr = pcr;
    }

    public PartialRefundRequest consentRedirect(String consentRedirect) {
        this.consentRedirect = consentRedirect;
        return this;
    }

    /**
     * The URI that the merchant will need to visit to authorise the refund payment from their bank, if applicable.
     *
     * @return consentRedirect
     **/
    @Schema(description = "The URI that the merchant will need to visit to authorise the refund payment from their bank, if applicable.")
    public String getConsentRedirect() {
        return consentRedirect;
    }

    public void setConsentRedirect(String consentRedirect) {
        this.consentRedirect = consentRedirect;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.PARTIAL_REFUND;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartialRefundRequest partialRefundRequest = (PartialRefundRequest) o;
        return Objects.equals(this.amount, partialRefundRequest.amount)
                && Objects.equals(this.pcr, partialRefundRequest.pcr)
                && Objects.equals(this.consentRedirect, partialRefundRequest.consentRedirect)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, pcr, consentRedirect, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PartialRefundRequest {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    pcr: ").append(toIndentedString(pcr)).append("\n");
        sb.append("    consentRedirect: ").append(toIndentedString(consentRedirect)).append("\n");
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
