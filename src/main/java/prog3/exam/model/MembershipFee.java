package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.ActivityStatus;
import prog3.exam.model.enums.Frequency;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MembershipFee {
    private int id;
    private LocalDate eligibleFrom;
    private Frequency frequency;
    private double amount;
    private String label;
    private ActivityStatus status;
}
