package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MobileBankingAccount {
    private int id;
    private String holderName;
    private MobileBankingService mobileBankingService;
    private String mobileNumber;
    private double amount;
}
