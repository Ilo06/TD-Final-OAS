package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FederationStatisticsRepository {

    private final DataSourceConfig dataSourceConfig;

    public FederationStatisticsRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String FIND_ALL_COLLECTIVITIES = """
            SELECT id, number, name FROM collectivity
            """;

    private static final String COUNT_NEW_MEMBERS = """
            SELECT COUNT(*)
            FROM member
            WHERE collectivity_id = ?
              AND adhesion_date BETWEEN ? AND ?
            """;

    private static final String FIND_MEMBER_IDS = """
            SELECT id FROM member WHERE collectivity_id = ?
            """;

    private static final String ACTIVE_FEES = """
            SELECT id, eligible_from, frequency, amount
            FROM membership_fee
            WHERE collectivity_id = ?
              AND status = 'ACTIVE'
              AND (eligible_from IS NULL OR eligible_from <= ?)
            """;

    private static final String PAID_FOR_FEE = """
            SELECT COALESCE(SUM(amount), 0)
            FROM member_payment
            WHERE member_id = ?
              AND membership_fee_id = ?
              AND creation_date BETWEEN ? AND ?
            """;

    // Uses the correct join table: collectivity_activity_occupation
    private static final String COUNT_ACTIVITIES_FOR_COLLECTIVITY = """
            SELECT COUNT(DISTINCT ca.id)
            FROM collectivity_activity ca
            LEFT JOIN collectivity_activity_occupation cao ON ca.id = cao.activity_id
            JOIN member m ON m.collectivity_id = ca.collectivity_id
            WHERE ca.collectivity_id = ?
              AND m.id = ?
              AND (
                  ca.executive_date BETWEEN ? AND ?
                  OR (ca.executive_date IS NULL AND ca.recurrence_week_ordinal IS NOT NULL)
              )
              AND (
                  NOT EXISTS (SELECT 1 FROM collectivity_activity_occupation x WHERE x.activity_id = ca.id)
                  OR cao.occupation = m.occupation
              )
            """;

    private static final String COUNT_ATTENDED_FOR_COLLECTIVITY = """
            SELECT COUNT(DISTINCT ama.activity_id)
            FROM activity_member_attendance ama
            JOIN collectivity_activity ca ON ama.activity_id = ca.id
            WHERE ama.member_id = ?
              AND ca.collectivity_id = ?
              AND ama.attendance_status = 'ATTENDED'
              AND (
                  ca.executive_date BETWEEN ? AND ?
                  OR (ca.executive_date IS NULL AND ca.recurrence_week_ordinal IS NOT NULL)
              )
            """;

    public record CollectivityRow(String id, Integer number, String name) {}

    public List<CollectivityRow> findAllCollectivities() {
        List<CollectivityRow> rows = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_COLLECTIVITIES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object numObj = rs.getObject("number");
                rows.add(new CollectivityRow(
                        rs.getString("id"),
                        numObj != null ? (Integer) numObj : null,
                        rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return rows;
    }

    public int countNewMembers(String collectivityId, LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_NEW_MEMBERS)) {
            ps.setString(1, collectivityId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public double computeCurrentDuePercentage(String collectivityId, LocalDate from, LocalDate to) {
        List<String> memberIds = findMemberIds(collectivityId);
        if (memberIds.isEmpty()) return 0.0;

        List<FeeRow> activeFees = findActiveFees(collectivityId, to);
        if (activeFees.isEmpty()) return 100.0;

        Connection conn = dataSourceConfig.getConnection();
        try {
            int upToDateCount = 0;
            for (String memberId : memberIds) {
                if (isMemberUpToDate(conn, memberId, activeFees, from, to)) {
                    upToDateCount++;
                }
            }
            return (upToDateCount * 100.0) / memberIds.size();
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private boolean isMemberUpToDate(Connection conn, String memberId,
                                     List<FeeRow> activeFees,
                                     LocalDate from, LocalDate to) {
        for (FeeRow fee : activeFees) {
            LocalDate effectiveStart = fee.eligibleFrom() != null && fee.eligibleFrom().isAfter(from)
                    ? fee.eligibleFrom()
                    : from;
            if (effectiveStart.isAfter(to)) continue;

            double expected = countOccurrences(fee.frequency(), effectiveStart, to) * fee.amount();
            if (expected <= 0) continue;

            double paid = getPaidForFee(conn, memberId, fee.id(), from, to);
            if (paid < expected) return false;
        }
        return true;
    }

    private List<String> findMemberIds(String collectivityId) {
        List<String> ids = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_MEMBER_IDS)) {
            ps.setString(1, collectivityId);
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

    private record FeeRow(String id, LocalDate eligibleFrom, String frequency, double amount) {}

    private List<FeeRow> findActiveFees(String collectivityId, LocalDate to) {
        List<FeeRow> fees = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(ACTIVE_FEES)) {
            ps.setString(1, collectivityId);
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("eligible_from");
                    fees.add(new FeeRow(
                            rs.getString("id"),
                            d != null ? d.toLocalDate() : null,
                            rs.getString("frequency"),
                            rs.getDouble("amount")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return fees;
    }

    private double getPaidForFee(Connection conn, String memberId, String feeId,
                                 LocalDate from, LocalDate to) {
        try (PreparedStatement ps = conn.prepareStatement(PAID_FOR_FEE)) {
            ps.setString(1, memberId);
            ps.setString(2, feeId);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int countOccurrences(String frequency, LocalDate start, LocalDate to) {
        if (frequency == null) return 0;
        return switch (frequency) {
            case "PUNCTUALLY" -> 1;
            case "WEEKLY" -> {
                long days = java.time.temporal.ChronoUnit.DAYS.between(start, to) + 1;
                yield (int) Math.ceil(days / 7.0);
            }
            case "MONTHLY" -> {
                int months = 0;
                LocalDate cursor = start.withDayOfMonth(1);
                LocalDate endMonth = to.withDayOfMonth(1);
                while (!cursor.isAfter(endMonth)) {
                    months++;
                    cursor = cursor.plusMonths(1);
                }
                yield months;
            }
            case "ANNUALLY" -> {
                int years = 0;
                LocalDate cursor = start.withDayOfYear(1);
                LocalDate endYear = to.withDayOfYear(1);
                while (!cursor.isAfter(endYear)) {
                    years++;
                    cursor = cursor.plusYears(1);
                }
                yield years;
            }
            default -> 0;
        };
    }

    public Double computeOverallAssiduityPercentage(String collectivityId,
                                                    LocalDate from, LocalDate to) {
        List<String> memberIds = findMemberIds(collectivityId);
        if (memberIds.isEmpty()) return null;

        int totalConcerned = 0;
        int totalAttended = 0;

        for (String memberId : memberIds) {
            totalConcerned += countActivitiesConcerned(collectivityId, memberId, from, to);
            totalAttended += countActivitiesAttended(memberId, collectivityId, from, to);
        }

        if (totalConcerned == 0) return null;
        return (totalAttended * 100.0) / totalConcerned;
    }

    private int countActivitiesConcerned(String collectivityId, String memberId,
                                         LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVITIES_FOR_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            ps.setString(2, memberId);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private int countActivitiesAttended(String memberId, String collectivityId,
                                        LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ATTENDED_FOR_COLLECTIVITY)) {
            ps.setString(1, memberId);
            ps.setString(2, collectivityId);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}