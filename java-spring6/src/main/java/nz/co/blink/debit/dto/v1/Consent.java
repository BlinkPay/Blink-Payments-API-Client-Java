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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
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
 * The model for a consent.
 */
@Schema(description = "The model for a consent.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Consent {

    @JsonProperty("consent_id")
    private UUID consentId = null;

    /**
     * The status of the consent
     */
    public enum StatusEnum {
        GATEWAYAWAITINGSUBMISSION("GatewayAwaitingSubmission"),

        GATEWAYTIMEOUT("GatewayTimeout"),

        AWAITINGAUTHORISATION("AwaitingAuthorisation"),

        AUTHORISED("Authorised"),

        CONSUMED("Consumed"),

        REJECTED("Rejected"),

        REVOKED("Revoked");

        private final String value;

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

    @JsonProperty("creation_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime creationTimestamp = null;

    @JsonProperty("status_updated_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime statusUpdatedTimestamp = null;

    @JsonProperty("detail")
    private OneOfconsentDetail detail = null;

    @JsonProperty("payments")
    private List<Payment> payments = new ArrayList<>();

    @JsonProperty("card_network")
    private CardNetwork cardNetwork = null;

    public Consent consentId(UUID consentId) {
        this.consentId = consentId;
        return this;
    }

    /**
     * The consent ID
     *
     * @return consentId
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The consent ID")
    @NotNull(message = "Consent ID must not be null")
    public UUID getConsentId() {
        return consentId;
    }

    public void setConsentId(UUID consentId) {
        this.consentId = consentId;
    }

    public Consent status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
     * The status of the consent
     *
     * @return status
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The status of the consent")
    @NotNull(message = "Status must not be null")
    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Consent creationTimestamp(OffsetDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
        return this;
    }

    /**
     * The timestamp that that the consent was created
     *
     * @return creationTimestamp
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The timestamp that that the consent was created")
    @NotNull(message = "Creation timestamp must not be null")
    @Valid
    public OffsetDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(OffsetDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Consent statusUpdatedTimestamp(OffsetDateTime statusUpdatedTimestamp) {
        this.statusUpdatedTimestamp = statusUpdatedTimestamp;
        return this;
    }

    /**
     * The time that the status was last updated. Absent until the status first changes.
     *
     * @return statusUpdatedTimestamp
     **/
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "The time that the status was last updated. Absent until the status first changes.")
    @Valid
    public OffsetDateTime getStatusUpdatedTimestamp() {
        return statusUpdatedTimestamp;
    }

    public void setStatusUpdatedTimestamp(OffsetDateTime statusUpdatedTimestamp) {
        this.statusUpdatedTimestamp = statusUpdatedTimestamp;
    }

    public Consent detail(OneOfconsentDetail detail) {
        this.detail = detail;
        return this;
    }

    /**
     * The consent details
     *
     * @return detail
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The consent details")
    @NotNull(message = "Detail must not be null")
    public OneOfconsentDetail getDetail() {
        return detail;
    }

    public void setDetail(OneOfconsentDetail detail) {
        this.detail = detail;
    }

    public Consent payments(List<Payment> payments) {
        this.payments = payments;
        return this;
    }

    public Consent addPaymentsItem(Payment paymentsItem) {
        this.payments.add(paymentsItem);
        return this;
    }

    /**
     * Payments associated with this consent, if any.
     *
     * @return payments
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Payments associated with this consent, if any.")
    @Valid
    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Consent cardNetwork(CardNetwork cardNetwork) {
        this.cardNetwork = cardNetwork;
        return this;
    }

    /**
     * Get cardNetwork
     *
     * @return cardNetwork
     **/
    @Schema(description = "")
    public CardNetwork getCardNetwork() {
        return cardNetwork;
    }

    public void setCardNetwork(CardNetwork cardNetwork) {
        this.cardNetwork = cardNetwork;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Consent consent = (Consent) o;
        return Objects.equals(this.consentId, consent.consentId)
               && Objects.equals(this.status, consent.status)
               && Objects.equals(this.creationTimestamp, consent.creationTimestamp)
               && Objects.equals(this.statusUpdatedTimestamp, consent.statusUpdatedTimestamp)
               && Objects.equals(this.detail, consent.detail)
               && Objects.equals(this.payments, consent.payments)
               && Objects.equals(this.cardNetwork, consent.cardNetwork);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consentId, status, creationTimestamp, statusUpdatedTimestamp, detail, payments, cardNetwork);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Consent {\n");
        sb.append("    consentId: ").append(toIndentedString(consentId)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    creationTimestamp: ").append(toIndentedString(creationTimestamp)).append("\n");
        sb.append("    statusUpdatedTimestamp: ").append(toIndentedString(statusUpdatedTimestamp)).append("\n");
        sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
        sb.append("    payments: ").append(toIndentedString(payments)).append("\n");
        sb.append("    cardNetwork: ").append(toIndentedString(cardNetwork)).append("\n");
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
