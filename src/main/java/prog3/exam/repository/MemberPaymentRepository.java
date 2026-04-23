package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.MemberPayment;
import prog3.exam.model.requests.CreateMemberPaymentRequest;

import java.sql.*;
import java.time.LocalDate;

@Repository
public class MemberPaymentRepository {

    private final DataSourceConfig dataSourceConfig;
    private final FinancialAccountResolver accountResolver;

    public MemberPaymentRepository(DataSourceConfig dataSourceConfig,
                                    FinancialAccountResolver accountResolver) {
        this.dataSourceConfig = dataSourceConfig;
        this.accountResolver = accountResolver;
    }

    private static final String INSERT = """
            INSERT INTO member_payment
                (amount, payment_mode, membership_fee_id,
                 account_credited_type, account_credited_id,
                 creation_date, member_id)
            VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)
            """;

    public MemberPayment save(int memberId, CreateMemberPaymentRequest req) {
        FinancialAccountResolver.ResolvedAccount resolved =
                accountResolver.resolve(req.getAccountCreditedIdentifier());

        if (resolved == null) {
            throw new NotFoundException("Account not found: " + req.getAccountCreditedIdentifier());
        }

        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, req.getAmount());
            ps.setString(2, req.getPaymentMode() != null ? req.getPaymentMode().name() : null);
            if (req.getMembershipFeeIdentifier() != null) {
                ps.setInt(3, Integer.parseInt(req.getMembershipFeeIdentifier()));
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, resolved.type);
            ps.setInt(5, resolved.id);
            ps.setInt(6, memberId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return MemberPayment.builder()
                            .id(keys.getInt(1))
                            .amount(req.getAmount())
                            .paymentMode(req.getPaymentMode())
                            .accountCredited(resolved.account)
                            .creationDate(LocalDate.now())
                            .build();
                }
                throw new IllegalStateException("No key returned for member_payment");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}
