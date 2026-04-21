package prog3.exam.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectivityRepository {

    private final JdbcTemplate jdbc;

    private static final String INSERT = """
            INSERT INTO collectivity (id, location, federation_approval,
                                      president_id, vice_president_id, treasurer_id, secretary_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String EXISTS_BY_ID =
            "SELECT COUNT(*) FROM collectivity WHERE id = ?";

    public int save(int id, String location, boolean federationApproval,
                     int presidentId, int vicePresidentId, int treasurerId, int secretaryId) {
        jdbc.update(INSERT, id, location, federationApproval,
                presidentId, vicePresidentId, treasurerId, secretaryId);
        return id;
    }

    public boolean existsById(int id) {
        Integer count = jdbc.queryForObject(EXISTS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
}
