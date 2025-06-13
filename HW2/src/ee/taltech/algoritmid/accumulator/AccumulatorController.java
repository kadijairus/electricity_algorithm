package ee.taltech.algoritmid.accumulator;

public interface AccumulatorController {

    Result findStrategy(double[] prices, int capacitance, int receivingLimit, int sendingLimit, double cost, double transmissionEfficiency, int initialLoad, int finalLoad);

}