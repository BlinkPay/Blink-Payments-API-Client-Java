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
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nz.co.blink.debit.helpers.CustomOffsetDateTimeDeserializer;
import nz.co.blink.debit.helpers.CustomOffsetDateTimeSerializer;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The model for an enduring consent request, relating to multiple payments.
 */
@Schema(description = "The model for an enduring consent request, relating to multiple payments.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-04-22T17:59:56.975143+12:00[Pacific/Auckland]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnduringConsentRequest extends ConsentDetail implements OneOfconsentDetail {

    @JsonProperty("flow")
    private AuthFlow flow = null;

    @JsonProperty("expiry_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime expiryTimestamp = null;

    @JsonProperty("from_timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime fromTimestamp = null;

    @JsonProperty("maximum_amount_period")
    private Amount maximumAmountPeriod = null;

    @JsonProperty("period")
    private Period period = null;

    @JsonProperty("hashed_customer_identifier")
    private String hashedCustomerIdentifier = null;

    @JsonProperty("maximum_amount_payment")
    private Amount maximumAmountPayment = null;

    public EnduringConsentRequest flow(AuthFlow flow) {
        this.flow = flow;
        return this;
    }

    /**
     * Get flow
     *
     * @return flow
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Authorization flow must not be null")
    @Valid
    public AuthFlow getFlow() {
        return flow;
    }

    public void setFlow(AuthFlow flow) {
        this.flow = flow;
    }

    public EnduringConsentRequest expiryTimestamp(OffsetDateTime expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
        return this;
    }

    /**
     * The ISO 8601 timeout for when an enduring consent will expire. If this field is blank, an indefinite request will be attempted.
     *
     * @return expiryTimestamp
     **/
    @Schema(example = "2021-12-01T00:00+13:00", description = "The ISO 8601 timeout for when an enduring consent will expire. If this field is blank, an indefinite request will be attempted.")
    @Valid
    public OffsetDateTime getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(OffsetDateTime expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public EnduringConsentRequest fromTimestamp(OffsetDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
        return this;
    }

    /**
     * The ISO 8601 start date to calculate the periods for which to calculate the consent period.
     *
     * @return fromTimestamp
     **/
    @Schema(example = "2020-12-01T00:00+13:00", requiredMode = Schema.RequiredMode.REQUIRED, description = "The ISO 8601 start date to calculate the periods for which to calculate the consent period.")
    @NotNull(message = "From timestamp must not be null")
    @Valid
    public OffsetDateTime getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(OffsetDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
    }

    public EnduringConsentRequest maximumAmountPeriod(Amount maximumAmountPeriod) {
        this.maximumAmountPeriod = maximumAmountPeriod;
        return this;
    }

    /**
     * Get maximumAmountPeriod
     *
     * @return maximumAmountPeriod
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Maximum amount period must not be null")
    @Valid
    public Amount getMaximumAmountPeriod() {
        return maximumAmountPeriod;
    }

    public void setMaximumAmountPeriod(Amount maximumAmountPeriod) {
        this.maximumAmountPeriod = maximumAmountPeriod;
    }

    public EnduringConsentRequest period(Period period) {
        this.period = period;
        return this;
    }

    /**
     * Get period
     *
     * @return period
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Period must not be null")
    @Valid
    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public EnduringConsentRequest hashedCustomerIdentifier(String hashedCustomerIdentifier) {
        this.hashedCustomerIdentifier = hashedCustomerIdentifier;
        return this;
    }

    /**
     * The hashed unique ID of the customer e.g. customer internal ID. SHA-256 is recommended.
     *
     * @return hashedCustomerIdentifier
     **/
    @Schema(description = "The hashed unique ID of the customer e.g. customer internal ID. SHA-256 is recommended.")
    public String getHashedCustomerIdentifier() {
        return hashedCustomerIdentifier;
    }

    public void setHashedCustomerIdentifier(String hashedCustomerIdentifier) {
        this.hashedCustomerIdentifier = hashedCustomerIdentifier;
    }

    public EnduringConsentRequest maximumAmountPayment(Amount maximumAmountPayment) {
        this.maximumAmountPayment = maximumAmountPayment;
        return this;
    }

    /**
     * Get maximumAmountPayment
     *
     * @return maximumAmountPayment
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Maximum amount payment must not be null")
    @Valid
    public Amount getMaximumAmountPayment() {
        return maximumAmountPayment;
    }

    public void setMaximumAmountPayment(Amount maximumAmountPayment) {
        this.maximumAmountPayment = maximumAmountPayment;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.ENDURING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnduringConsentRequest enduringConsentRequest = (EnduringConsentRequest) o;
        return Objects.equals(this.flow, enduringConsentRequest.flow)
               && Objects.equals(this.expiryTimestamp, enduringConsentRequest.expiryTimestamp)
               && Objects.equals(this.fromTimestamp, enduringConsentRequest.fromTimestamp)
               && Objects.equals(this.maximumAmountPeriod, enduringConsentRequest.maximumAmountPeriod)
               && Objects.equals(this.period, enduringConsentRequest.period)
               && Objects.equals(this.hashedCustomerIdentifier, enduringConsentRequest.hashedCustomerIdentifier)
               && Objects.equals(this.maximumAmountPayment, enduringConsentRequest.maximumAmountPayment)
               && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flow, expiryTimestamp, fromTimestamp, maximumAmountPeriod, period, hashedCustomerIdentifier,
                maximumAmountPayment, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class EnduringConsentRequest {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    flow: ").append(toIndentedString(flow)).append("\n");
        sb.append("    expiryTimestamp: ").append(toIndentedString(expiryTimestamp)).append("\n");
        sb.append("    fromTimestamp: ").append(toIndentedString(fromTimestamp)).append("\n");
        sb.append("    maximumAmountPeriod: ").append(toIndentedString(maximumAmountPeriod)).append("\n");
        sb.append("    period: ").append(toIndentedString(period)).append("\n");
        sb.append("    hashedCustomerIdentifier: ").append(toIndentedString(hashedCustomerIdentifier)).append("\n");
        sb.append("    maximumAmountPayment: ").append(toIndentedString(maximumAmountPayment)).append("\n");
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
