package ee.taltech.algoritmid.accumulator;

import java.util.*;

public class Transactions implements Result {

    List<Integer> transactions;
    Double profit;

    public Transactions(List<Integer> transactions, Double profit) {
        this.transactions = transactions;
        this.profit = profit;
    }

    @Override
    public List<Integer> getTransactions() {
        return this.transactions;
    }

    @Override
    public double getTotalIncome() {
        return this.profit;
    }
}

