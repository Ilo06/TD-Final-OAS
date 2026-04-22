package prog3.exam.model;

import prog3.exam.model.enums.ActivityStatus;
import prog3.exam.model.enums.Frequency;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipFee {
    private int id;
    private LocalDate eligibleFrom;
    private Frequency frequency;
    private double amount;
    private String label;
    private ActivityStatus status;
}
