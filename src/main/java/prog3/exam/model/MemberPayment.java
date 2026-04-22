package prog3.exam.model;

import prog3.exam.model.enums.PaymentMode;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPayment {
    private int id;
    private int amount;
    private PaymentMode paymentMode;
    private Object accountCredited;
    private LocalDate creationDate;
}
