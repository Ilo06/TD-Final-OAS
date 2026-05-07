package prog3.exam.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CollectivityTransaction {

    public String id;
    public LocalDate creationDate;
    public BigDecimal amount;
    public PaymentMode paymentMode;
    //public FinancialAccount accountCredited;
    public Member memberDebited;

    @Override
    public String toString() {
        return "CollectivityTransaction{" +
                "id='" + id + '\'' +
                ", creationDate=" + creationDate +
                ", amount=" + amount +
                ", paymentMode=" + paymentMode +
                //", accountCredited=" + accountCredited +
                ", memberDebited=" + memberDebited +
                '}';
    }
}