package prog3.exam.model;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CashAccount {
    private int id;
    private int amount;
}
