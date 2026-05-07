package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.enums.AttendanceStatus;
import prog3.exam.model.enums.MemberOccupation;

import java.sql.*;
import java.util.*;

@Repository
public class ActivityAttendanceRepository {

    private final DataSourceConfig dataSourceConfig;

    public ActivityAttendanceRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    private static final String FIND_EXISTING =
            "SELECT id, attendance_status FROM activity_member_attendance WHERE activity_id = ? AND member_id = ?";

    private static final String INSERT =
            "INSERT INTO activity_member_attendance (id, activity_id, member_id, attendance_status) VALUES (?, ?, ?, ?::attendance_status_enum)";

    private static final String UPDATE =
            "UPDATE activity_member_attendance SET attendance_status = ?::attendance_status_enum WHERE activity_id = ? AND member_id = ?";

    private static final String FIND_ALL_BY_ACTIVITY =
            "SELECT id, member_id, attendance_status FROM activity_member_attendance WHERE activity_id = ?";

    // Members of collectivity concerned by the activity's occupations (or all if no occupation filter)
    private static final String FIND_CONCERNED_MEMBER_IDS = """
            SELECT m.id FROM member m
            WHERE m.collectivity_id = ?
            AND (
                NOT EXISTS (
                    SELECT 1 FROM collectivity_activity_occupation cao WHERE cao.activity_id = ?
                )
                OR m.occupation IN (
                    SELECT cao.occupation FROM collectivity_activity_occupation cao WHERE cao.activity_id = ?
                )
            )
            """;

    private static final String ACTIVITY_EXISTS_IN_COLLECTIVITY =
            "SELECT COUNT(*) FROM collectivity_activity WHERE id = ? AND collectivity_id = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    public boolean activityBelongsToCollectivity(String activityId, String collectivityId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(ACTIVITY_EXISTS_IN_COLLECTIVITY)) {
            ps.setString(1, activityId);
            ps.setString(2, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * Returns the current attendance status for a member at an activity, or null if not recorded yet.
     */
    public AttendanceStatus findStatus(String activityId, String memberId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_EXISTING)) {
            ps.setString(1, activityId);
            ps.setString(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String s = rs.getString("attendance_status");
                return s != null ? AttendanceStatus.valueOf(s) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * Insert a new attendance record.
     */
    public String insert(String activityId, String memberId, AttendanceStatus status) {
        String id = UUID.randomUUID().toString();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, id);
            ps.setString(2, activityId);
            ps.setString(3, memberId);
            ps.setString(4, status.name());
            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * Update an existing UNDEFINED record to a new status.
     */
    public void update(String activityId, String memberId, AttendanceStatus status) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, status.name());
            ps.setString(2, activityId);
            ps.setString(3, memberId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * All explicitly recorded attendance rows for this activity.
     * Returns map of memberId -> {rowId, status}
     */
    public Map<String, RecordedAttendance> findAllRecorded(String activityId) {
        Map<String, RecordedAttendance> map = new LinkedHashMap<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_BY_ACTIVITY)) {
            ps.setString(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String memberId = rs.getString("member_id");
                    String statusStr = rs.getString("attendance_status");
                    map.put(memberId, new RecordedAttendance(
                            rs.getString("id"),
                            statusStr != null ? AttendanceStatus.valueOf(statusStr) : AttendanceStatus.UNDEFINED
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return map;
    }

    /**
     * Returns IDs of collectivity members who are concerned by this activity
     * (i.e. their occupation is in the activity's occupation filter, or no filter exists).
     */
    public List<String> findConcernedMemberIds(String collectivityId, String activityId) {
        List<String> ids = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_CONCERNED_MEMBER_IDS)) {
            ps.setString(1, collectivityId);
            ps.setString(2, activityId);
            ps.setString(3, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return ids;
    }

    public record RecordedAttendance(String id, AttendanceStatus status) {}
}
