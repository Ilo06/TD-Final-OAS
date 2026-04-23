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
            INSERT INTO member (first_name, last_name, birth_date, gender, address, profession,
                                phone_number, email, occupation, collectivity_id,
                                registration_fee_paid, membership_dues_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public int save(CreateMemberRequest req) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_MEMBER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getFirstName());
            ps.setString(2, req.getLastName());
            ps.setDate(3, req.getBirthDate() != null ? Date.valueOf(req.getBirthDate()) : null);
            ps.setString(4, req.getGender() != null ? req.getGender().name() : null);
            ps.setString(5, req.getAddress());
            ps.setString(6, req.getProfession());
            ps.setString(7, req.getPhoneNumber());
            ps.setString(8, req.getEmail());
            ps.setString(9, req.getOccupation() != null ? req.getOccupation().name() : null);
            if (req.getCollectivityIdentifier() != null) {
                ps.setInt(10, req.getCollectivityIdentifier());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.setBoolean(11, Boolean.TRUE.equals(req.getRegistrationFeePaid()));
            ps.setBoolean(12, Boolean.TRUE.equals(req.getMembershipDuesPaid()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new IllegalStateException("No generated key returned for member insert");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public void saveReferee(int memberId, int refereeId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_REFEREE)) {
            ps.setInt(1, memberId);
            ps.setInt(2, refereeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public Optional<Member> findById(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, id);
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

    public List<Member> findRefereesOf(int memberId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_REFEREES_OF)) {
            ps.setInt(1, memberId);
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

    public boolean existsByIdAndCollectivity(int memberId, int collectivityId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_COLLECTIVITY)) {
            ps.setInt(1, memberId);
            ps.setInt(2, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
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
                .id(rs.getInt("id"))
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

    private  String FIND_COLLECTIVITY_ID =
            "SELECT collectivity_id FROM member WHERE id = ?";

    public Integer findCollectivityId(int memberId) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_COLLECTIVITY_ID)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int val = rs.getInt("collectivity_id");
                    return rs.wasNull() ? null : val;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }


}