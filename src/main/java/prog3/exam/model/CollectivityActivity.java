package prog3.exam.model;

import lombok.*;
import prog3.exam.model.enums.ActivityType;
import prog3.exam.model.enums.MemberOccupation;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectivityActivity {
    private String id;
    private String label;
    private ActivityType activityType;
    private List<MemberOccupation> memberOccupationConcerned;
    private MonthlyRecurrenceRule recurrenceRule;
    private LocalDate executiveDate;
}
