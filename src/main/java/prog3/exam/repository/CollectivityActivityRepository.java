package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.CollectivityActivity;
import prog3.exam.model.MonthlyRecurrenceRule;
import prog3.exam.model.enums.ActivityType;
import prog3.exam.model.enums.DayOfWeek;
import prog3.exam.model.enums.MemberOccupation;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Repository
public class CollectivityActivityRepository {

    private final DataSourceConfig dataSourceConfig;

    public CollectivityActivityRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    // ── DDL helpers (call once at startup or via schema SQL) ─────────────────

    private static final String INSERT_ACTIVITY = """
            INSERT INTO collectivity_activity
                (id, label, activity_type, recurrence_week_ordinal, recurrence_day_of_week,
                 executive_date, collectivity_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_OCCUPATION = """
            INSERT INTO collectivity_activity_occupation (activity_id, occupation)
            VALUES (?, ?)
            """;

    private static final String FIND_BY_COLLECTIVITY = """
            SELECT id, label, activity_type, recurrence_week_ordinal, recurrence_day_of_week,
                   executive_date
            FROM collectivity_activity
            WHERE collectivity_id = ?
            """;

    private static final String FIND_OCCUPATIONS_BY_ACTIVITY = """
            SELECT occupation FROM collectivity_activity_occupation WHERE activity_id = ?
            """;

    // ── Public API ───────────────────────────────────────────────────────────

    public CollectivityActivity save(String collectivityId,
                                     String activityId,
                                     prog3.exam.model.requests.CreateCollectivityActivityRequest req) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ACTIVITY)) {
            ps.setString(1, activityId);
            ps.setString(2, req.getLabel());
            ps.setString(3, req.getActivityType() != null ? req.getActivityType().name() : null);

            // recurrence rule fields
            if (req.getRecurrenceRule() != null) {
                ps.setObject(4, req.getRecurrenceRule().getWeekOrdinal());
                ps.setString(5, req.getRecurrenceRule().getDayOfWeek() != null
                        ? req.getRecurrenceRule().getDayOfWeek().name() : null);
            } else {
                ps.setNull(4, Types.INTEGER);
                ps.setNull(5, Types.VARCHAR);
            }

            // executive date
            if (req.getExecutiveDate() != null) {
                ps.setDate(6, Date.valueOf(req.getExecutiveDate()));
            } else {
                ps.setNull(6, Types.DATE);
            }

            ps.setString(7, collectivityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }

        // Save occupations
        if (req.getMemberOccupationConcerned() != null) {
            for (MemberOccupation occ : req.getMemberOccupationConcerned()) {
                saveOccupation(activityId, occ);
            }
        }

        return CollectivityActivity.builder()
                .id(activityId)
                .label(req.getLabel())
                .activityType(req.getActivityType())
                .memberOccupationConcerned(req.getMemberOccupationConcerned())
                .recurrenceRule(req.getRecurrenceRule())
                .executiveDate(req.getExecutiveDate())
                .build();
    }

    private void saveOccupation(String activityId, MemberOccupation occupation) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_OCCUPATION)) {
            ps.setString(1, activityId);
            ps.setString(2, occupation.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<CollectivityActivity> findByCollectivityId(String collectivityId) {
        List<CollectivityActivity> activities = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    List<MemberOccupation> occupations = findOccupationsOf(id);

                    String typeStr = rs.getString("activity_type");
                    String dayStr = rs.getString("recurrence_day_of_week");
                    Object weekOrdObj = rs.getObject("recurrence_week_ordinal");
                    Date execDate = rs.getDate("executive_date");

                    MonthlyRecurrenceRule rule = null;
                    if (weekOrdObj != null || dayStr != null) {
                        rule = MonthlyRecurrenceRule.builder()
                                .weekOrdinal(weekOrdObj != null ? (Integer) weekOrdObj : null)
                                .dayOfWeek(dayStr != null ? DayOfWeek.valueOf(dayStr) : null)
                                .build();
                    }

                    activities.add(CollectivityActivity.builder()
                            .id(id)
                            .label(rs.getString("label"))
                            .activityType(typeStr != null ? ActivityType.valueOf(typeStr) : null)
                            .memberOccupationConcerned(occupations)
                            .recurrenceRule(rule)
                            .executiveDate(execDate != null ? execDate.toLocalDate() : null)
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return activities;
    }

    private List<MemberOccupation> findOccupationsOf(String activityId) {
        List<MemberOccupation> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_OCCUPATIONS_BY_ACTIVITY)) {
            ps.setString(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String occ = rs.getString("occupation");
                    if (occ != null) list.add(MemberOccupation.valueOf(occ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return list;
    }

    public boolean existsById(String activityId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM collectivity_activity WHERE id = ?")) {
            ps.setString(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}
