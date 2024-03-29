package pt.ulisboa.tecnico.cnv.middleware;

import java.util.Iterator;
import java.util.List;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class AutoScaler implements Runnable {

    private final CustomLogger LOGGER = new CustomLogger(AutoScaler.class.getName());

    private static final int SCALER_TIMER = 10000; // 10 seconds

    private AWSInterface awsInterface;

    public AutoScaler(AWSInterface awsInterface) {
        this.awsInterface = awsInterface;
        if (awsInterface.getAliveInstances().size() == 0)
            awsInterface.createInstances(1);
    }

    @Override
    public void run() {
        while (true) {
            this.scale();
            try {
                Thread.sleep(SCALER_TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Decide whether to scale up or down based on the average CPU utilization of
     * all instances.
     * Instances are terminated based on their load to avoid terminating an instance
     * that is
     * currently processing a lot of requests.
     */
    private void scale() {

        // Average CPU utilization for each instance
        // List<Pair<String, Double>> results = this.awsInterface.queryCPUUtilization();
        List<Pair<String, Double>> results = this.awsInterface.queryCPUUtilizationHomeMade();

        double avgCPUUtilization = 0;
        for (Pair<String, Double> result : results) {
            avgCPUUtilization += result.getSecond();
        }
        avgCPUUtilization /= results.size();

        if (Double.isNaN(avgCPUUtilization)) {
            LOGGER.log("No CloudWatch data available.");
            return;
        }

        results.stream()
                .forEach(result -> LOGGER
                        .log("Instance " + result.getFirst() + " CPU Utilization: " + result.getSecond() + "%"));

        LOGGER.log("Average CPU Utilization: " + avgCPUUtilization + " (" + results.size() + " instances)");

        this.awsInterface.getAliveInstances().forEach(instance -> LOGGER
                .log("Instance " + instance.getInstance().getInstanceId() + " with load " + instance.getLoad() + " ("
                        + instance.getRequests().size() + " requests)"));

        // Total requests enables us to avoid scaling up after a huge wave of requests
        int totalRequests = 0;
        Iterator<InstanceInfo> instance = this.awsInterface.getAliveInstances().iterator();
        while(instance.hasNext()) {
            totalRequests += instance.next().getRequests().size();
        }

        if ((avgCPUUtilization > 80 && totalRequests != 0) || this.awsInterface.getAliveInstances().size() == 0) {
            this.scaleUp();
        } else if (avgCPUUtilization < 20 && this.awsInterface.getAliveInstances().size() > 1) {
            this.scaleDown();
        }
    }

    /*
     * Increase the number of instances by 10%
     */
    private void scaleUp() {
        LOGGER.log("Scaling up...");
        int numberOfInstances = this.awsInterface.getAliveInstances().size();
        int newNumberOfInstances = (int) Math.ceil(numberOfInstances * 1.1);
        int numberOfInstancesToCreate = newNumberOfInstances - numberOfInstances;
        this.awsInterface.createInstances(numberOfInstancesToCreate);
    }

    /*
     * Decrease the number of instances by 10%
     */
    private void scaleDown() {
        LOGGER.log("Scaling down...");
        int numberOfInstances = this.awsInterface.getAliveInstances().size();
        int newNumberOfInstances = (int) Math.ceil(numberOfInstances * 0.1);
        this.awsInterface.terminateInstances(newNumberOfInstances);
    }
}
