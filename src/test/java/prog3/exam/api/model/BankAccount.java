package prog3.exam.api.model;

import java.math.BigDecimal;

public class BankAccount implements FinancialAccount {

    public String id;
    public String holderName;
    public Bank bankName;
    public Integer bankCode;
    public Integer bankBranchCode;
    public Integer bankAccountNumber;
    public Integer bankAccountKey;
    public BigDecimal amount;
    public String type = "BANK";

    @Override
    public String toString() {
        return "BankAccount{" +
                "id='" + id + '\'' +
                ", holderName='" + holderName + '\'' +
                ", bankName=" + bankName +
                ", bankCode=" + bankCode +
                ", bankBranchCode=" + bankBranchCode +
                ", bankAccountNumber=" + bankAccountNumber +
                ", bankAccountKey=" + bankAccountKey +
                ", amount=" + amount +
                '}';
    }
}