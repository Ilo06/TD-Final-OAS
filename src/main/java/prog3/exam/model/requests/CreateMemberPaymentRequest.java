package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.enums.PaymentMode;

@Data
public class CreateMemberPaymentRequest {
    private Integer amount;
    private String membershipFeeIdentifier;
    private String accountCreditedIdentifier;
    private PaymentMode paymentMode;
}
