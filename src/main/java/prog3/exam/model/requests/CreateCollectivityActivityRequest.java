package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.MonthlyRecurrenceRule;
import prog3.exam.model.enums.ActivityType;
import prog3.exam.model.enums.MemberOccupation;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateCollectivityActivityRequest {
    private String label;
    private ActivityType activityType;
    private List<MemberOccupation> memberOccupationConcerned;
    private MonthlyRecurrenceRule recurrenceRule;
    private LocalDate executiveDate;
}
