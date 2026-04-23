package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.PaymentMode;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CollectivityTransaction {
    private int id;
    private LocalDate creationDate;
    private double amount;
    private PaymentMode paymentMode;
    private Object accountCredited;
    private Member memberDebited;
}
