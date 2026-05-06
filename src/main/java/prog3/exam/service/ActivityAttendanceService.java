package prog3.exam.service;

import org.springframework.stereotype.Service;
import prog3.exam.exception.BadRequestException;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.ActivityMemberAttendance;
import prog3.exam.model.Member;
import prog3.exam.model.MemberDescription;
import prog3.exam.model.enums.AttendanceStatus;
import prog3.exam.model.requests.CreateActivityMemberAttendanceRequest;
import prog3.exam.repository.ActivityAttendanceRepository;
import prog3.exam.repository.CollectivityRepository;
import prog3.exam.repository.MemberRepository;

import java.util.*;

@Service
public class ActivityAttendanceService {

    private final ActivityAttendanceRepository attendanceRepository;
    private final CollectivityRepository collectivityRepository;
    private final MemberRepository memberRepository;

    public ActivityAttendanceService(ActivityAttendanceRepository attendanceRepository,
                                     CollectivityRepository collectivityRepository,
                                     MemberRepository memberRepository) {
        this.attendanceRepository = attendanceRepository;
        this.collectivityRepository = collectivityRepository;
        this.memberRepository = memberRepository;
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    public List<ActivityMemberAttendance> confirmAttendance(
            String collectivityId,
            String activityId,
            List<CreateActivityMemberAttendanceRequest> requests) {

        validateCollectivityAndActivity(collectivityId, activityId);

        List<ActivityMemberAttendance> result = new ArrayList<>();

        for (CreateActivityMemberAttendanceRequest req : requests) {
            String memberId = req.getMemberIdentifier();
            AttendanceStatus newStatus = req.getAttendanceStatus();

            if (memberId == null || newStatus == null) {
                throw new BadRequestException("memberIdentifier and attendanceStatus are required.");
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NotFoundException("Member not found: " + memberId));

            AttendanceStatus existing = attendanceRepository.findStatus(activityId, memberId);

            if (existing != null && existing != AttendanceStatus.UNDEFINED) {
                // Already confirmed as ATTENDED or MISSING — cannot change
                throw new BadRequestException(
                        "Attendance for member " + memberId + " is already confirmed as "
                        + existing + " and cannot be modified.");
            }

            String rowId;
            if (existing == null) {
                // No record yet — insert
                rowId = attendanceRepository.insert(activityId, memberId, newStatus);
            } else {
                // Existing UNDEFINED — update
                attendanceRepository.update(activityId, memberId, newStatus);
                rowId = UUID.randomUUID().toString(); // local placeholder; real id already in DB
            }

            result.add(ActivityMemberAttendance.builder()
                    .id(rowId)
                    .memberDescription(toDescription(member))
                    .attendanceStatus(newStatus)
                    .build());
        }

        return result;
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    /**
     * Returns the full attendance list for an activity:
     *
     * - Collectivity members whose occupation is concerned by the activity →
     *   ATTENDED / MISSING / UNDEFINED (defaulting to UNDEFINED if not yet recorded)
     * - All other members (outside collectivity, or not in concerned occupations) →
     *   only shown if they have an explicit ATTENDED record
     */
    public List<ActivityMemberAttendance> getAttendance(String collectivityId, String activityId) {
        validateCollectivityAndActivity(collectivityId, activityId);

        // All explicitly recorded rows
        Map<String, ActivityAttendanceRepository.RecordedAttendance> recorded =
                attendanceRepository.findAllRecorded(activityId);

        // Concerned members of the collectivity
        List<String> concernedIds = attendanceRepository.findConcernedMemberIds(collectivityId, activityId);
        Set<String> concernedSet = new HashSet<>(concernedIds);

        List<ActivityMemberAttendance> result = new ArrayList<>();

        // 1. Concerned collectivity members — always appear (UNDEFINED if not yet recorded)
        for (String memberId : concernedIds) {
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null) continue;

            ActivityAttendanceRepository.RecordedAttendance rec = recorded.get(memberId);
            AttendanceStatus status = rec != null ? rec.status() : AttendanceStatus.UNDEFINED;
            String rowId = rec != null ? rec.id() : null;

            result.add(ActivityMemberAttendance.builder()
                    .id(rowId)
                    .memberDescription(toDescription(member))
                    .attendanceStatus(status)
                    .build());
        }

        // 2. Other members (outside collectivity or not in concerned occupations)
        //    — only shown if explicitly ATTENDED
        for (Map.Entry<String, ActivityAttendanceRepository.RecordedAttendance> entry : recorded.entrySet()) {
            String memberId = entry.getKey();
            if (concernedSet.contains(memberId)) continue; // already handled above

            ActivityAttendanceRepository.RecordedAttendance rec = entry.getValue();
            if (rec.status() != AttendanceStatus.ATTENDED) continue; // only ATTENDED for outsiders

            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null) continue;

            result.add(ActivityMemberAttendance.builder()
                    .id(rec.id())
                    .memberDescription(toDescription(member))
                    .attendanceStatus(AttendanceStatus.ATTENDED)
                    .build());
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void validateCollectivityAndActivity(String collectivityId, String activityId) {
        if (!collectivityRepository.existsById(collectivityId)) {
            throw new NotFoundException("Collectivity not found: " + collectivityId);
        }
        if (!attendanceRepository.activityBelongsToCollectivity(activityId, collectivityId)) {
            throw new NotFoundException("Activity not found: " + activityId
                    + " in collectivity " + collectivityId);
        }
    }

    private MemberDescription toDescription(Member member) {
        return MemberDescription.builder()
                .id(member.getId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .occupation(member.getOccupation())
                .build();
    }
}
