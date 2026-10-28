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
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * If enabled, will show the available fields to use to identify the customer with their bank
 */
@Schema(description = "If enabled, will show the available fields to use to identify the customer with their bank")
@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankmetadataFeaturesDecoupledFlowAvailableIdentifiers {

    @JsonProperty("type")
    private IdentifierType type;

    @JsonProperty("regex")
    private String regex;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    public BankmetadataFeaturesDecoupledFlowAvailableIdentifiers type(IdentifierType type) {
        this.type = type;
        return this;
    }

    /**
     * The type of the field
     *
     * @return type
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The type of the field")
    @NotNull(message = "Identifier type must not be null")
    public IdentifierType getType() {
        return type;
    }

    public void setType(IdentifierType type) {
        this.type = type;
    }

    public BankmetadataFeaturesDecoupledFlowAvailableIdentifiers regex(String regex) {
        this.regex = regex;
        return this;
    }

    /**
     * A regex that can be used for validation of the field
     *
     * @return regex
     */
    @Schema(example = "^[0-9]{9}$", description = "A regex that can be used for validation of the field")
    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public BankmetadataFeaturesDecoupledFlowAvailableIdentifiers name(String name) {
        this.name = name;
        return this;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Access Number", description = "The common name of the field")
    @NotNull(message = "Name must not be null")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BankmetadataFeaturesDecoupledFlowAvailableIdentifiers description(String description) {
        this.description = description;
        return this;
    }

    /**
     * The description of the field and/or where to find it
     *
     * @return description
     */
    @Schema(example = "The nine-digit access number used to login to your internet banking", description = "The description of the field and/or where to find it")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankmetadataFeaturesDecoupledFlowAvailableIdentifiers that = (BankmetadataFeaturesDecoupledFlowAvailableIdentifiers) o;
        return type == that.type
                && Objects.equals(regex, that.regex)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, regex, name, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BankmetadataFeaturesDecoupledFlowAvailableIdentifiers {\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    regex: ").append(toIndentedString(regex)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
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
