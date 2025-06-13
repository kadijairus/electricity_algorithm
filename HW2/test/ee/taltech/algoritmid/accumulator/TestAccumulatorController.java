package ee.taltech.algoritmid.accumulator;

import org.junit.*;

import java.util.*;

public class TestAccumulatorController {

    List<Double> listOfPrices = List.of(
            10.926, 12.97, 11.909, 11.632, 11.46, 11.998,
            15.317, 14.817, 16.502, 16.8, 15.33, 14.335,
            14.195, 14.196, 14.596, 15.5, 16.915, 16.702,
            19.272, 17.962, 16.758, 14.701, 13.678, 13.439
    );
    List<Double> listOfPricesTwelveHours = List.of(
            10.926, 12.97, 15.33, 14.335, 10.005, 10.006,
            17.962, 16.758, 14.701, 8.678, 8.439, 9.444
    );
    List<Integer> moodleActions = List.of(+2, 0, +2, +2, +2, +2,
            -2, 0, -2, -2, -2, +2,
            +2, +2, +2, 0, -2, -2,
            -2, -2, -2, 0, 0, 0);
    List<Integer> lessEfficientMoodleActions = List.of(+3, 0, +2, +2, +2, +2,
            -3, -2, -2, -2, -1, +2,
            +2, +2, +2, +2, -2, -2,
            -2, -2, -2, 0, 0, 0);
    HW2 controller = new HW2();
    double[] prices = new double[24]; // Prices per 24 hours, cents per kWh, hours come from index
    double[] pricesTwelveHours = new double[12];
    double[] twoDaysPrices = new double[48];
    double[] threeDaysPrices = new double[72];
    double[] pricesWeek = new double[7 * 24];
    Integer capacitance = 10; // How much kWh can fit to accumulator
    int receivingLimit = 3; // How many kWh you are allowed to buy
    int sendingLimit = 3;
    double cost = 0.1; // How much you have to pay for traffic cents per kWh
    double transmissionEfficiency = 0.8; // Inefficiency coefficient that reduces kWh
    int initialLoad = 0; // Initial kWh in the accumulator in the beginning
    int finalLoad = 10; // Minimal allowed kWh in the accumulator in the end

    @Before
    public void setUp() {
        // Populate the prices array with the values from listOfPrices
        for (int i = 0; i < 24; i++) {
            prices[i] = listOfPrices.get(i);
            twoDaysPrices[i] = prices[i];
            threeDaysPrices[i] = prices[i];
            threeDaysPrices[i + 24] = prices[i];
            threeDaysPrices[i + 48] = prices[i];
            twoDaysPrices[i + 24] = prices[i];
        }

        for (int i = 0; i < 10; i++) {
            pricesTwelveHours[i] = listOfPricesTwelveHours.get(i);
        }

        for (int d = 0; d < 7; d++) {
            int dayStart = d * 24; // starting index for day d in the week array
            // Copy the first 12 hours from pricesTwelveHours
            for (int i = 0; i < 12; i++) {
                pricesWeek[dayStart + i] = pricesTwelveHours[i];
            }
            // Copy the next 12 hours from prices
            for (int i = 0; i < 12; i++) {
                pricesWeek[dayStart + 12 + i] = prices[i];
            }
        }
    }

    private Integer calculateFinalLoad(List<Integer> transactions, Integer initialLoad) {
        int finalLoad = initialLoad;
        for (int transaction : transactions) {
            finalLoad += transaction;
        }
        return finalLoad;
    }

    @Test
    public void testMoodleExampleFinalLoadMet() {
        long start = System.nanoTime();
        Result result = (controller.findStrategy(prices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        System.out.println("Actual transactions single day: \n" + result.getTransactions()
                + "\nActual profit: " + result.getTotalIncome());
        System.out.println("""
                Moodle example optimal transactions:
                [+2, 0, +2, +2, +2, +2, -2, 0, -2, -2, -2, +2, +2, +2, +2, 0, -2, -2, -2, -2, -2, 0, 0, 0]
                Moodle example optimal profit: 69.022""");

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), initialLoad);
        assert(actualFinalLoad >= finalLoad);
    }

    @Test
    public void testTwoDaysFinalLoadMetInitialLoadZero() {
        Result result = (controller.findStrategy(twoDaysPrices, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                0, finalLoad));
        System.out.println("Transactions 2 days: \n" + result.getTransactions()
                + "\nProfit: " + result.getTotalIncome());

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), 0);
        assert(actualFinalLoad >= finalLoad);
    }

    @Test
    public void testThreeDaysFinalLoadMetInitialLoadTen() {
        Result result = (controller.findStrategy(threeDaysPrices, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                10, finalLoad));
        System.out.println("Transactions 3 days: \n" + result.getTransactions()
                + "\nProfit: " + result.getTotalIncome());

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), 10);
        assert(actualFinalLoad >= finalLoad);
    }

    @Test
    public void testOptimalMatrixProfitableAndFinalLoadMetWithinTwelveHours() {
        Result result = (controller.optimalTransactionsWithDP(pricesTwelveHours, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                2, 5));
        double profit = result.getTotalIncome();
        System.out.println("Transactions 12 hours: \n" + result.getTransactions()
                + "\nProfit: " + profit);

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), 2);
        assert(actualFinalLoad >= 5);
        assert(profit > 0);
    }

    @Test
    public void testOptimalMatrixProfitableAndFinalLoadThreeDays() {
        Result result = (controller.optimalTransactionsWithDP(threeDaysPrices, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                initialLoad, finalLoad));
        double profit = result.getTotalIncome();
        System.out.println("Transactions 3 days: \n" + result.getTransactions()
                + "\nProfit: " + profit);

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), initialLoad);
        assert(actualFinalLoad >= finalLoad);
        assert(profit > 0);
    }

    @Test
    public void testTimeOneWeekDPStrategyProfitableAndFinalLoadMet() {
        Result result = (controller.optimalTransactionsWithDP(pricesWeek, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                initialLoad, finalLoad));
        double profit = result.getTotalIncome();
        System.out.println("Transactions 1 week: \n" + result.getTransactions()
                + "\nProfit: " + profit);

        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), initialLoad);

        assert(actualFinalLoad >= finalLoad);
        assert(profit > 0);
    }

    @Test
    public void testTimeOneWeekAverageStrategyFinalLoadMet() {
        Result result = (controller.findStrategyFirstSolutionBestTransactionsInWindow(pricesWeek, capacitance,
                receivingLimit, sendingLimit,
                cost, transmissionEfficiency,
                initialLoad, finalLoad, 24, 0.05));

        double profit = result.getTotalIncome();
        System.out.println("Transactions 1 week: \n" + result.getTransactions()
                + "\nProfit: " + profit);
        int actualFinalLoad = calculateFinalLoad(result.getTransactions(), initialLoad);

        assert(actualFinalLoad >= finalLoad);
    }

    @Test
    public void calculatesCorrectProfitFromTransactions() {
        double profit = controller.calculateProfitFromTransactions(prices, moodleActions,
        cost, transmissionEfficiency);
        assert(profit == 69.022);

        System.out.println(lessEfficientMoodleActions);
        profit = controller.calculateProfitFromTransactions(prices, lessEfficientMoodleActions,
                cost, transmissionEfficiency);
        assert(profit == 40.90);
    }

    @Test
    public void testTimeOneDay() {
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(prices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }
        long end = System.nanoTime();
        long time = (end - start) / 1000;
        System.out.println("Average of 24 h case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / 24);
    }

    @Test
    public void testTimeTwoDays() {
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(twoDaysPrices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }

        long end = System.nanoTime();
        long time = (end - start) / 1000;

        System.out.println("Average of 48 h case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / 48);
    }

    @Test
    public void testTimeThreeDays() {
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(threeDaysPrices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }

        long end = System.nanoTime();
        long time = (end - start) / 1000;

        System.out.println("Average of 72 h case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / 72);
    }

    @Test
    public void testTimeOneWeek() {
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(pricesWeek, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }

        long end = System.nanoTime();
        long time = (end - start) / 1000;

        System.out.println("Average of one week case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / (7 * 24));
    }

    @Test
    public void testTimeOneMonth() {
        int fourWeeks = 24 * 7 * 4;
        double[] monthPrices = new double[fourWeeks];
        for (int w = 0; w < 4; w++) {
            for (int d = 0; d < 7; d++) {
                for (int h = 0; h < 24; h++) {
                    int index = (w * 7 * 24) + (d * 24) + h;
                    monthPrices[index] = prices[h];
                }
            }
        }
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(monthPrices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }

        long end = System.nanoTime();
        long time = (end - start) / 1000;

        System.out.println("Average of one month case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / fourWeeks);
    }

    @Test
    public void testTimeOneYear() {
        int hoursInDay = 24;
        int daysInWeek = 7;
        int weeksInYear = 52;
        int totalHoursInYear = hoursInDay * daysInWeek * weeksInYear;
        double[] yearPrices = new double[totalHoursInYear];
        for (int w = 0; w < weeksInYear; w++) {
            for (int d = 0; d < daysInWeek; d++) {
                for (int h = 0; h < hoursInDay; h++) {
                    int index = (w * daysInWeek * hoursInDay)
                            + (d * hoursInDay)
                            + h;
                    yearPrices[index] = prices[h];
                }
            }
        }
        System.out.println(yearPrices.length);
        long start = System.nanoTime();
        Result result;
        for (int i = 0; i < 1000; i++) {
            result = (controller.findStrategy(yearPrices, capacitance,
                    receivingLimit, sendingLimit,
                    cost, transmissionEfficiency,
                    initialLoad, finalLoad));
        }
        long end = System.nanoTime();

        long time = (end - start) / 1000;

        System.out.println("Average of one month case, time in nanoseconds: " + time);
        System.out.println("Nanoseconds per n: " + time / totalHoursInYear);
    }
}
