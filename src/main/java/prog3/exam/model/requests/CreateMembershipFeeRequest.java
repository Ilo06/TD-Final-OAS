package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.enums.Frequency;
import java.time.LocalDate;

@Data
public class CreateMembershipFeeRequest {
    private String id;
    private LocalDate eligibleFrom;
    private Frequency frequency;
    private Double amount;
    private String label;
}
