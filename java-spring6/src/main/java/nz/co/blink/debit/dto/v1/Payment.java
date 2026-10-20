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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import io.swagger.v3.oas.annotations.media.Schema;
import nz.co.blink.debit.helpers.CustomOffsetDateTimeDeserializer;
import nz.co.blink.debit.helpers.CustomOffsetDateTimeSerializer;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The model for a payment.
 */
@Schema(description = "The model for a payment.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment {

    @JsonProperty("payment_id")
    private UUID paymentId = null;

    /**
     * The type of payment (single or enduring).
     */
    public enum TypeEnum {

        SINGLE("single"),

        ENDURING("enduring");

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

    @JsonProperty("type")
    private TypeEnum type = null;

    /**
     * The status of the payment.
     */
    public enum StatusEnum {

        PENDING("Pending"),

        ACCEPTEDSETTLEMENTINPROCESS("AcceptedSettlementInProcess"),

        ACCEPTEDSETTLEMENTCOMPLETED("AcceptedSettlementCompleted"),

        REJECTED("Rejected");

        private String value;

        StatusEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String status) throws BlinkInvalidValueException {
            return Arrays.stream(StatusEnum.values())
                    .filter(statusEnum -> String.valueOf(statusEnum.value).equals(status))
                    .findFirst()
                    .orElseThrow(() -> new BlinkInvalidValueException("Unknown status: " + status));
        }
    }

    @JsonProperty("status")
    private StatusEnum status = null;

    /**
     * The reason for `AcceptedSettlementCompleted`.
     */
    public enum AcceptedReasonEnum {
        SOURCE_BANK_PAYMENT_SENT("source_bank_payment_sent"),

        CARD_NETWORK_ACCEPTED("card_network_accepted");

        private String value;

        AcceptedReasonEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static AcceptedReasonEnum fromValue(String acceptedReason) throws BlinkInvalidValueException {
            return Arrays.stream(AcceptedReasonEnum.values())
                    .filter(acceptedReasonEnum -> String.valueOf(acceptedReasonEnum.value).equals(acceptedReason))
                    .findFirst()
                    .orElseThrow(() -> new BlinkInvalidValueException("Unknown accepted reason: " + acceptedReason));
        }
    }

    @JsonProperty("accepted_reason")
    private AcceptedReasonEnum acceptedReason = null;

    @JsonProperty("creation_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime creationTimestamp = null;

    @JsonProperty("status_updated_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime statusUpdatedTimestamp = null;

    @JsonProperty("detail")
    private PaymentRequest detail = null;

    @JsonProperty("refunds")
    @Valid
    private List<Refund> refunds = new ArrayList<>();

    public Payment paymentId(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    /**
     * The payment ID
     *
     * @return paymentId
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The payment ID")
    @NotNull(message = "Payment ID must not be null")
    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public Payment type(TypeEnum type) {
        this.type = type;
        return this;
    }

    /**
     * The type of payment (single of enduring).
     *
     * @return type
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The type of payment (single of enduring).")
    @NotNull(message = "Type must not be null")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public Payment status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
     * The status of the payment.
     *
     * @return status
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The status of the payment.")
    @NotNull(message = "Status must not be null")
    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Payment acceptedReason(AcceptedReasonEnum acceptedReason) {
        this.acceptedReason = acceptedReason;
        return this;
    }

    /**
     * The reason for `AcceptedSettlementCompleted`.
     *
     * @return acceptedReason
     **/
    @Schema(description = "The reason for `AcceptedSettlementCompleted`.")
    public AcceptedReasonEnum getAcceptedReason() {
        return acceptedReason;
    }

    public void setAcceptedReason(AcceptedReasonEnum acceptedReason) {
        this.acceptedReason = acceptedReason;
    }

    public Payment creationTimestamp(OffsetDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    /**
     * The timestamp that the payment was created.
     *
     * @return creationTimestamp
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The timestamp that the payment was created.")
    @NotNull(message = "Creation timestamp must not be null")
    @Valid
    public OffsetDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(OffsetDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Payment statusUpdatedTimestamp(OffsetDateTime statusUpdatedTimestamp) {
        this.statusUpdatedTimestamp = statusUpdatedTimestamp;
        return this;
    }

    /**
     * The timestamp that the payment status was last updated.
     *
     * @return statusUpdatedTimestamp
     **/
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "The timestamp that the payment status was last updated.")
    @Valid
    public OffsetDateTime getStatusUpdatedTimestamp() {
        return statusUpdatedTimestamp;
    }

    public void setStatusUpdatedTimestamp(OffsetDateTime statusUpdatedTimestamp) {
        this.statusUpdatedTimestamp = statusUpdatedTimestamp;
    }

    public Payment detail(PaymentRequest detail) {
        this.detail = detail;
        return this;
    }

    /**
     * Get detail
     *
     * @return detail
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Payment request must not be null")
    @Valid
    public PaymentRequest getDetail() {
        return detail;
    }

    public void setDetail(PaymentRequest detail) {
        this.detail = detail;
    }

    public Payment refunds(List<Refund> refunds) {
        this.refunds = refunds;
        return this;
    }

    public Payment addRefundsItem(Refund refundsItem) {
        this.refunds.add(refundsItem);
        return this;
    }

    /**
     * Refunds that are related to this payment, if any.
     *
     * @return refunds
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Refunds that are related to this payment, if any.")
    @NotNull
    @Valid
    public List<Refund> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<Refund> refunds) {
        this.refunds = refunds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Payment payment = (Payment) o;
        return Objects.equals(this.paymentId, payment.paymentId)
                && Objects.equals(this.type, payment.type)
                && Objects.equals(this.status, payment.status)
                && Objects.equals(this.acceptedReason, payment.acceptedReason)
                && Objects.equals(this.creationTimestamp, payment.creationTimestamp)
                && Objects.equals(this.statusUpdatedTimestamp, payment.statusUpdatedTimestamp)
                && Objects.equals(this.detail, payment.detail)
                && Objects.equals(this.refunds, payment.refunds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, type, status, acceptedReason, creationTimestamp, statusUpdatedTimestamp, detail,
                refunds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Payment {\n");
        sb.append("    paymentId: ").append(toIndentedString(paymentId)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    acceptedReason: ").append(toIndentedString(acceptedReason)).append("\n");
        sb.append("    creationTimestamp: ").append(toIndentedString(creationTimestamp)).append("\n");
        sb.append("    statusUpdatedTimestamp: ").append(toIndentedString(statusUpdatedTimestamp)).append("\n");
        sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
        sb.append("    refunds: ").append(toIndentedString(refunds)).append("\n");
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
