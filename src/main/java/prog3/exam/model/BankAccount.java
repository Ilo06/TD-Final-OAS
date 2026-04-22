package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.Bank;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankAccount {
    private int id;
    private String holderName;
    private Bank bankName;
    private int bankCode;
    private int bankBranchCode;
    private long bankAccountNumber;
    private int bankAccountKey;
    private double amount;
}
