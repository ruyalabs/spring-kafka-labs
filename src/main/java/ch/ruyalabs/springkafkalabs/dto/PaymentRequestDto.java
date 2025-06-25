
package ch.ruyalabs.springkafkalabs.dto;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


/**
 * PaymentRequestDto
 * <p>
 * A payment request message
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "paymentId",
    "isFaulty",
    "amount",
    "currency",
    "description"
})
@Generated("jsonschema2pojo")
public class PaymentRequestDto {

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
     * Flag indicating if this request should trigger an error for testing purposes
     * (Required)
     * 
     */
    @JsonProperty("isFaulty")
    @JsonPropertyDescription("Flag indicating if this request should trigger an error for testing purposes")
    @NotNull
    private Boolean isFaulty;
    /**
     * Payment amount
     * (Required)
     * 
     */
    @JsonProperty("amount")
    @JsonPropertyDescription("Payment amount")
    @NotNull
    private Double amount;
    /**
     * Three-letter currency code (ISO 4217)
     * (Required)
     * 
     */
    @JsonProperty("currency")
    @JsonPropertyDescription("Three-letter currency code (ISO 4217)")
    @Pattern(regexp = "^[A-Z]{3}$")
    @NotNull
    private String currency;
    /**
     * Payment description
     * 
     */
    @JsonProperty("description")
    @JsonPropertyDescription("Payment description")
    private String description;

    /**
     * No args constructor for use in serialization
     * 
     */
    public PaymentRequestDto() {
    }

    /**
     * 
     * @param amount
     *     Payment amount.
     * @param paymentId
     *     Unique identifier for the payment request.
     * @param isFaulty
     *     Flag indicating if this request should trigger an error for testing purposes.
     * @param currency
     *     Three-letter currency code (ISO 4217).
     */
    public PaymentRequestDto(String paymentId, Boolean isFaulty, Double amount, String currency) {
        super();
        this.paymentId = paymentId;
        this.isFaulty = isFaulty;
        this.amount = amount;
        this.currency = currency;
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
     * Flag indicating if this request should trigger an error for testing purposes
     * (Required)
     * 
     */
    @JsonProperty("isFaulty")
    public Boolean getIsFaulty() {
        return isFaulty;
    }

    /**
     * Flag indicating if this request should trigger an error for testing purposes
     * (Required)
     * 
     */
    @JsonProperty("isFaulty")
    public void setIsFaulty(Boolean isFaulty) {
        this.isFaulty = isFaulty;
    }

    /**
     * Payment amount
     * (Required)
     * 
     */
    @JsonProperty("amount")
    public Double getAmount() {
        return amount;
    }

    /**
     * Payment amount
     * (Required)
     * 
     */
    @JsonProperty("amount")
    public void setAmount(Double amount) {
        this.amount = amount;
    }

    /**
     * Three-letter currency code (ISO 4217)
     * (Required)
     * 
     */
    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }

    /**
     * Three-letter currency code (ISO 4217)
     * (Required)
     * 
     */
    @JsonProperty("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Payment description
     * 
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * Payment description
     * 
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PaymentRequestDto.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("paymentId");
        sb.append('=');
        sb.append(((this.paymentId == null)?"<null>":this.paymentId));
        sb.append(',');
        sb.append("isFaulty");
        sb.append('=');
        sb.append(((this.isFaulty == null)?"<null>":this.isFaulty));
        sb.append(',');
        sb.append("amount");
        sb.append('=');
        sb.append(((this.amount == null)?"<null>":this.amount));
        sb.append(',');
        sb.append("currency");
        sb.append('=');
        sb.append(((this.currency == null)?"<null>":this.currency));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
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
        result = ((result* 31)+((this.isFaulty == null)? 0 :this.isFaulty.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.amount == null)? 0 :this.amount.hashCode()));
        result = ((result* 31)+((this.currency == null)? 0 :this.currency.hashCode()));
        result = ((result* 31)+((this.paymentId == null)? 0 :this.paymentId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PaymentRequestDto) == false) {
            return false;
        }
        PaymentRequestDto rhs = ((PaymentRequestDto) other);
        return ((((((this.isFaulty == rhs.isFaulty)||((this.isFaulty!= null)&&this.isFaulty.equals(rhs.isFaulty)))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.amount == rhs.amount)||((this.amount!= null)&&this.amount.equals(rhs.amount))))&&((this.currency == rhs.currency)||((this.currency!= null)&&this.currency.equals(rhs.currency))))&&((this.paymentId == rhs.paymentId)||((this.paymentId!= null)&&this.paymentId.equals(rhs.paymentId))));
    }

}
