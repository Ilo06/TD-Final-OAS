package prog3.exam.repository;

import org.springframework.stereotype.Component;
import prog3.exam.datasource.DataSourceConfig;
import prog3.exam.model.BankAccount;
import prog3.exam.model.CashAccount;
import prog3.exam.model.*;
import prog3.exam.model.enums.*;

import java.sql.*;
import java.util.Optional;

@Component
public class FinancialAccountResolver {

    private final DataSourceConfig dataSourceConfig;

    public FinancialAccountResolver(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public ResolvedAccount resolve(String accountId) {
        if (accountId == null) return null;
        int id;
        try {
            id = Integer.parseInt(accountId);
        } catch (NumberFormatException e) {
            return null;
        }

        Optional<CashAccount> cash = findCash(id);
        if (cash.isPresent()) return new ResolvedAccount("CASH", id, cash.get());

        Optional<MobileBankingAccount> mobile = findMobile(id);
        if (mobile.isPresent()) return new ResolvedAccount("MOBILE_BANKING", id, mobile.get());

        Optional<BankAccount> bank = findBank(id);
        if (bank.isPresent()) return new ResolvedAccount("BANK", id, bank.get());

        return null;
    }

    private Optional<CashAccount> findCash(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, amount FROM cash_account WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(CashAccount.builder()
                        .id(rs.getInt("id"))
                        .amount(rs.getInt("amount"))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private Optional<MobileBankingAccount> findMobile(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, holder_name, mobile_banking_service, mobile_number, amount " +
                "FROM mobile_banking_account WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String svc = rs.getString("mobile_banking_service");
                return Optional.of(MobileBankingAccount.builder()
                        .id(rs.getInt("id"))
                        .holderName(rs.getString("holder_name"))
                        .mobileBankingService(svc != null ? MobileBankingService.valueOf(svc) : null)
                        .mobileNumber(rs.getString("mobile_number"))
                        .amount(rs.getDouble("amount"))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    private Optional<BankAccount> findBank(int id) {
        Connection conn = dataSourceConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, holder_name, bank_name, bank_code, bank_branch_code, " +
                "bank_account_number, bank_account_key, amount FROM bank_account WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String bank = rs.getString("bank_name");
                return Optional.of(BankAccount.builder()
                        .id(rs.getInt("id"))
                        .holderName(rs.getString("holder_name"))
                        .bankName(bank != null ? Bank.valueOf(bank) : null)
                        .bankCode(rs.getInt("bank_code"))
                        .bankBranchCode(rs.getInt("bank_branch_code"))
                        .bankAccountNumber(rs.getLong("bank_account_number"))
                        .bankAccountKey(rs.getInt("bank_account_key"))
                        .amount(rs.getDouble("amount"))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSourceConfig.closeConnection(conn);
        }
    }

    public static class ResolvedAccount {
        public final String type;
        public final int id;
        public final Object account;

        public ResolvedAccount(String type, int id, Object account) {
            this.type = type;
            this.id = id;
            this.account = account;
        }
    }
}
