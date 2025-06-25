
package ch.ruyalabs.springkafkalabs.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;


/**
 * SuccessDataDto
 * <p>
 * Success data for a payment response
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "transactionId",
    "processedAmount",
    "processedAt",
    "status"
})
@Generated("jsonschema2pojo")
public class SuccessDataDto {

    /**
     * Transaction identifier from the payment processor
     * (Required)
     * 
     */
    @JsonProperty("transactionId")
    @JsonPropertyDescription("Transaction identifier from the payment processor")
    @NotNull
    private String transactionId;
    /**
     * The actual amount that was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAmount")
    @JsonPropertyDescription("The actual amount that was processed")
    @NotNull
    private Double processedAmount;
    /**
     * Timestamp when the payment was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAt")
    @JsonPropertyDescription("Timestamp when the payment was processed")
    @NotNull
    private Date processedAt;
    /**
     * Payment status
     * (Required)
     * 
     */
    @JsonProperty("status")
    @JsonPropertyDescription("Payment status")
    @NotNull
    private SuccessDataDto.Status status;

    /**
     * No args constructor for use in serialization
     * 
     */
    public SuccessDataDto() {
    }

    /**
     * 
     * @param processedAmount
     *     The actual amount that was processed.
     * @param processedAt
     *     Timestamp when the payment was processed.
     * @param transactionId
     *     Transaction identifier from the payment processor.
     * @param status
     *     Payment status.
     */
    public SuccessDataDto(String transactionId, Double processedAmount, Date processedAt, SuccessDataDto.Status status) {
        super();
        this.transactionId = transactionId;
        this.processedAmount = processedAmount;
        this.processedAt = processedAt;
        this.status = status;
    }

    /**
     * Transaction identifier from the payment processor
     * (Required)
     * 
     */
    @JsonProperty("transactionId")
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Transaction identifier from the payment processor
     * (Required)
     * 
     */
    @JsonProperty("transactionId")
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * The actual amount that was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAmount")
    public Double getProcessedAmount() {
        return processedAmount;
    }

    /**
     * The actual amount that was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAmount")
    public void setProcessedAmount(Double processedAmount) {
        this.processedAmount = processedAmount;
    }

    /**
     * Timestamp when the payment was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAt")
    public Date getProcessedAt() {
        return processedAt;
    }

    /**
     * Timestamp when the payment was processed
     * (Required)
     * 
     */
    @JsonProperty("processedAt")
    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    /**
     * Payment status
     * (Required)
     * 
     */
    @JsonProperty("status")
    public SuccessDataDto.Status getStatus() {
        return status;
    }

    /**
     * Payment status
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(SuccessDataDto.Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SuccessDataDto.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("transactionId");
        sb.append('=');
        sb.append(((this.transactionId == null)?"<null>":this.transactionId));
        sb.append(',');
        sb.append("processedAmount");
        sb.append('=');
        sb.append(((this.processedAmount == null)?"<null>":this.processedAmount));
        sb.append(',');
        sb.append("processedAt");
        sb.append('=');
        sb.append(((this.processedAt == null)?"<null>":this.processedAt));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
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
        result = ((result* 31)+((this.processedAt == null)? 0 :this.processedAt.hashCode()));
        result = ((result* 31)+((this.transactionId == null)? 0 :this.transactionId.hashCode()));
        result = ((result* 31)+((this.processedAmount == null)? 0 :this.processedAmount.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SuccessDataDto) == false) {
            return false;
        }
        SuccessDataDto rhs = ((SuccessDataDto) other);
        return (((((this.processedAt == rhs.processedAt)||((this.processedAt!= null)&&this.processedAt.equals(rhs.processedAt)))&&((this.transactionId == rhs.transactionId)||((this.transactionId!= null)&&this.transactionId.equals(rhs.transactionId))))&&((this.processedAmount == rhs.processedAmount)||((this.processedAmount!= null)&&this.processedAmount.equals(rhs.processedAmount))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }


    /**
     * Payment status
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Status {

        COMPLETED("COMPLETED");
        private final String value;
        private final static Map<String, SuccessDataDto.Status> CONSTANTS = new HashMap<String, SuccessDataDto.Status>();

        static {
            for (SuccessDataDto.Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static SuccessDataDto.Status fromValue(String value) {
            SuccessDataDto.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
