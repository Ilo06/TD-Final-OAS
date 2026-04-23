package prog3.exam.model;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CashAccount {
    private String id;
    private double amount;
}
