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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Objects;

/**
 * Amount with currency.
 */
@Schema(description = "Amount with currency.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-07-22T04:23:07.030Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Amount {

    @JsonProperty("total")
    private String total = null;

    @JsonProperty(value = "surcharge", access = JsonProperty.Access.WRITE_ONLY)
    private String surcharge = null;

    @JsonProperty(value = "total_charge", access = JsonProperty.Access.WRITE_ONLY)
    private String totalCharge = null;

    /**
     * The currency. Only NZD is supported.
     */
    public enum CurrencyEnum {
        NZD("NZD");

        private String value;

        CurrencyEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static CurrencyEnum fromValue(String currency) throws BlinkInvalidValueException {
            return Arrays.stream(CurrencyEnum.values())
                    .filter(currencyEnum -> String.valueOf(currencyEnum.value).equals(currency))
                    .findFirst()
                    .orElseThrow(() -> new BlinkInvalidValueException("Unknown currency: " + currency));
        }
    }

    @JsonProperty("currency")
    private CurrencyEnum currency = null;

    public Amount total(String total) {
        this.total = total;
        return this;
    }

    /**
     * The amount.
     *
     * @return total
     **/
    @Schema(example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED, description = "The amount.")
    @NotNull(message = "Total must not be null")
    @Pattern(regexp = "^\\d{1,13}\\.\\d{1,2}$")
    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public Amount surcharge(String surcharge) {
        this.surcharge = surcharge;
        return this;
    }

    /**
     * The surcharge applied to this payment, where surcharging applies. Read-only: returned by Blink and never
     * serialised into a request.
     *
     * @return surcharge
     **/
    @Schema(example = "0.68", accessMode = Schema.AccessMode.READ_ONLY, description = "The surcharge applied to this payment, where surcharging applies.")
    @Pattern(regexp = "^\\d{1,13}\\.\\d{1,2}$")
    public String getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(String surcharge) {
        this.surcharge = surcharge;
    }

    public Amount totalCharge(String totalCharge) {
        this.totalCharge = totalCharge;
        return this;
    }

    /**
     * The total amount charged to the customer, inclusive of any surcharge (total + surcharge). On an enduring
     * consent's maxima it is the raised maximum. Read-only: returned by Blink and never serialised into a request.
     *
     * @return totalCharge
     **/
    @Schema(example = "100.68", accessMode = Schema.AccessMode.READ_ONLY, description = "The total amount charged to the customer, inclusive of any surcharge (total + surcharge).")
    @Pattern(regexp = "^\\d{1,13}\\.\\d{1,2}$")
    public String getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(String totalCharge) {
        this.totalCharge = totalCharge;
    }

    public Amount currency(CurrencyEnum currency) {
        this.currency = currency;
        return this;
    }

    /**
     * The currency. Only NZD is supported.
     *
     * @return currency
     **/
    @Schema(example = "NZD", requiredMode = Schema.RequiredMode.REQUIRED, description = "The currency. Only NZD is supported.")
    @NotNull(message = "Currency must not be null and only NZD is supported")
    public CurrencyEnum getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyEnum currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Amount amount = (Amount) o;
        return Objects.equals(this.total, amount.total)
                && Objects.equals(this.surcharge, amount.surcharge)
                && Objects.equals(this.totalCharge, amount.totalCharge)
                && Objects.equals(this.currency, amount.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, surcharge, totalCharge, currency);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Amount {\n");
        sb.append("    total: ").append(toIndentedString(total)).append("\n");
        sb.append("    surcharge: ").append(toIndentedString(surcharge)).append("\n");
        sb.append("    totalCharge: ").append(toIndentedString(totalCharge)).append("\n");
        sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
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
