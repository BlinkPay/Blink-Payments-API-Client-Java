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
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * A best-effort preview of the surcharge each payment method would add, returned while a gateway consent is in
 * GatewayAwaitingSubmission status. Only methods the merchant surcharges are included, and card only when the consent
 * can be paid by card. Omitted if the rates cannot be looked up. Read-only: returned by Blink.
 */
@Schema(description = "A best-effort preview of the surcharge each payment method would add, returned while a gateway consent is in GatewayAwaitingSubmission status. Only methods the merchant surcharges are included, and card only when the consent can be paid by card. Omitted if the rates cannot be looked up. Read-only: returned by Blink.", accessMode = Schema.AccessMode.READ_ONLY)
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurchargeOptions {

    @JsonProperty("bank")
    private SurchargeOption bank = null;

    @JsonProperty("card")
    private SurchargeOption card = null;

    public SurchargeOptions bank(SurchargeOption bank) {
        this.bank = bank;
        return this;
    }

    /**
     * The surcharge that would apply when paying via bank (direct credit).
     *
     * @return bank
     **/
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "The surcharge that would apply when paying via bank (direct credit).")
    @Valid
    public SurchargeOption getBank() {
        return bank;
    }

    public void setBank(SurchargeOption bank) {
        this.bank = bank;
    }

    public SurchargeOptions card(SurchargeOption card) {
        this.card = card;
        return this;
    }

    /**
     * The surcharge that would apply when paying via card. Single consents and quick payments only.
     *
     * @return card
     **/
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "The surcharge that would apply when paying via card. Single consents and quick payments only.")
    @Valid
    public SurchargeOption getCard() {
        return card;
    }

    public void setCard(SurchargeOption card) {
        this.card = card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SurchargeOptions surchargeOptions = (SurchargeOptions) o;
        return Objects.equals(this.bank, surchargeOptions.bank)
               && Objects.equals(this.card, surchargeOptions.card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bank, card);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SurchargeOptions {\n");
        sb.append("    bank: ").append(toIndentedString(bank)).append("\n");
        sb.append("    card: ").append(toIndentedString(card)).append("\n");
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
