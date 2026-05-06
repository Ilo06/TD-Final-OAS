package prog3.exam.model.requests;

import lombok.Data;
import prog3.exam.model.enums.AttendanceStatus;

@Data
public class CreateActivityMemberAttendanceRequest {
    private String memberIdentifier;
    private AttendanceStatus attendanceStatus;
}
