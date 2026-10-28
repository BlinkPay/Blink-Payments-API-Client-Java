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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * The detailed error response.
 */
@Schema(description = "The detailed error response.")
@Validated
@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-01-21T00:34:29.984Z[GMT]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailErrorResponseModel {

    @JsonProperty("timestamp")
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime timestamp = null;

    @JsonProperty("status")
    private Integer status = null;

    @JsonProperty("error")
    private String error = null;

    @JsonProperty("message")
    private String message = null;

    @JsonProperty("path")
    private String path = null;

    @JsonProperty("code")
    private String code = null;

    public DetailErrorResponseModel timestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * The error timestamp.
     *
     * @return timestamp
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The error timestamp.")
    @NotNull
    @Valid
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public DetailErrorResponseModel status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * The status code.
     *
     * @return status
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The status code.")
    @NotNull
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public DetailErrorResponseModel error(String error) {
        this.error = error;
        return this;
    }

    /**
     * The title of the error code.
     *
     * @return error
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The title of the error code.")
    @NotNull
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public DetailErrorResponseModel message(String message) {
        this.message = message;
        return this;
    }

    /**
     * The error detail.
     *
     * @return message
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The error detail.")
    @NotNull
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DetailErrorResponseModel path(String path) {
        this.path = path;
        return this;
    }

    /**
     * The requested path when the error was triggered.
     *
     * @return path
     **/
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The requested path when the error was triggered.")
    @NotNull
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DetailErrorResponseModel code(String code) {
        this.code = code;
        return this;
    }

    /**
     * A code supplied by BlinkPay to reference the error type
     *
     * @return code
     **/
    @Schema(description = "A code supplied by BlinkPay to reference the error type")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DetailErrorResponseModel detailErrorResponseModel = (DetailErrorResponseModel) o;
        return Objects.equals(this.timestamp, detailErrorResponseModel.timestamp)
                && Objects.equals(this.status, detailErrorResponseModel.status)
                && Objects.equals(this.error, detailErrorResponseModel.error)
                && Objects.equals(this.message, detailErrorResponseModel.message)
                && Objects.equals(this.path, detailErrorResponseModel.path)
                && Objects.equals(this.code, detailErrorResponseModel.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, status, error, message, path, code);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DetailErrorResponseModel {\n");
        sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    error: ").append(toIndentedString(error)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
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
