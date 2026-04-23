package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.Collectivity;
import prog3.exam.model.CollectivityStructure;
import prog3.exam.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CollectivityRepository {

    private final DataSourceConfig dataSourceConfig;
    private final MemberRepository memberRepository;

    public CollectivityRepository(DataSourceConfig dataSourceConfig,
                                   MemberRepository memberRepository) {
        this.dataSourceConfig = dataSourceConfig;
        this.memberRepository = memberRepository;
    }

    private static final String INSERT = """
            INSERT INTO collectivity (id, location, federation_approval,
                                      president_id, vice_president_id, treasurer_id, secretary_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String EXISTS_BY_ID =
            "SELECT COUNT(*) FROM collectivity WHERE id = ?";

    private static final String FIND_BY_ID = """
            SELECT id, number, name, location, president_id, vice_president_id, treasurer_id, secretary_id
            FROM collectivity WHERE id = ?
            """;

    private static final String FIND_MEMBERS_OF_COLLECTIVITY =
            "SELECT id FROM member WHERE collectivity_id = ?";

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

    public void save(String id, String location, boolean federationApproval,
                    String presidentId, String vicePresidentId,
                    String treasurerId, String secretaryId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, id);
            ps.setString(2, location);
            ps.setBoolean(3, federationApproval);
            setNullableString(ps, 4, presidentId);
            setNullableString(ps, 5, vicePresidentId);
            setNullableString(ps, 6, treasurerId);
            setNullableString(ps, 7, secretaryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public boolean existsById(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_ID)) {
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

    public Optional<Collectivity> findById(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                String presidentId     = rs.getString("president_id");
                String vicePresidentId = rs.getString("vice_president_id");
                String treasurerId     = rs.getString("treasurer_id");
                String secretaryId     = rs.getString("secretary_id");

                CollectivityStructure structure = CollectivityStructure.builder()
                        .president(presidentId != null
                                ? memberRepository.findById(presidentId).orElse(null) : null)
                        .vicePresident(vicePresidentId != null
                                ? memberRepository.findById(vicePresidentId).orElse(null) : null)
                        .treasurer(treasurerId != null
                                ? memberRepository.findById(treasurerId).orElse(null) : null)
                        .secretary(secretaryId != null
                                ? memberRepository.findById(secretaryId).orElse(null) : null)
                        .build();

                List<Member> members = findMembersOf(id);

                Collectivity c = Collectivity.builder()
                        .id(rs.getString("id"))
                        .number(rs.getObject("number") != null ? rs.getInt("number") : null)
                        .name(rs.getString("name"))
                        .location(rs.getString("location"))
                        .structure(structure)
                        .members(members)
                        .build();
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private List<Member> findMembersOf(String collectivityId) {
        List<Member> members = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_MEMBERS_OF_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String memberId = rs.getString("id");
                    memberRepository.findById(memberId).ifPresent(members::add);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return members;
    }

    public boolean hasNumber(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(HAS_NUMBER)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public boolean hasName(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(HAS_NAME)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public boolean numberExistsElsewhere(int number, String excludeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_NUMBER)) {
            ps.setInt(1, number);
            ps.setString(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public boolean nameExistsElsewhere(String name, String excludeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_NAME)) {
            ps.setString(1, name);
            ps.setString(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public void updateIdentity(String id, int number, String name) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_IDENTITY)) {
            ps.setInt(1, number);
            ps.setString(2, name);
            ps.setString(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) ps.setString(index, value);
        else ps.setNull(index, Types.VARCHAR);
    }
}
