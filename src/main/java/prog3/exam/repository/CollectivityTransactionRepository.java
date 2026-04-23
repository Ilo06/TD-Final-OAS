package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.*;
import prog3.exam.model.enums.PaymentMode;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                (amount, payment_mode, account_credited_type, account_credited_id,
                 member_debited_id, collectivity_id, creation_date)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)
            """;

    private static final String FIND_BY_PERIOD = """
            SELECT id, creation_date, amount, payment_mode,
                   account_credited_type, account_credited_id, member_debited_id
            FROM collectivity_transaction
            WHERE collectivity_id = ? AND creation_date BETWEEN ? AND ?
            """;

    public void save(int collectivityId, int memberId, MemberPayment payment) {
        String accountType;
        int accountId;
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
            ps.setInt(1, payment.getAmount());
            ps.setString(2, payment.getPaymentMode() != null ? payment.getPaymentMode().name() : null);
            ps.setString(3, accountType);
            ps.setInt(4, accountId);
            ps.setInt(5, memberId);
            ps.setInt(6, collectivityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public List<CollectivityTransaction> findByCollectivityAndPeriod(int collectivityId,
                                                                      LocalDate from,
                                                                      LocalDate to) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PERIOD)) {
            ps.setInt(1, collectivityId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<CollectivityTransaction> list = new ArrayList<>();
                while (rs.next()) {
                    String pmStr = rs.getString("payment_mode");
                    int accountId = rs.getInt("account_credited_id");
                    Date d = rs.getDate("creation_date");

                    FinancialAccountResolver.ResolvedAccount resolved =
                            accountResolver.resolve(String.valueOf(accountId));

                    list.add(CollectivityTransaction.builder()
                            .id(rs.getInt("id"))
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
