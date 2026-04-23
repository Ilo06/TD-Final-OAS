package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.exception.NotFoundException;
import prog3.exam.model.MemberPayment;
import prog3.exam.model.requests.CreateMemberPaymentRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

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
                (id, amount, payment_mode, membership_fee_id,
                 account_credited_type, account_credited_id,
                 creation_date, member_id)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, ?)
            """;

    public MemberPayment save(String memberId, CreateMemberPaymentRequest req) {
        FinancialAccountResolver.ResolvedAccount resolved =
                accountResolver.resolve(req.getAccountCreditedIdentifier());

        if (resolved == null) {
            throw new NotFoundException("Account not found: " + req.getAccountCreditedIdentifier());
        }

        String paymentId = UUID.randomUUID().toString();

        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, paymentId);
            ps.setDouble(2, req.getAmount());
            ps.setString(3, req.getPaymentMode() != null ? req.getPaymentMode().name() : null);
            if (req.getMembershipFeeIdentifier() != null) {
                ps.setString(4, req.getMembershipFeeIdentifier());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            ps.setString(5, resolved.type);
            ps.setString(6, resolved.id);
            ps.setString(7, memberId);
            ps.executeUpdate();

            return MemberPayment.builder()
                    .id(paymentId)
                    .amount(req.getAmount())
                    .paymentMode(req.getPaymentMode())
                    .accountCredited(resolved.account)
                    .creationDate(LocalDate.now())
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}
