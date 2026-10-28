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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * PCR (Particulars, code, reference) details.
 */
@Schema(description = "PCR (Particulars, code, reference) details.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pcr {

    @JsonProperty("particulars")
    private String particulars = null;

    @JsonProperty("code")
    private String code = null;

    @JsonProperty("reference")
    private String reference = null;

    public Pcr particulars(String particulars) {
        this.particulars = particulars;
        return this;
    }

    /**
     * The particulars
     *
     * @return particulars
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The particulars")
    @NotNull(message = "Particulars must not be null")
    @Pattern(regexp = "[a-zA-Z0-9- &#\\?:_/,\\.']{1,12}", message = "Particulars must match [a-zA-Z0-9- &#\\?:_/,\\.']{1,12}")
    @Size(min = 1, max = 12, message = "Particulars has a maximum length of 12 characters and must have at least 1 non-whitespace character")
    public String getParticulars() {
        return particulars;
    }

    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }

    public Pcr code(String code) {
        this.code = code;
        return this;
    }

    /**
     * The code
     *
     * @return code
     **/
    @Schema(description = "The code")
    @Pattern(regexp = "[a-zA-Z0-9- &#\\?:_/,\\.']{0,12}", message = "Code must match [a-zA-Z0-9- &#\\?:_/,\\.']{0,12}")
    @Size(max = 12, message = "Code has a maximum length of 12 characters")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Pcr reference(String reference) {
        this.reference = reference;
        return this;
    }

    /**
     * The reference
     *
     * @return reference
     **/
    @Schema(description = "The reference")
    @Pattern(regexp = "[a-zA-Z0-9- &#\\?:_/,\\.']{0,12}", message = "Reference must match [a-zA-Z0-9- &#\\?:_/,\\.']{0,12}")
    @Size(max = 12, message = "Reference has a maximum length of 12 characters")
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pcr pcr = (Pcr) o;
        return Objects.equals(this.particulars, pcr.particulars)
                && Objects.equals(this.code, pcr.code)
                && Objects.equals(this.reference, pcr.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(particulars, code, reference);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Pcr {\n");
        sb.append("    particulars: ").append(toIndentedString(particulars)).append("\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
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
