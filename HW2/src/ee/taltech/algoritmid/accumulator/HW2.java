package ee.taltech.algoritmid.accumulator;

import java.util.*;

public class HW2 implements AccumulatorController {

    private Integer currentLoad;
    private final boolean useSecondSolution = true;

    /**
     * Demonstrates both solutions based on reviewers choice above.
     */
    @Override
    public Result findStrategy(double[] prices, int capacitance,
                               int receivingLimit, int sendingLimit,
                               double cost, double transmissionEfficiency,
                               int initialLoad, int finalLoad) {

        assert initialLoad >= 0 : "Battery load cannot be negative!";
        assert initialLoad <= capacitance : "Battery load exceeds capacity!";

        if (useSecondSolution) {
            return optimalTransactionsWithDP(prices, capacitance,
                    receivingLimit, sendingLimit, cost,
                    transmissionEfficiency, initialLoad, finalLoad);
        }
        return findStrategyFirstSolutionBestTransactionsInWindow(prices, capacitance,
                receivingLimit, sendingLimit, cost,
                transmissionEfficiency, initialLoad, finalLoad, 24, 0.05);
    }

    /**
     * Finds best transactions with first, average-based method.
     */
    public Result findStrategyFirstSolutionBestTransactionsInWindow(double[] prices, int capacitance,
                                                                    int receivingLimit, int sendingLimit,
                                                                    double cost, double transmissionEfficiency,
                                                                    int initialLoad, int finalLoad, int timeWindowSize, double expectedProfit) {
        List<Integer> transactions = new ArrayList<>();
        Double totalProfit = 0.0;
        double[] currentPrices;
        currentLoad = initialLoad;

        int numberOfWindows = (int) Math.ceil((double) prices.length / timeWindowSize);

        // Forces to use more aggressive buying strategy if last window(s)
        boolean endIsNear;

        for (int window = 0; window < numberOfWindows; window++) {

            int start = window * timeWindowSize;
            int end = Math.min(start + timeWindowSize, prices.length);
            currentPrices = Arrays.copyOfRange(prices, start, end);
            int minWindowsToBuyFinalLoad = (int) Math.ceil(finalLoad - currentLoad / transmissionEfficiency);
            endIsNear = (window >= numberOfWindows - minWindowsToBuyFinalLoad || window == numberOfWindows - 1);

            Result currentResult = bestTransactionsInWindow(
                    currentPrices, capacitance, receivingLimit, sendingLimit,
                    cost, transmissionEfficiency, finalLoad, endIsNear, expectedProfit);

            List<Integer> windowTransactions = currentResult.getTransactions();
            transactions.addAll(windowTransactions);
            totalProfit += currentResult.getTotalIncome();
        }

        return new Transactions(transactions, totalProfit);
    }

    /**
     * Finds best transactions using average price in time window.
     */
    private Result bestTransactionsInWindow(double[] prices, int capacitance,
                                            int receivingLimit, int sendingLimit,
                                            double cost, double transmissionEfficiency,
                                            int finalLoad, boolean endIsNear, double expectedProfit) {
        int timeSteps = prices.length;
        double averagePrice = Arrays.stream(prices).average().orElse(Double.NaN);
        List<Integer> transactions = new ArrayList<>();
        double profit = 0.0;

        for (int i = 0; i < timeSteps; i++) {

            double currentPrice = prices[i];
            boolean needToStopSelling = (currentLoad == 0);
            boolean needToStopBuying = (currentLoad == capacitance);
            int limitingTimeStep = timeSteps;

            if (endIsNear) {
                // Checks if enough timeSteps left to achieve needed final load and tunes buying and selling.
                limitingTimeStep = (int) (timeSteps - (finalLoad - currentLoad) / Math.round(transmissionEfficiency * receivingLimit) );
                needToStopSelling = currentLoad < finalLoad && i >= limitingTimeStep || currentLoad == 0;
                needToStopBuying = currentLoad >= finalLoad || currentLoad == capacitance;
            }

            // More aggressive buying if final timeSteps and finalLoad is not met
            if (endIsNear && i >= limitingTimeStep - 1) {
                int requiredChange = finalLoad - currentLoad;
                if (requiredChange > 0) {
                    int minBuyNeeded = (int) Math.ceil(requiredChange / transmissionEfficiency);
                    int buyAmount = Math.min(receivingLimit, Math.min(capacitance - currentLoad, minBuyNeeded));
                    if (buyAmount > 0) {
                        int actualChange = (int) Math.round(buyAmount * transmissionEfficiency);
                        currentLoad += actualChange;
                        profit -= (buyAmount * currentPrice) + (buyAmount * cost);
                        profit = Math.round(profit * 1000.0) / 1000.0;
                        transactions.add(buyAmount);
                    }

                } else {
                    // Final timeSteps of final window already at or above finalLoad. Do nothing to keep finalLoad.
                    transactions.add(0);
                }
                // Skip further normal buy/sell logic.
                continue;
            }

            // Main strategy. Buy if price is significantly lower than average: at least 5% profit.
            if ((currentPrice < averagePrice * (1 - cost - expectedProfit)) && currentLoad < capacitance && !needToStopBuying) {
                int buyAmount = Math.min(receivingLimit, capacitance - currentLoad);
                int actualChange = (int) Math.round(buyAmount * transmissionEfficiency);
                if (currentLoad + buyAmount <= capacitance && buyAmount <= receivingLimit) {
                    currentLoad += actualChange;
                    profit -= ((buyAmount * currentPrice) + (buyAmount * cost));
                    profit = Math.round(profit * 1000.0) / 1000.0;
                    transactions.add(buyAmount);
                } else {
                    transactions.add(0);
                }
            }

            // Main strategy. Sell if price is significantly higher than average: at least 5% profit.
            else if ((currentPrice > averagePrice * (1 + cost + expectedProfit)) && (currentLoad > 0) && !needToStopSelling) {
                int sellAmount = Math.min(sendingLimit, currentLoad);
                int energyReceived = (int) Math.round(sellAmount * transmissionEfficiency);
                currentLoad -= sellAmount;
                double changeInProfit = ((energyReceived * currentPrice) - (sellAmount * cost));
                profit += changeInProfit;
                profit = Math.round(profit * 1000.0) / 1000.0;
                transactions.add(-sellAmount);

            // Main strategy. Do nothing if price too near to average or limited by capacity.
            } else {
                transactions.add(0);
            }
        }
        return new Transactions(transactions, profit);
    }

    /**
     * Additional first solution method for small timeFrames
     */

    public Result optimalTransactionsWithDP(double[] prices, int capacitance,
                                                           int receivingLimit, int sendingLimit,
                                                           double cost, double transmissionEfficiency,
                                                           int initialLoad, int finalLoad) {
        int timeSteps = prices.length;

        // dp[hour][load]: max profit after considering all actions at "hour".
        double[][] dp = new double[timeSteps + 1][capacitance + 1];
        for (int hour = 0; hour <= timeSteps; hour++) {
            Arrays.fill(dp[hour], Double.NEGATIVE_INFINITY);
        }

        // transaction[hour][load]: record of net action at hour h-1
        int[][] transaction = new int[timeSteps + 1][capacitance + 1];
        for (int hour = 0; hour <= timeSteps; hour++) {
            Arrays.fill(transaction[hour], 0);
        }

        dp[0][initialLoad] = 0.0;

        for (int hour = 0; hour < timeSteps; hour++) {
            double price = prices[hour];
            for (int load = 0; load <= capacitance; load++) {
                double currentProfit = dp[hour][load];
                if (currentProfit == Double.NEGATIVE_INFINITY) {
                    continue;
                }

                // 1. Do Nothing
                if (currentProfit > dp[hour + 1][load]) {
                    dp[hour + 1][load] = currentProfit;
                    transaction[hour + 1][load] = 0;
                }

                // 2. Try buying (from largest to smallest increment)
                for (int buy = receivingLimit; buy >= 1; buy--) {
                    int actualChange = (int) Math.round(buy * transmissionEfficiency);
                    int newLoad = load + actualChange;
                    if (newLoad <= capacitance) {
                        double newProfit = currentProfit - (buy * price + buy * cost);
                        if (newProfit > dp[hour + 1][newLoad]) {
                            dp[hour + 1][newLoad] = newProfit;
                            transaction[hour + 1][newLoad] = buy;
                        }
                    }
                }

                // 3. Try selling (from largest to smallest increment)
                for (int sell = sendingLimit; sell >= 1; sell--) {
                    if (sell <= load) {
                        int actualChange = (int) Math.round(sell * transmissionEfficiency);
                        int newLoad = load - sell;
                        double revenue = (actualChange * price) - (sell * cost);
                        double newProfit = currentProfit + revenue;
                        if (newProfit > dp[hour + 1][newLoad]) {
                            dp[hour + 1][newLoad] = newProfit;
                            transaction[hour + 1][newLoad] = -sell;
                        }
                    }
                }
            }
        }

        double maxProfit = dp[timeSteps][finalLoad];
        if (maxProfit == Double.NEGATIVE_INFINITY) {
            // No reachable scenario for finalLoad
            return new Transactions(new ArrayList<>(), Double.NEGATIVE_INFINITY);
        }

        // Reconstruct the transaction sequence
        Integer[] resultTransactions = new Integer[timeSteps];
        Arrays.fill(resultTransactions, 0);
        int load = finalLoad;
        for (int hour = timeSteps; hour > 0; hour--) {
            int action = transaction[hour][load];
            resultTransactions[hour - 1] = action;
            if (action > 0) {
                int actualChange = (int) Math.round(action * transmissionEfficiency);
                load -= actualChange;
            } else if (action < 0) {
                int sellAmount = -action;
                load += sellAmount;
            }
            // If action == 0, do nothing
        }

        double totalProfit = calculateProfitFromTransactions(prices, Arrays.asList(resultTransactions), cost, transmissionEfficiency);
        return new Transactions(Arrays.asList(resultTransactions), totalProfit);
    }

    /**
     * Helper method for profit calculation
     */
    public double calculateProfitFromTransactions(
            double[] prices, List<Integer> transactions,
            double cost, double transmissionEfficiency) {
        double profit = 0.0;

        for (int hour = 0; hour < prices.length; hour++) {

            int amount = transactions.get(hour);
            double price = prices[hour];

            if (amount > 0) {
                double totalCost = (amount * price) + (amount * cost);
                profit -= totalCost;
                profit = Math.round(profit * 1000.0) / 1000.0;
            } else if (amount < 0) {
                amount = -amount;

                double actualChange = Math.round(amount * transmissionEfficiency);
                double revenue = (actualChange * price) - (amount * cost);

                profit += revenue;
                profit = Math.round(profit * 1000.0) / 1000.0;

            } else {
                // action == 0, do nothing
            }
        }
        return profit;
    }
}
