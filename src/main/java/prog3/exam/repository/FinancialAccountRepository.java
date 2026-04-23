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
            "SELECT id, amount FROM cash_account WHERE collectivity_id = ?";

    private static final String CASH_PAYMENTS_AFTER =
            """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'CASH'
              AND mp.account_credited_id   = ?
              AND mp.creation_date         > ?
            """;


    private static final String FIND_MOBILE_BY_COLLECTIVITY =
            "SELECT id, holder_name, mobile_banking_service, mobile_number, amount " +
            "FROM mobile_banking_account WHERE collectivity_id = ?";

    private static final String MOBILE_PAYMENTS_AFTER =
            """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'MOBILE_BANKING'
              AND mp.account_credited_id   = ?
              AND mp.creation_date         > ?
            """;


    private static final String FIND_BANK_BY_COLLECTIVITY =
            "SELECT id, holder_name, bank_name, bank_code, bank_branch_code, " +
            "bank_account_number, bank_account_key, amount " +
            "FROM bank_account WHERE collectivity_id = ?";

    private static final String BANK_PAYMENTS_AFTER =
            """
            SELECT COALESCE(SUM(mp.amount), 0)
            FROM member_payment mp
            WHERE mp.account_credited_type = 'BANK'
              AND mp.account_credited_id   = ?
              AND mp.creation_date         > ?
            """;


    public List<Object> findByCollectivityId(int collectivityId, LocalDate at) {
        List<Object> accounts = new ArrayList<>();
        accounts.addAll(findCashAccounts(collectivityId, at));
        accounts.addAll(findMobileAccounts(collectivityId, at));
        accounts.addAll(findBankAccounts(collectivityId, at));
        return accounts;
    }


    private List<CashAccount> findCashAccounts(int collectivityId, LocalDate at) {
        List<CashAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_CASH_BY_COLLECTIVITY)) {
            ps.setInt(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double currentAmount = rs.getDouble("amount");
                    double balance = at != null
                            ? currentAmount - paymentsAfter(conn, CASH_PAYMENTS_AFTER, id, at)
                            : currentAmount;
                    list.add(CashAccount.builder()
                            .id(id)
                            .amount((int) balance)
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

    private List<MobileBankingAccount> findMobileAccounts(int collectivityId, LocalDate at) {
        List<MobileBankingAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_MOBILE_BY_COLLECTIVITY)) {
            ps.setInt(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double currentAmount = rs.getDouble("amount");
                    double balance = at != null
                            ? currentAmount - paymentsAfter(conn, MOBILE_PAYMENTS_AFTER, id, at)
                            : currentAmount;
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

    private List<BankAccount> findBankAccounts(int collectivityId, LocalDate at) {
        List<BankAccount> list = new ArrayList<>();
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BANK_BY_COLLECTIVITY)) {
            ps.setInt(1, collectivityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double currentAmount = rs.getDouble("amount");
                    double balance = at != null
                            ? currentAmount - paymentsAfter(conn, BANK_PAYMENTS_AFTER, id, at)
                            : currentAmount;
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
     * Returns the total amount of payments credited to account {@code accountId}
     * (of the type encoded in {@code sql}) strictly after {@code at}.
     * Uses the provided open connection — does NOT close it.
     */
    private double paymentsAfter(Connection conn, String sql, int accountId, LocalDate at) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setDate(2, Date.valueOf(at));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
