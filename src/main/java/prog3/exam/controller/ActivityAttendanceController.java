package prog3.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import prog3.exam.model.ActivityMemberAttendance;
import prog3.exam.model.requests.CreateActivityMemberAttendanceRequest;
import prog3.exam.service.ActivityAttendanceService;

import java.util.List;

@RestController
@RequestMapping("/collectivities/{id}/activities/{activityId}/attendance")
@CrossOrigin(origins = "*")
public class ActivityAttendanceController {

    private final ActivityAttendanceService attendanceService;

    public ActivityAttendanceController(ActivityAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ActivityMemberAttendance> confirmAttendance(
            @PathVariable String id,
            @PathVariable String activityId,
            @RequestBody List<CreateActivityMemberAttendanceRequest> requests) {
        return attendanceService.confirmAttendance(id, activityId, requests);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ActivityMemberAttendance> getAttendance(
            @PathVariable String id,
            @PathVariable String activityId) {
        return attendanceService.getAttendance(id, activityId);
    }
}
