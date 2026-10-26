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
 * The details for a Decoupled flow.
 */
@Schema(description = "The details for a Decoupled flow.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-07-22T00:54:15.842Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecoupledFlow extends AuthFlowDetail implements OneOfauthFlowDetail {

    @JsonProperty("bank")
    private Bank bank = null;

    @JsonProperty("identifier_type")
    private IdentifierType identifierType = null;

    @JsonProperty("identifier_value")
    private String identifierValue = null;

    @JsonProperty("callback_url")
    private String callbackUrl = null;

    public DecoupledFlow bank(Bank bank) {
        this.bank = bank;
        return this;
    }

    /**
     * Get bank
     *
     * @return bank
     **/
    @Schema(description = "")
    @Valid
    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public DecoupledFlow identifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
        return this;
    }

    /**
     * The value type used to identify the customer with their bank.
     *
     * @return identifierType
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The value type used to identify the customer with their bank.")
    @NotNull(message = "Identifier type must not be null")
    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    public DecoupledFlow identifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
        return this;
    }

    /**
     * The identifier value.
     *
     * @return identifierValue
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "The identifier value.")
    @NotNull(message = "Identifier value must not be null")
    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public DecoupledFlow callbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
        return this;
    }

    /**
     * A callback URL to call once the consent status has been updated using decoupled flow. Blink will also append the `cid` (the Consent ID) in an additional URL parameter. This is sent to your api as as a GET request and will be retried up to 3 times if 5xx errors are received from your server.
     *
     * @return callbackUrl
     **/
    @Schema(example = "https://api.mybiller.co.nz/payments/1.0/consentresponse?secret=SOME_SECRET&id=SOME_ID", description = "A callback URL to call once the consent status has been updated using decoupled flow. Blink will also append the `consent_id` in an additional URL parameter. This is sent to your api as as a GET request.")
    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.DECOUPLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecoupledFlow decoupledFlow = (DecoupledFlow) o;
        return Objects.equals(this.bank, decoupledFlow.bank)
                && Objects.equals(this.identifierType, decoupledFlow.identifierType)
                && Objects.equals(this.identifierValue, decoupledFlow.identifierValue)
                && Objects.equals(this.callbackUrl, decoupledFlow.callbackUrl)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bank, identifierType, identifierValue, callbackUrl, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DecoupledFlow {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    bank: ").append(toIndentedString(bank)).append("\n");
        sb.append("    identifierType: ").append(toIndentedString(identifierType)).append("\n");
        sb.append("    identifierValue: ").append(toIndentedString(identifierValue)).append("\n");
        sb.append("    callbackUrl: ").append(toIndentedString(callbackUrl)).append("\n");
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
