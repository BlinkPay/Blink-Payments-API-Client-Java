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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * The refund detail model.
 */
@Schema(description = "The refund detail model.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-02-13T21:29:12.057Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundDetail {

    @JsonProperty("payment_id")
    private UUID paymentId = null;

    /**
     * The refund type.
     */
    public enum TypeEnum {

        ACCOUNT_NUMBER("account_number"),

        PARTIAL_REFUND("partial_refund"),

        FULL_REFUND("full_refund");

        private String value;

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

    // Write-only: RefundRequest carries @JsonTypeInfo(property = "type"), which writes the
    // discriminator on serialisation. Without this the key is written twice and the API
    // rejects the request. Keep this if the DTOs are regenerated.
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private TypeEnum type = null;

    public RefundDetail paymentId(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    /**
     * The payment ID. The payment must have a status of `AcceptedSettlementCompleted`.
     *
     * @return paymentId
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The payment ID. The payment must have a status of `AcceptedSettlementCompleted`.")
    @NotNull
    @Valid
    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public RefundDetail type(TypeEnum type) {
        this.type = type;
        return this;
    }

    /**
     * The refund type.
     *
     * @return type
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The refund type.")
    @NotNull
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RefundDetail refundDetail = (RefundDetail) o;
        return Objects.equals(this.paymentId, refundDetail.paymentId)
                && Objects.equals(this.type, refundDetail.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RefundDetail {\n");
        sb.append("    paymentId: ").append(toIndentedString(paymentId)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
