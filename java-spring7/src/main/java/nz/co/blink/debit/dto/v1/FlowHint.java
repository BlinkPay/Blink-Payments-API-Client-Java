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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optionally include a hint to the Gateway of which flow should be used, allowing the customers details to be prefilled in order to make the checkout experience faster. You can also use Gateway flow hint to instruct Gateway to identify a customer using their last consent ID for mobile payments.
 */
@Schema(description = "Optionally include a hint to the Gateway of which flow should be used, allowing the customers details to be prefilled in order to make the checkout experience faster. You can also use Gateway flow hint to instruct Gateway to identify a customer using their last consent ID for mobile payments.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-12T03:11:35.286Z[GMT]")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RedirectFlowHint.class, name = "redirect"),
        @JsonSubTypes.Type(value = DecoupledFlowHint.class, name = "decoupled"),
})
public class FlowHint {

    @JsonProperty("bank")
    private Bank bank = null;

    /**
     * The flow hint type, i.e. Redirect or Decoupled.
     */
    public enum TypeEnum {

        REDIRECT("redirect"),

        DECOUPLED("decoupled");

        private final String value;

        TypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TypeEnum fromValue(String type) throws BlinkInvalidValueException {
            return Arrays.stream(TypeEnum.values())
                    .filter(typeEnum -> String.valueOf(typeEnum.value).equals(type))
                    .findFirst()
                    .orElseThrow(() -> new BlinkInvalidValueException("Unknown type: " + type));
        }
    }

    @JsonTypeId
    private TypeEnum type = null;

    public FlowHint type(TypeEnum type) {
        this.type = type;
        return this;
    }

    /**
     * The flow hint type, i.e. Redirect or Decoupled.
     *
     * @return type
     **/
    @Schema(example = "redirect", requiredMode = Schema.RequiredMode.REQUIRED, description = "The flow hint type, i.e. Redirect or Decoupled.")
    @NotNull(message = "Type must not be null")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public FlowHint bank(Bank bank) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowHint flowHint = (FlowHint) o;
        return Objects.equals(this.type, flowHint.type)
                && Objects.equals(this.bank, flowHint.bank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, bank);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class FlowHint {\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    bank: ").append(toIndentedString(bank)).append("\n");
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
