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

    // ── Member IDs ───────────────────────────────────────────────────────────

    private static final String FIND_MEMBER_IDS =
            "SELECT id FROM member WHERE collectivity_id = ?";

    // ── Earned amount ────────────────────────────────────────────────────────
    // Only payments credited to accounts belonging to THIS collectivity,
    // within [from, to]. Excludes payments the member made to other collectivities.

    private static final String TOTAL_PAID_BY_MEMBER = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.member_id = ?
              AND mp.creation_date BETWEEN ? AND ?
              AND (
                  (mp.account_credited_type = 'CASH'
                   AND EXISTS (SELECT 1 FROM cash_account ca
                               WHERE ca.id = mp.account_credited_id
                                 AND ca.collectivity_id = ?))
                  OR
                  (mp.account_credited_type = 'MOBILE_BANKING'
                   AND EXISTS (SELECT 1 FROM mobile_banking_account mba
                               WHERE mba.id = mp.account_credited_id
                                 AND mba.collectivity_id = ?))
                  OR
                  (mp.account_credited_type = 'BANK'
                   AND EXISTS (SELECT 1 FROM bank_account ba
                               WHERE ba.id = mp.account_credited_id
                                 AND ba.collectivity_id = ?))
              )
            """;

    // ── Unpaid amount ────────────────────────────────────────────────────────
    // unpaid = max(0, total_expected - total_paid_to_this_collectivity)
    //
    // total_expected: sum over all ACTIVE fees of (occurrences × amount)
    //   where occurrences are computed from effective_start = GREATEST(eligible_from, from)
    //   to `to`, per frequency.
    //
    // total_paid: same filter as earnedAmount above — payments to THIS collectivity's
    //   accounts only, regardless of membership_fee_id (matches exam data with no fee ref).
    //
    // Occurrence formulas (pure SQL):
    //   PUNCTUALLY → 1
    //   WEEKLY     → CEIL((to - effective_start + 1) / 7.0)
    //   MONTHLY    → (year_diff * 12 + month_diff + 1)
    //   ANNUALLY   → (year_diff + 1)

    private static final String UNPAID_BY_MEMBER = """
            WITH effective_fees AS (
                SELECT
                    mf.amount,
                    mf.frequency,
                    GREATEST(COALESCE(mf.eligible_from, ?::date), ?::date) AS effective_start
                FROM membership_fee mf
                WHERE mf.collectivity_id = ?
                  AND mf.status = 'ACTIVE'::activity_status_enum
                  AND (mf.eligible_from IS NULL OR mf.eligible_from <= ?::date)
            ),
            fees_with_occurrences AS (
                SELECT
                    ef.amount,
                    CASE ef.frequency
                        WHEN 'PUNCTUALLY' THEN 1
                        WHEN 'WEEKLY'     THEN GREATEST(0, CEIL((?::date - ef.effective_start + 1) / 7.0)::int)
                        WHEN 'MONTHLY'    THEN GREATEST(0,
                            (EXTRACT(YEAR  FROM ?::date) - EXTRACT(YEAR  FROM ef.effective_start))::int * 12
                          + (EXTRACT(MONTH FROM ?::date) - EXTRACT(MONTH FROM ef.effective_start))::int
                          + 1)
                        WHEN 'ANNUALLY'   THEN GREATEST(0,
                            (EXTRACT(YEAR FROM ?::date) - EXTRACT(YEAR FROM ef.effective_start))::int + 1)
                        ELSE 0
                    END AS occurrences
                FROM effective_fees ef
                WHERE ef.effective_start <= ?::date
            ),
            total_expected AS (
                SELECT COALESCE(SUM(occurrences * amount), 0) AS val
                FROM fees_with_occurrences
            ),
            total_paid AS (
                SELECT COALESCE(SUM(mp.amount), 0) AS val
                FROM member_payment mp
                WHERE mp.member_id = ?
                  AND mp.creation_date BETWEEN ? AND ?
                  AND (
                      (mp.account_credited_type = 'CASH'
                       AND EXISTS (SELECT 1 FROM cash_account ca
                                   WHERE ca.id = mp.account_credited_id
                                     AND ca.collectivity_id = ?))
                      OR
                      (mp.account_credited_type = 'MOBILE_BANKING'
                       AND EXISTS (SELECT 1 FROM mobile_banking_account mba
                                   WHERE mba.id = mp.account_credited_id
                                     AND mba.collectivity_id = ?))
                      OR
                      (mp.account_credited_type = 'BANK'
                       AND EXISTS (SELECT 1 FROM bank_account ba
                                   WHERE ba.id = mp.account_credited_id
                                     AND ba.collectivity_id = ?))
                  )
            )
            SELECT GREATEST(0, te.val - tp.val)
            FROM total_expected te, total_paid tp
            """;

    // ── Assiduity ────────────────────────────────────────────────────────────

    private static final String ASSIDUITY_BY_MEMBER = """
            SELECT
                COUNT(DISTINCT ca.id) AS total_concerned,
                COUNT(DISTINCT CASE WHEN ama.attendance_status = 'ATTENDED'::attendance_status_enum
                                    THEN ca.id END) AS total_attended
            FROM collectivity_activity ca
            LEFT JOIN collectivity_activity_occupation cao ON ca.id = cao.activity_id
            JOIN member m ON m.collectivity_id = ca.collectivity_id
            LEFT JOIN activity_member_attendance ama
                   ON ama.activity_id = ca.id AND ama.member_id = m.id
            WHERE m.id = ?
              AND ca.collectivity_id = ?
              AND (
                  ca.executive_date BETWEEN ? AND ?
                  OR (ca.executive_date IS NULL AND ca.recurrence_week_ordinal IS NOT NULL)
              )
              AND (
                  NOT EXISTS (SELECT 1 FROM collectivity_activity_occupation x WHERE x.activity_id = ca.id)
                  OR cao.occupation = m.occupation
              )
            """;

    // ── Public API ───────────────────────────────────────────────────────────

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

    /**
     * Sum of payments by this member to accounts of THIS collectivity within [from, to].
     */
    public double getTotalPaid(String memberId, String collectivityId, LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(TOTAL_PAID_BY_MEMBER)) {
            ps.setString(1, memberId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            ps.setString(4, collectivityId);
            ps.setString(5, collectivityId);
            ps.setString(6, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * Unpaid = max(0, total_expected - total_paid_to_collectivity).
     * No fee-level matching needed: works with payments that have no membership_fee_id.
     *
     * Param order for UNPAID_BY_MEMBER:
     *  1  from           (GREATEST: fallback for NULL eligible_from)
     *  2  from           (GREATEST: lower bound)
     *  3  collectivityId
     *  4  to             (eligible_from <= to filter)
     *  5  to             (WEEKLY end)
     *  6  to             (MONTHLY year)
     *  7  to             (MONTHLY month)
     *  8  to             (ANNUALLY year)
     *  9  to             (WHERE effective_start <= to)
     *  10 memberId
     *  11 from           (BETWEEN from)
     *  12 to             (BETWEEN to)
     *  13 collectivityId (CASH)
     *  14 collectivityId (MOBILE_BANKING)
     *  15 collectivityId (BANK)
     */
    public double getPotentialUnpaid(String memberId, String collectivityId,
                                     LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UNPAID_BY_MEMBER)) {
            ps.setString(1,  from.toString());
            ps.setString(2,  from.toString());
            ps.setString(3,  collectivityId);
            ps.setString(4,  to.toString());
            ps.setString(5,  to.toString());
            ps.setString(6,  to.toString());
            ps.setString(7,  to.toString());
            ps.setString(8,  to.toString());
            ps.setString(9,  to.toString());
            ps.setString(10, memberId);
            ps.setDate(11,   Date.valueOf(from));
            ps.setDate(12,   Date.valueOf(to));
            ps.setString(13, collectivityId);
            ps.setString(14, collectivityId);
            ps.setString(15, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    /**
     * Returns the assiduity percentage for a member, or null if no concerned activities.
     */
    public Double getAssiduityPercentage(String memberId, String collectivityId,
                                         LocalDate from, LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(ASSIDUITY_BY_MEMBER)) {
            ps.setString(1, memberId);
            ps.setString(2, collectivityId);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int total = rs.getInt("total_concerned");
                if (total == 0) return null;
                int attended = rs.getInt("total_attended");
                return (attended * 100.0) / total;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}
