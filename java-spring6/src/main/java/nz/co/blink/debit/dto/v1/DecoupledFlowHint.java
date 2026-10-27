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
 * Decoupled flow hint.
 */
@Schema(description = "Decoupled flow hint.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-12T03:11:35.286Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecoupledFlowHint extends FlowHint {

    @JsonProperty("identifier_type")
    private IdentifierType identifierType = null;

    @JsonProperty("identifier_value")
    private String identifierValue = null;

    public DecoupledFlowHint identifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
        return this;
    }

    /**
     * Get identifierType
     *
     * @return identifierType
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Identifier type must not be null")
    @Valid
    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    public DecoupledFlowHint identifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
        return this;
    }

    /**
     * The identifier value. Required in requests. In responses it is {@code null} once the consent is no longer
     * awaiting submission or authorisation (it may already be {@code null} once the bank has accepted the request);
     * a {@code consent_id} reference is always returned.
     *
     * @return identifierValue
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
            description = "The identifier value. Required in requests. In responses it is `null` once the consent is no longer awaiting submission or authorisation (it may already be `null` once the bank has accepted the request); a `consent_id` reference is always returned.")
    @NotNull(message = "Identifier value must not be null")
    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
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
        DecoupledFlowHint decoupledFlowHint = (DecoupledFlowHint) o;
        return Objects.equals(this.identifierType, decoupledFlowHint.identifierType)
                && Objects.equals(this.identifierValue, decoupledFlowHint.identifierValue)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierType, identifierValue, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DecoupledFlowHint {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    identifierType: ").append(toIndentedString(identifierType)).append("\n");
        sb.append("    identifierValue: ").append(toIndentedString(identifierValue)).append("\n");
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
