package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.DayOfWeek;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRecurrenceRule {
    private Integer weekOrdinal;  // 1–5: week position in a month
    private DayOfWeek dayOfWeek;  // MO, TU, WE, TH, FR, SA, SU
}
