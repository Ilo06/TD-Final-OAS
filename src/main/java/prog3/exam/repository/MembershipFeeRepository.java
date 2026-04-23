package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.MembershipFee;
import prog3.exam.model.enums.ActivityStatus;
import prog3.exam.model.enums.Frequency;
import prog3.exam.model.requests.CreateMembershipFeeRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MembershipFeeRepository {

    private final DataSourceConfig dataSourceConfig;

    public MembershipFeeRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String INSERT = """
            INSERT INTO membership_fee (id, eligible_from, frequency, amount, label, status, collectivity_id)
            VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)
            """;

    private static final String FIND_BY_COLLECTIVITY =
            "SELECT id, eligible_from, frequency, amount, label, status FROM membership_fee WHERE collectivity_id = ?";

    private static final String COLLECTIVITY_EXISTS =
            "SELECT COUNT(*) FROM collectivity WHERE id = ?";

    public boolean collectivityExists(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COLLECTIVITY_EXISTS)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<MembershipFee> findByCollectivityId(String collectivityId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MembershipFee> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<MembershipFee> saveAll(String collectivityId, List<CreateMembershipFeeRequest> requests) {
        List<MembershipFee> result = new ArrayList<>();
        for (CreateMembershipFeeRequest req : requests) {
            Connection conn = dataSourceConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                String feeId = req.getId() != null ? req.getId()
                        : "fee-" + collectivityId + "-" + System.currentTimeMillis();
                ps.setString(1, feeId);
                ps.setDate(2, req.getEligibleFrom() != null ? Date.valueOf(req.getEligibleFrom()) : null);
                ps.setString(3, req.getFrequency() != null ? req.getFrequency().name() : null);
                ps.setDouble(4, req.getAmount());
                ps.setString(5, req.getLabel());
                ps.setString(6, collectivityId);
                ps.executeUpdate();
                result.add(MembershipFee.builder()
                        .id(feeId)
                        .eligibleFrom(req.getEligibleFrom())
                        .frequency(req.getFrequency())
                        .amount(req.getAmount())
                        .label(req.getLabel())
                        .status(ActivityStatus.ACTIVE)
                        .build());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                dataSourceConfig.closeConnection(conn);
            }
        }
        return result;
    }

    private MembershipFee mapRow(ResultSet rs) throws SQLException {
        String freqStr = rs.getString("frequency");
        String statusStr = rs.getString("status");
        Date d = rs.getDate("eligible_from");
        return MembershipFee.builder()
                .id(rs.getString("id"))
                .eligibleFrom(d != null ? d.toLocalDate() : null)
                .frequency(freqStr != null ? Frequency.valueOf(freqStr) : null)
                .amount(rs.getDouble("amount"))
                .label(rs.getString("label"))
                .status(statusStr != null ? ActivityStatus.valueOf(statusStr) : null)
                .build();
    }
}
