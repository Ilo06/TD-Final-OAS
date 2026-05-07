package prog3.exam.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateMembershipFee {

    public LocalDate eligibleFrom;
    public Frequency frequency;
    public BigDecimal amount;
    public String label;
}