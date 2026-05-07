package prog3.exam.api.model;

import java.time.LocalDate;

public class MemberPayment {

    public String id;
    public Integer amount;
    public PaymentMode paymentMode;
    //public FinancialAccount accountCredited;
    public LocalDate creationDate;

    @Override
    public String toString() {
        return "MemberPayment{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", paymentMode=" + paymentMode +
                //", accountCredited=" + accountCredited +
                ", creationDate=" + creationDate +
                '}';
    }
}