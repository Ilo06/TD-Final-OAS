package prog3.exam.repository;

import org.springframework.stereotype.Repository;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.BankAccount;
import prog3.exam.model.CashAccount;
import prog3.exam.model.MobileBankingAccount;
import prog3.exam.model.enums.Bank;
import prog3.exam.model.enums.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FinancialAccountRepository {

    private final DataSourceConfig dataSourceConfig;

    public FinancialAccountRepository(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    private static final String FIND_CASH_BY_COLLECTIVITY =
            "SELECT id FROM cash_account WHERE collectivity_id = ?";

    private static final String CASH_BALANCE_UP_TO = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'CASH'
              AND mp.account_credited_id   = ?
              AND mp.creation_date        <= ?
            """;

    private static final String CASH_BALANCE_ALL = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'CASH'
              AND mp.account_credited_id   = ?
            """;

    private static final String FIND_MOBILE_BY_COLLECTIVITY =
            "SELECT id, holder_name, mobile_banking_service, mobile_number " +
                    "FROM mobile_banking_account WHERE collectivity_id = ?";

    private static final String MOBILE_BALANCE_UP_TO = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'MOBILE_BANKING'
              AND mp.account_credited_id   = ?
              AND mp.creation_date        <= ?
            """;

    private static final String MOBILE_BALANCE_ALL = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'MOBILE_BANKING'
              AND mp.account_credited_id   = ?
            """;

    private static final String FIND_BANK_BY_COLLECTIVITY =
            "SELECT id, holder_name, bank_name, bank_code, bank_branch_code, " +
                    "bank_account_number, bank_account_key " +
                    "FROM bank_account WHERE collectivity_id = ?";

    private static final String BANK_BALANCE_UP_TO = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'BANK'
              AND mp.account_credited_id   = ?
              AND mp.creation_date        <= ?
            """;

    private static final String BANK_BALANCE_ALL = """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'BANK'
              AND mp.account_credited_id   = ?
            """;

    public List<Object> findByCollectivityId(String collectivityId, LocalDate at) {
        List<Object> accounts = new ArrayList<>();
        accounts.addAll(findCashAccounts(collectivityId, at));
        accounts.addAll(findMobileAccounts(collectivityId, at));
        accounts.addAll(findBankAccounts(collectivityId, at));
        return accounts;
    }

    private List<CashAccount> findCashAccounts(String collectivityId, LocalDate at) {
        List<CashAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_CASH_BY_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    double balance = computeBalance(conn,
                            at != null ? CASH_BALANCE_UP_TO : CASH_BALANCE_ALL, id, at);
                    list.add(CashAccount.builder()
                            .id(id)
                            .amount(balance)
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return list;
    }

    private List<MobileBankingAccount> findMobileAccounts(String collectivityId, LocalDate at) {
        List<MobileBankingAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_MOBILE_BY_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    double balance = computeBalance(conn,
                            at != null ? MOBILE_BALANCE_UP_TO : MOBILE_BALANCE_ALL, id, at);
                    String svc = rs.getString("mobile_banking_service");
                    list.add(MobileBankingAccount.builder()
                            .id(id)
                            .holderName(rs.getString("holder_name"))
                            .mobileBankingService(svc != null ? MobileBankingService.valueOf(svc) : null)
                            .mobileNumber(rs.getString("mobile_number"))
                            .amount(balance)
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return list;
    }

    private List<BankAccount> findBankAccounts(String collectivityId, LocalDate at) {
        List<BankAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BANK_BY_COLLECTIVITY)) {
            ps.setString(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    double balance = computeBalance(conn,
                            at != null ? BANK_BALANCE_UP_TO : BANK_BALANCE_ALL, id, at);
                    String bankStr = rs.getString("bank_name");
                    list.add(BankAccount.builder()
                            .id(id)
                            .holderName(rs.getString("holder_name"))
                            .bankName(bankStr != null ? Bank.valueOf(bankStr) : null)
                            .bankCode(rs.getInt("bank_code"))
                            .bankBranchCode(rs.getInt("bank_branch_code"))
                            .bankAccountNumber(rs.getLong("bank_account_number"))
                            .bankAccountKey(rs.getInt("bank_account_key"))
                            .amount(balance)
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
        return list;
    }

    /**
     * Computes balance from payment records.
     * If {@code at} is non-null, sums payments up to and including that date.
     * If {@code at} is null, the sql passed in should have no date parameter (sums all payments).
     */
    private double computeBalance(Connection conn, String sql, String accountId, LocalDate at) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            if (at != null) {
                ps.setDate(2, Date.valueOf(at));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}