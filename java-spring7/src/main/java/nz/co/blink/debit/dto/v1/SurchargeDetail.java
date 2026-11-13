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
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * A surcharge and the total it gives. Used by an enduring consent's surcharge option, where it previews one of
 * the consent's maxima. Read-only: returned by Blink.
 */
@Schema(description = "A surcharge and the total it gives. Used by an enduring consent's surcharge option, where it previews one of the consent's maxima. Read-only: returned by Blink.", accessMode = Schema.AccessMode.READ_ONLY)
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurchargeDetail {

    @JsonProperty("surcharge")
    private String surcharge = null;

    @JsonProperty("total_charge")
    private String totalCharge = null;

    public SurchargeDetail surcharge(String surcharge) {
        this.surcharge = surcharge;
        return this;
    }

    /**
     * The amount the maximum is raised by.
     *
     * @return surcharge
     **/
    @Schema(example = "0.68", accessMode = Schema.AccessMode.READ_ONLY, description = "The amount the maximum is raised by.")
    @Pattern(regexp = "^\\d{1,13}\\.\\d{1,2}$")
    public String getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(String surcharge) {
        this.surcharge = surcharge;
    }

    public SurchargeDetail totalCharge(String totalCharge) {
        this.totalCharge = totalCharge;
        return this;
    }

    /**
     * The raised maximum (the maximum's total + surcharge).
     *
     * @return totalCharge
     **/
    @Schema(example = "100.68", accessMode = Schema.AccessMode.READ_ONLY, description = "The raised maximum (the maximum's total + surcharge).")
    @Pattern(regexp = "^\\d{1,13}\\.\\d{1,2}$")
    public String getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(String totalCharge) {
        this.totalCharge = totalCharge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SurchargeDetail surchargeDetail = (SurchargeDetail) o;
        return Objects.equals(this.surcharge, surchargeDetail.surcharge)
               && Objects.equals(this.totalCharge, surchargeDetail.totalCharge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surcharge, totalCharge);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SurchargeDetail {\n");
        sb.append("    surcharge: ").append(toIndentedString(surcharge)).append("\n");
        sb.append("    totalCharge: ").append(toIndentedString(totalCharge)).append("\n");
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
