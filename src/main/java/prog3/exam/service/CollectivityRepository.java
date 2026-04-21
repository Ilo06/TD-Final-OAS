package prog3.exam.service;

import prog3.exam.datasource.DataSourceConfig;

import java.sql.*;

public class CollectivityRepository {

    private final DataSourceConfig dataSourceConfig;

    public CollectivityRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String INSERT = """
            INSERT INTO collectivity (location, federation_approval,
                                      president_id, vice_president_id, treasurer_id, secretary_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String EXISTS_BY_ID =
            "SELECT COUNT(*) FROM collectivity WHERE id = ?";

    public int save(String location, boolean federationApproval,
                    Integer presidentId, Integer vicePresidentId,
                    Integer treasurerId, Integer secretaryId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, location);
            ps.setBoolean(2, federationApproval);
            setNullableInt(ps, 3, presidentId);
            setNullableInt(ps, 4, vicePresidentId);
            setNullableInt(ps, 5, treasurerId);
            setNullableInt(ps, 6, secretaryId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new IllegalStateException("No generated key returned for collectivity insert");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public boolean existsById(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) ps.setInt(index, value);
        else ps.setNull(index, Types.INTEGER);
    }
}