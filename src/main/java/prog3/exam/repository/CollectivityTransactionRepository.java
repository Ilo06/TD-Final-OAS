package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.*;
import prog3.exam.model.enums.PaymentMode;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class CollectivityTransactionRepository {

    private final DataSourceConfig dataSourceConfig;
    private final FinancialAccountResolver accountResolver;

    public CollectivityTransactionRepository(DataSourceConfig dataSourceConfig,
                                              FinancialAccountResolver accountResolver) {
        this.dataSourceConfig = dataSourceConfig;
        this.accountResolver = accountResolver;
    }

    private static final String INSERT = """
            INSERT INTO collectivity_transaction
                (id, amount, payment_mode, account_credited_type, account_credited_id,
                 member_debited_id, collectivity_id, creation_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
            """;

    private static final String FIND_BY_PERIOD = """
            SELECT id, creation_date, amount, payment_mode,
                   account_credited_type, account_credited_id, member_debited_id
            FROM collectivity_transaction
            WHERE collectivity_id = ? AND creation_date BETWEEN ? AND ?
            """;

    public void save(String collectivityId, String memberId, MemberPayment payment) {
        String accountType;
        String accountId;
        Object credited = payment.getAccountCredited();

        if (credited instanceof CashAccount c) {
            accountType = "CASH";
            accountId = c.getId();
        } else if (credited instanceof MobileBankingAccount m) {
            accountType = "MOBILE_BANKING";
            accountId = m.getId();
        } else if (credited instanceof BankAccount b) {
            accountType = "BANK";
            accountId = b.getId();
        } else {
            return;
        }

        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setDouble(2, payment.getAmount());
            ps.setString(3, payment.getPaymentMode() != null ? payment.getPaymentMode().name() : null);
            ps.setString(4, accountType);
            ps.setString(5, accountId);
            ps.setString(6, memberId);
            ps.setString(7, collectivityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<CollectivityTransaction> findByCollectivityAndPeriod(String collectivityId,
                                                                      LocalDate from,
                                                                      LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PERIOD)) {
            ps.setString(1, collectivityId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<CollectivityTransaction> list = new ArrayList<>();
                while (rs.next()) {
                    String pmStr = rs.getString("payment_mode");
                    String accountId = rs.getString("account_credited_id");
                    Date d = rs.getDate("creation_date");

                    FinancialAccountResolver.ResolvedAccount resolved =
                            accountResolver.resolve(accountId);

                    list.add(CollectivityTransaction.builder()
                            .id(rs.getString("id"))
                            .creationDate(d != null ? d.toLocalDate() : null)
                            .amount(rs.getDouble("amount"))
                            .paymentMode(pmStr != null ? PaymentMode.valueOf(pmStr) : null)
                            .accountCredited(resolved != null ? resolved.account : null)
                            .build());
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }
}
