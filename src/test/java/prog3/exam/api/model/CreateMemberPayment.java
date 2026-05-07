package prog3.exam.api.model;

import java.time.LocalDate;

public class CreateMemberPayment {
    public Integer amount;
    public String membershipFeeIdentifier;
    public String accountCreditedIdentifier;
    public PaymentMode paymentMode;
}