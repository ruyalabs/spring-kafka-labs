
package ch.ruyalabs.springkafkalabs.dto;

import java.util.Date;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;


/**
 * ErrorDataDto
 * <p>
 * Error data for a payment response
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "errorCode",
    "errorMessage",
    "errorDetails",
    "occurredAt"
})
@Generated("jsonschema2pojo")
public class ErrorDataDto {

    /**
     * Error code identifying the type of error
     * (Required)
     * 
     */
    @JsonProperty("errorCode")
    @JsonPropertyDescription("Error code identifying the type of error")
    @NotNull
    private String errorCode;
    /**
     * Human-readable error message
     * (Required)
     * 
     */
    @JsonProperty("errorMessage")
    @JsonPropertyDescription("Human-readable error message")
    @NotNull
    private String errorMessage;
    /**
     * Additional error details for debugging
     * 
     */
    @JsonProperty("errorDetails")
    @JsonPropertyDescription("Additional error details for debugging")
    private String errorDetails;
    /**
     * Timestamp when the error occurred
     * (Required)
     * 
     */
    @JsonProperty("occurredAt")
    @JsonPropertyDescription("Timestamp when the error occurred")
    @NotNull
    private Date occurredAt;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ErrorDataDto() {
    }

    /**
     * 
     * @param occurredAt
     *     Timestamp when the error occurred.
     * @param errorMessage
     *     Human-readable error message.
     * @param errorCode
     *     Error code identifying the type of error.
     */
    public ErrorDataDto(String errorCode, String errorMessage, Date occurredAt) {
        super();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.occurredAt = occurredAt;
    }

    /**
     * Error code identifying the type of error
     * (Required)
     * 
     */
    @JsonProperty("errorCode")
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Error code identifying the type of error
     * (Required)
     * 
     */
    @JsonProperty("errorCode")
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Human-readable error message
     * (Required)
     * 
     */
    @JsonProperty("errorMessage")
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Human-readable error message
     * (Required)
     * 
     */
    @JsonProperty("errorMessage")
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Additional error details for debugging
     * 
     */
    @JsonProperty("errorDetails")
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * Additional error details for debugging
     * 
     */
    @JsonProperty("errorDetails")
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    /**
     * Timestamp when the error occurred
     * (Required)
     * 
     */
    @JsonProperty("occurredAt")
    public Date getOccurredAt() {
        return occurredAt;
    }

    /**
     * Timestamp when the error occurred
     * (Required)
     * 
     */
    @JsonProperty("occurredAt")
    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ErrorDataDto.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("errorCode");
        sb.append('=');
        sb.append(((this.errorCode == null)?"<null>":this.errorCode));
        sb.append(',');
        sb.append("errorMessage");
        sb.append('=');
        sb.append(((this.errorMessage == null)?"<null>":this.errorMessage));
        sb.append(',');
        sb.append("errorDetails");
        sb.append('=');
        sb.append(((this.errorDetails == null)?"<null>":this.errorDetails));
        sb.append(',');
        sb.append("occurredAt");
        sb.append('=');
        sb.append(((this.occurredAt == null)?"<null>":this.occurredAt));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.errorMessage == null)? 0 :this.errorMessage.hashCode()));
        result = ((result* 31)+((this.errorCode == null)? 0 :this.errorCode.hashCode()));
        result = ((result* 31)+((this.occurredAt == null)? 0 :this.occurredAt.hashCode()));
        result = ((result* 31)+((this.errorDetails == null)? 0 :this.errorDetails.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ErrorDataDto) == false) {
            return false;
        }
        ErrorDataDto rhs = ((ErrorDataDto) other);
        return (((((this.errorMessage == rhs.errorMessage)||((this.errorMessage!= null)&&this.errorMessage.equals(rhs.errorMessage)))&&((this.errorCode == rhs.errorCode)||((this.errorCode!= null)&&this.errorCode.equals(rhs.errorCode))))&&((this.occurredAt == rhs.occurredAt)||((this.occurredAt!= null)&&this.occurredAt.equals(rhs.occurredAt))))&&((this.errorDetails == rhs.errorDetails)||((this.errorDetails!= null)&&this.errorDetails.equals(rhs.errorDetails))));
    }

}
