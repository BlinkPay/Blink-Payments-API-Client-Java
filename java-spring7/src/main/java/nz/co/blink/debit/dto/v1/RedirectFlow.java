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
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * The details for a Redirect flow.
 */
@Schema(description = "The details for a Redirect flow.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-07-22T00:54:15.842Z[GMT]")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedirectFlow extends AuthFlowDetail implements OneOfauthFlowDetail {

    @JsonProperty("redirect_uri")
    private String redirectUri = null;

    @JsonProperty("bank")
    private Bank bank = null;

    @JsonProperty("redirect_to_app")
    private Boolean redirectToApp = null;

    public RedirectFlow redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    /**
     * The URI to redirect back to once the consent is completed. App-based workflows may use deep/universal links.  The `cid` (Consent ID) will be added as a URL parameter. If there is an error, an `error` parameter will be appended also.
     *
     * @return redirectUri
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The URI to redirect back to once the consent is completed. App-based workflows may use deep/universal links.")
    @NotNull(message = "Redirect URI must not be null")
    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public RedirectFlow bank(Bank bank) {
        this.bank = bank;
        return this;
    }

    /**
     * Get bank
     *
     * @return bank
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "")
    @NotNull(message = "Bank must not be null")
    @Valid
    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public RedirectFlow redirectToApp(Boolean redirectToApp) {
        this.redirectToApp = redirectToApp;
        return this;
    }

    /**
     * Whether the redirect URI goes back to an app directly. If this value is true, the app will receive code and state parameters with this redirection. The app must pass these through to us at: https://debit.blinkpay.co.nz/bank/1.0/return?state={state}&code={code}, along with other query parameters like error. Applies only to Redirect flow.
     *
     * @return redirectToApp
     **/
    @Schema(description = "Whether the redirect URI goes back to an app directly. If this value is true, the app will receive code and state parameters with this redirection. The app must pass these through to us at: https://debit.blinkpay.co.nz/bank/1.0/return?state={state}&code={code}, along with other query parameters like error. Applies only to Redirect flow.")
    public Boolean getRedirectToApp() {
        return redirectToApp;
    }

    public void setRedirectToApp(Boolean redirectToApp) {
        this.redirectToApp = redirectToApp;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.REDIRECT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedirectFlow redirectFlow = (RedirectFlow) o;
        return Objects.equals(this.redirectUri, redirectFlow.redirectUri)
                && Objects.equals(this.bank, redirectFlow.bank)
                && Objects.equals(this.redirectToApp, redirectFlow.redirectToApp)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(redirectUri, bank, redirectToApp, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RedirectFlow {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
        sb.append("    bank: ").append(toIndentedString(bank)).append("\n");
        sb.append("    redirectToApp: ").append(toIndentedString(redirectToApp)).append("\n");
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
