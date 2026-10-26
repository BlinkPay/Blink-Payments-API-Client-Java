/*
 * Copyright (c) 2025 BlinkPay
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
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
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;

/**
 * The details for a Decoupled flow.
 * <p>
 * NOTE: This is a hand-written class to avoid circular reference issues.
 */
@JsonTypeName("decoupled")
public class DecoupledFlow extends AuthFlowDetail {

    @Valid
    @JsonProperty("bank")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private Bank bank;

    @NotNull
    @Valid
    @JsonProperty("identifier_type")
    private IdentifierType identifierType;

    @NotNull
    @JsonProperty("identifier_value")
    private String identifierValue;

    @Valid
    @JsonProperty("callback_url")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private URI callbackUrl;

    public DecoupledFlow() {
    }

    public DecoupledFlow bank(Bank bank) {
        this.bank = bank;
        return this;
    }

    /**
     * Get bank
     *
     * @return bank
     */
    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public DecoupledFlow identifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
        return this;
    }

    /**
     * Get identifierType
     *
     * @return identifierType
     */
    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    public DecoupledFlow identifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
        return this;
    }

    /**
     * The identifier value. Required in requests. In responses it is {@code null} once the consent is no longer
     * awaiting submission or authorisation (it may already be {@code null} once the bank has accepted the request);
     * a {@code consent_id} reference is always returned.
     *
     * @return identifierValue
     */
    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public DecoupledFlow callbackUrl(URI callbackUrl) {
        this.callbackUrl = callbackUrl;
        return this;
    }

    /**
     * A callback URL to call once the consent status has been updated using decoupled flow.
     *
     * @return callbackUrl
     */
    public URI getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(URI callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecoupledFlow that = (DecoupledFlow) o;
        return Objects.equals(bank, that.bank) &&
                Objects.equals(identifierType, that.identifierType) &&
                Objects.equals(identifierValue, that.identifierValue) &&
                Objects.equals(callbackUrl, that.callbackUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bank, identifierType, identifierValue, callbackUrl);
    }

    @Override
    public String toString() {
        return "DecoupledFlow{" +
                "bank=" + bank +
                ", identifierType=" + identifierType +
                ", identifierValue='" + identifierValue + '\'' +
                ", callbackUrl=" + callbackUrl +
                '}';
    }
}
