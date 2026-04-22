package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.Collectivity;

import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;

@Repository
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

    private static final String FIND_BY_ID = """
            SELECT id, number, name, location, president_id, vice_president_id, treasurer_id, secretary_id
            FROM collectivity WHERE id = ?
            """;

    private static final String EXISTS_BY_NUMBER =
            "SELECT COUNT(*) FROM collectivity WHERE number = ? AND id <> ?";

    private static final String EXISTS_BY_NAME =
            "SELECT COUNT(*) FROM collectivity WHERE name = ? AND id <> ?";

    private static final String HAS_NUMBER =
            "SELECT number IS NOT NULL FROM collectivity WHERE id = ?";

    private static final String HAS_NAME =
            "SELECT name IS NOT NULL FROM collectivity WHERE id = ?";

    private static final String UPDATE_IDENTITY =
            "UPDATE collectivity SET number = ?, name = ? WHERE id = ?";

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
        }
    }

    public Optional<Collectivity> findById(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Collectivity c = Collectivity.builder()
                        .id(rs.getInt("id"))
                        .number(rs.getObject("number") != null ? rs.getInt("number") : null)
                        .name(rs.getString("name"))
                        .location(rs.getString("location"))
                        .members(new ArrayList<>())
                        .build();
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true if this collectivity already has a number assigned. */
    public boolean hasNumber(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(HAS_NUMBER)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true if this collectivity already has a name assigned. */
    public boolean hasName(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(HAS_NAME)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true if the given number is already used by another collectivity. */
    public boolean numberExistsElsewhere(int number, int excludeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_NUMBER)) {
            ps.setInt(1, number);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true if the given name is already used by another collectivity. */
    public boolean nameExistsElsewhere(String name, int excludeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_NAME)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Persists number and name for the given collectivity. */
    public void updateIdentity(int id, int number, String name) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_IDENTITY)) {
            ps.setInt(1, number);
            ps.setString(2, name);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) ps.setInt(index, value);
        else ps.setNull(index, Types.INTEGER);
    }
}
