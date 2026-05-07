package prog3.exam.api.model;

public class CashAccount implements FinancialAccount {

    public String id;
    public Integer amount;
    public String type= "CASH";

    @Override
    public String toString() {
        return "CashAccount{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                '}';
    }
}