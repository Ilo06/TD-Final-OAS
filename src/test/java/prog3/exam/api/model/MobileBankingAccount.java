package prog3.exam.api.model;

import java.math.BigDecimal;

public class MobileBankingAccount implements FinancialAccount {

    public String id;
    public String holderName;
    public MobileBankingService mobileBankingService;
    public Integer mobileNumber;
    public BigDecimal amount;
    public String type = "MOBILE_BANKING";

    @Override
    public String toString() {
        return "MobileBankingAccount{" +
                "id='" + id + '\'' +
                ", holderName='" + holderName + '\'' +
                ", mobileBankingService=" + mobileBankingService +
                ", mobileNumber=" + mobileNumber +
                ", amount=" + amount +
                '}';
    }
}