package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;

public class Estimator {

    private final CustomLogger LOGGER = new CustomLogger(Estimator.class.getName());

    private double simulationWorld1;
    private double simulationWorld2;
    private double simulationWorld3;
    private double simulationWorld4;

    private List<Double> insectWarWeights;

    public Estimator() {
        // Precomputed values averaged for all 3 scenarios
        this.simulationWorld1 = 11182.2659;
        this.simulationWorld2 = 41612.3819;
        this.simulationWorld3 = 157492.947;
        this.simulationWorld4 = 157492.947;

        // Precomputed weights for the insect wars
        this.insectWarWeights = new ArrayList<Double>();
        this.insectWarWeights.add(0, 0.5);
        this.insectWarWeights.add(1, 0.3);
        this.insectWarWeights.add(2, 0.2);
    }

    public double estimate(Request request) {
        switch(request.getEndpoint()) {
            case SIMULATION:
                return this.estimateSimulation(request);
            case WAR:
                return this.estimateInsectWars(request);
            case COMPRESSION:
                return this.estimateCompression(request);
            default:
                LOGGER.log("Failed to estimate: Invalid endpoint");
                return 0;
        }
    }

    private double estimateSimulation(Request request) {

        double estimatedCost = 0;

        // simulate?generations=751&world=1&scenario=3
        List<String> arguments = request.getArguments();
        switch (arguments.get(1)) {
            case "1":
                estimatedCost = this.simulationWorld1 * Integer.parseInt(arguments.get(0));
            case "2":
                estimatedCost = this.simulationWorld2 * Integer.parseInt(arguments.get(0));
            case "3":
                estimatedCost = this.simulationWorld3 * Integer.parseInt(arguments.get(0));
            case "4":
                estimatedCost = this.simulationWorld4 * Integer.parseInt(arguments.get(0));
            default:
                LOGGER.log("Failed to estimate: Invalid world number");
                estimatedCost = 0;
        }
        return estimatedCost;
    }

    private double estimateInsectWars(Request request) {

        double estimatedCost = 0;

        // insectwar?max=10&army1=100&army2=100
        List<String> arguments = request.getArguments();
        int max = Integer.parseInt(arguments.get(0));
        int army1 = Integer.parseInt(arguments.get(1));
        int army2 = Integer.parseInt(arguments.get(2));

        int armyDiff = Math.abs(army1 - army2);
        int armyAvg = (army1 + army2) / 2;

        estimatedCost = armyDiff * this.insectWarWeights.get(0) + armyAvg * this.insectWarWeights.get(1) + max * this.insectWarWeights.get(2);
        return estimatedCost;
    }

    private double estimateCompression(Request request) {
        // TODO
        return 0;
    }

}