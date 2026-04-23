package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.Member;
import prog3.exam.model.enums.Gender;
import prog3.exam.model.enums.MemberOccupation;
import prog3.exam.model.requests.CreateMemberRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberRepository {

    private final DataSourceConfig dataSourceConfig;

    public MemberRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String INSERT_MEMBER = """
            INSERT INTO member (id, first_name, last_name, birth_date, gender, address, profession,
                                phone_number, email, occupation, collectivity_id,
                                registration_fee_paid, membership_dues_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_REFEREE =
            "INSERT INTO member_referee (member_id, referee_id) VALUES (?, ?)";

    private static final String FIND_BY_ID = """
            SELECT id, first_name, last_name, birth_date, gender, address, profession,
                   phone_number, email, occupation
            FROM member WHERE id = ?
            """;

    private static final String FIND_REFEREES_OF = """
            SELECT m.id, m.first_name, m.last_name, m.birth_date, m.gender, m.address,
                   m.profession, m.phone_number, m.email, m.occupation
            FROM member m
            JOIN member_referee mr ON m.id = mr.referee_id
                WHERE mr.member_id = ?
            """;

    private static final String EXISTS_BY_ID =
            "SELECT COUNT(*) FROM member WHERE id = ?";

    private static final String EXISTS_BY_COLLECTIVITY =
            "SELECT COUNT(*) FROM member WHERE id = ? AND collectivity_id = ?";

    private static final String FIND_COLLECTIVITY_ID =
            "SELECT collectivity_id FROM member WHERE id = ?";

    public void save(CreateMemberRequest req) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_MEMBER)) {
            ps.setString(1, req.getId());
            ps.setString(2, req.getFirstName());
            ps.setString(3, req.getLastName());
            ps.setDate(4, req.getBirthDate() != null ? Date.valueOf(req.getBirthDate()) : null);
            ps.setString(5, req.getGender() != null ? req.getGender().name() : null);
            ps.setString(6, req.getAddress());
            ps.setString(7, req.getProfession());
            ps.setString(8, req.getPhoneNumber());
            ps.setString(9, req.getEmail());
            ps.setString(10, req.getOccupation() != null ? req.getOccupation().name() : null);
            if (req.getCollectivityIdentifier() != null) {
                ps.setString(11, req.getCollectivityIdentifier());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }
            ps.setBoolean(12, Boolean.TRUE.equals(req.getRegistrationFeePaid()));
            ps.setBoolean(13, Boolean.TRUE.equals(req.getMembershipDuesPaid()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public void saveReferee(String memberId, String refereeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_REFEREE)) {
            ps.setString(1, memberId);
            ps.setString(2, refereeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public Optional<Member> findById(String id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Member member = mapRow(rs);
                member.setReferees(findRefereesOf(id));
                return Optional.of(member);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<Member> findRefereesOf(String memberId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_REFEREES_OF)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Member> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
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

    public boolean existsByIdAndCollectivity(String memberId, String collectivityId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_COLLECTIVITY)) {
            ps.setString(1, memberId);
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

    public String findCollectivityId(String memberId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_COLLECTIVITY_ID)) {
            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("collectivity_id");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        String genderStr = rs.getString("gender");
        String occupationStr = rs.getString("occupation");
        Date birthDate = rs.getDate("birth_date");
        return Member.builder()
                .id(rs.getString("id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .birthDate(birthDate != null ? birthDate.toLocalDate() : null)
                .gender(genderStr != null ? Gender.valueOf(genderStr) : null)
                .address(rs.getString("address"))
                .profession(rs.getString("profession"))
                .phoneNumber(rs.getString("phone_number"))
                .email(rs.getString("email"))
                .occupation(occupationStr != null ? MemberOccupation.valueOf(occupationStr) : null)
                .referees(new ArrayList<>())
                .build();
    }
}
