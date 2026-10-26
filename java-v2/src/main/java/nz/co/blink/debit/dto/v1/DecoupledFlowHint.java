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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Decoupled flow hint.
 * <p>
 * NOTE: This is a hand-written class to avoid circular reference issues.
 */
@JsonTypeName("decoupled")
public class DecoupledFlowHint extends GatewayFlowAllOfFlowHint {

    @NotNull
    @Valid
    @JsonProperty("identifier_type")
    private IdentifierType identifierType;

    @NotNull
    @JsonProperty("identifier_value")
    private String identifierValue;

    public DecoupledFlowHint() {
        super();
    }

    public DecoupledFlowHint identifierType(IdentifierType identifierType) {
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

    public DecoupledFlowHint identifierValue(String identifierValue) {
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

    public DecoupledFlowHint bank(Bank bank) {
        this.bank = bank;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecoupledFlowHint that = (DecoupledFlowHint) o;
        return Objects.equals(bank, that.bank) &&
                Objects.equals(identifierType, that.identifierType) &&
                Objects.equals(identifierValue, that.identifierValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bank, identifierType, identifierValue);
    }

    @Override
    public String toString() {
        return "DecoupledFlowHint{" +
                "bank=" + bank +
                ", identifierType=" + identifierType +
                ", identifierValue='" + identifierValue + '\'' +
                '}';
    }
}
