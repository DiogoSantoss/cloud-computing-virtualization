package pt.ulisboa.tecnico.cnv.middleware;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

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
        this.insectWarWeights.add(0, 0.01);
        this.insectWarWeights.add(1, 0.48);
        this.insectWarWeights.add(2, 0.51);
    }

    public double estimate(Request request) {
        double estimatedCost = 0;
        switch (request.getEndpoint()) {
            case SIMULATION:
                estimatedCost = this.estimateSimulation(request);
                break;
            case WAR:
                estimatedCost = this.estimateInsectWars(request);
                break;
            case COMPRESSION:
                estimatedCost = this.estimateCompression(request);
                break;
            default:
                estimatedCost = 0;
        }

        LOGGER.log("Estimated cost for " + request.toString() + " is " + estimatedCost);
        return estimatedCost;
    }

    private double estimateSimulation(Request request) {

        double estimatedCost = 0;

        // simulate?generations=751&world=1&scenario=3
        List<String> arguments = request.getArguments();
        switch (arguments.get(1)) {
            case "1":
                estimatedCost = this.simulationWorld1 * Integer.parseInt(arguments.get(0));
                break;
            case "2":
                estimatedCost = this.simulationWorld2 * Integer.parseInt(arguments.get(0));
                break;
            case "3":
                estimatedCost = this.simulationWorld3 * Integer.parseInt(arguments.get(0));
                break;
            case "4":
                estimatedCost = this.simulationWorld4 * Integer.parseInt(arguments.get(0));
                break;
            default:
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

        estimatedCost = (armyDiff * this.insectWarWeights.get(0) + armyAvg * this.insectWarWeights.get(1)
                + max * this.insectWarWeights.get(2)) * 10000000;
        return estimatedCost;
    }

    private double estimateCompression(Request request) {

        int imagePixeis;
        String encodedImage = request.getArguments().get(0);
        byte[] decodedImage = Base64.getDecoder().decode(encodedImage);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedImage);
            BufferedImage bi = ImageIO.read(bais);
            imagePixeis = bi.getWidth() * bi.getHeight();
        } catch (IOException e) {
            LOGGER.log(e.getMessage());
            e.printStackTrace();
            return 0;
        }

        String targetFormat = request.getArguments().get(1);
        int compressionFactor = Integer.parseInt(request.getArguments().get(2));

        int estimatedCost;
        switch (targetFormat) {
            case "PNG":
                estimatedCost = (int) Math.ceil(276958.771);
                break;
            case "JPEG":
                double baseSlope = 0.00268249844;
                double maxSlope = 0.0266551814;
                double slope = baseSlope + (maxSlope - baseSlope) * compressionFactor;
                estimatedCost = (int) Math.ceil(slope * imagePixeis + 23000);
                break;
            case "BMP": // compression doens't affect
                estimatedCost = (int) Math.ceil(imagePixeis * 0.358 + 26088.4532);
                break;
            default:
                estimatedCost = 0;
        }

        return estimatedCost;
    }

}