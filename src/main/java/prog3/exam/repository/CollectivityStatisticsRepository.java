package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CollectivityStatisticsRepository {

    private final DataSourceConfig dataSourceConfig;

    public CollectivityStatisticsRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String FIND_MEMBER_IDS =
            "SELECT id FROM member WHERE collectivity_id = ?";

    private static final String TOTAL_PAID_BY_MEMBER = """
            SELECT COALESCE(SUM(amount), 0)
            FROM member_payment
            WHERE member_id = ?
              AND creation_date BETWEEN ? AND ?
            """;

    private static final String ACTIVE_FEES_FOR_COLLECTIVITY = """
            SELECT id, eligible_from, frequency, amount
            FROM membership_fee
            WHERE collectivity_id = ?
              AND status = 'ACTIVE'
              AND (eligible_from IS NULL OR eligible_from <= ?)
            """;

    private static final String PAID_FOR_FEE_BY_MEMBER = """
            SELECT COALESCE(SUM(amount), 0)
            FROM member_payment
            WHERE member_id = ?
              AND membership_fee_id = ?
              AND creation_date BETWEEN ? AND ?
            """;

    public List<String> findMemberIds(String collectivityId) {
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

    public double getTotalPaid(String memberId, LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(TOTAL_PAID_BY_MEMBER)) {
            ps.setString(1, memberId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public double getPotentialUnpaid(String memberId, String collectivityId,
                                      LocalDate from, LocalDate to) {
        double totalExpected = 0.0;
        double totalPaidForFees = 0.0;

        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(ACTIVE_FEES_FOR_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String feeId = rs.getString("id");
                    Date eligibleFromDate = rs.getDate("eligible_from");
                    String frequency = rs.getString("frequency");
                    double amount = rs.getDouble("amount");

                    LocalDate eligibleFrom = eligibleFromDate != null
                            ? eligibleFromDate.toLocalDate() : from;

                    LocalDate effectiveStart = eligibleFrom.isAfter(from) ? eligibleFrom : from;
                    if (effectiveStart.isAfter(to)) continue;

                    int occurrences = countOccurrences(frequency, effectiveStart, to);
                    totalExpected += occurrences * amount;

                    totalPaidForFees += getPaidForFee(conn, memberId, feeId, from, to);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }

        return Math.max(0.0, totalExpected - totalPaidForFees);
    }

    private double getPaidForFee(Connection conn, String memberId, String feeId,
                                  LocalDate from, LocalDate to) {
        try (PreparedStatement ps = conn.prepareStatement(PAID_FOR_FEE_BY_MEMBER)) {
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
}
