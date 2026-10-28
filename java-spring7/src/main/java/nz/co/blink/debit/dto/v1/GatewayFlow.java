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
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * The details for a Gateway flow.
 */
@Schema(description = "The details for a Gateway flow.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-11-12T03:11:35.286Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayFlow extends AuthFlowDetail implements OneOfauthFlowDetail {

    @JsonProperty("redirect_uri")
    private String redirectUri = null;

    @JsonProperty("flow_hint")
    private FlowHint flowHint = null;

    public GatewayFlow redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    /**
     * The URL to redirect back to once the payment is completed through the gateway.  The `cid` (Consent ID) will be added as a URL parameter. If there is an error, an `error` parameter will be appended also.
     *
     * @return redirectUri
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The URL to redirect back to once the payment is completed through the gateway.")
    @NotNull(message = "Redirect URI must not be null")
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public GatewayFlow flowHint(FlowHint flowHint) {
        this.flowHint = flowHint;
        return this;
    }

    /**
     * The gateway flow hint
     *
     * @return flowHint
     **/
    @Schema(description = "The gateway flow hint")
    public FlowHint getFlowHint() {
        return flowHint;
    }

    public void setFlowHint(FlowHint flowHint) {
        this.flowHint = flowHint;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.GATEWAY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GatewayFlow gatewayFlow = (GatewayFlow) o;
        return Objects.equals(this.redirectUri, gatewayFlow.redirectUri)
                && Objects.equals(this.flowHint, gatewayFlow.flowHint)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(redirectUri, flowHint, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class GatewayFlow {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
        sb.append("    flowHint: ").append(toIndentedString(flowHint)).append("\n");
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
