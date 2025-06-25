
package ch.ruyalabs.springkafkalabs.dto;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


/**
 * PaymentResponseDto
 * <p>
 * A payment response message containing either success or error data
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "paymentId",
    "successData",
    "errorData"
})
@Generated("jsonschema2pojo")
public class PaymentResponseDto {

    /**
     * Unique identifier for the payment request
     * (Required)
     * 
     */
    @JsonProperty("paymentId")
    @JsonPropertyDescription("Unique identifier for the payment request")
    @NotNull
    private String paymentId;
    /**
     * SuccessDataDto
     * <p>
     * Success data for a payment response
     * 
     */
    @JsonProperty("successData")
    @JsonPropertyDescription("Success data for a payment response")
    @Valid
    private SuccessDataDto successData;
    /**
     * ErrorDataDto
     * <p>
     * Error data for a payment response
     * 
     */
    @JsonProperty("errorData")
    @JsonPropertyDescription("Error data for a payment response")
    @Valid
    private ErrorDataDto errorData;

    /**
     * No args constructor for use in serialization
     * 
     */
    public PaymentResponseDto() {
    }

    /**
     * 
     * @param paymentId
     *     Unique identifier for the payment request.
     */
    public PaymentResponseDto(String paymentId) {
        super();
        this.paymentId = paymentId;
    }

    /**
     * Unique identifier for the payment request
     * (Required)
     * 
     */
    @JsonProperty("paymentId")
    public String getPaymentId() {
        return paymentId;
    }

    /**
     * Unique identifier for the payment request
     * (Required)
     * 
     */
    @JsonProperty("paymentId")
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    /**
     * SuccessDataDto
     * <p>
     * Success data for a payment response
     * 
     */
    @JsonProperty("successData")
    public SuccessDataDto getSuccessData() {
        return successData;
    }

    /**
     * SuccessDataDto
     * <p>
     * Success data for a payment response
     * 
     */
    @JsonProperty("successData")
    public void setSuccessData(SuccessDataDto successData) {
        this.successData = successData;
    }

    /**
     * ErrorDataDto
     * <p>
     * Error data for a payment response
     * 
     */
    @JsonProperty("errorData")
    public ErrorDataDto getErrorData() {
        return errorData;
    }

    /**
     * ErrorDataDto
     * <p>
     * Error data for a payment response
     * 
     */
    @JsonProperty("errorData")
    public void setErrorData(ErrorDataDto errorData) {
        this.errorData = errorData;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PaymentResponseDto.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("paymentId");
        sb.append('=');
        sb.append(((this.paymentId == null)?"<null>":this.paymentId));
        sb.append(',');
        sb.append("successData");
        sb.append('=');
        sb.append(((this.successData == null)?"<null>":this.successData));
        sb.append(',');
        sb.append("errorData");
        sb.append('=');
        sb.append(((this.errorData == null)?"<null>":this.errorData));
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
        result = ((result* 31)+((this.successData == null)? 0 :this.successData.hashCode()));
        result = ((result* 31)+((this.paymentId == null)? 0 :this.paymentId.hashCode()));
        result = ((result* 31)+((this.errorData == null)? 0 :this.errorData.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PaymentResponseDto) == false) {
            return false;
        }
        PaymentResponseDto rhs = ((PaymentResponseDto) other);
        return ((((this.successData == rhs.successData)||((this.successData!= null)&&this.successData.equals(rhs.successData)))&&((this.paymentId == rhs.paymentId)||((this.paymentId!= null)&&this.paymentId.equals(rhs.paymentId))))&&((this.errorData == rhs.errorData)||((this.errorData!= null)&&this.errorData.equals(rhs.errorData))));
    }

}
