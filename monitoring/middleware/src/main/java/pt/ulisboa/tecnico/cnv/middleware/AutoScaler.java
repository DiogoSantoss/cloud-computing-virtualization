package pt.ulisboa.tecnico.cnv.middleware;

import java.util.List;
import java.util.logging.Logger;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class AutoScaler implements Runnable {
 
    private static final Logger LOGGER = Logger.getLogger(AutoScaler.class.getName());
    
    private static final int TIMER = 10000;

    private AWSInterface awsInterface;

    public AutoScaler(AWSInterface awsInterface) {
        this.awsInterface = awsInterface;
        if(awsInterface.getAliveInstances().size() == 0) 
            awsInterface.createInstances(1);
    }

    @Override
    public void run() {
        while(true) {
            this.scale();
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Decide whether to scale up or down based on the average CPU utilization and 
     * average load of all instances.
     */
    public void scale() {

        System.out.println("--------------------");

        // Average CPU utilization for each instance
        List<Pair<String, Double>> results = this.awsInterface.queryCPUUtilization();

        // (TODO) Average Load for each instance (based on requests)

        double avgCPUUtilization = 0;
        for(Pair<String, Double> result : results) {
            avgCPUUtilization += result.second;
        }
        avgCPUUtilization /= results.size();

        if(Double.isNaN(avgCPUUtilization)) {
            LOGGER.info("No data yet available...");
            return;
        }

        LOGGER.info("Average CPU Utilization: " + avgCPUUtilization + " (" + results.size() + " instances)");
        
        // (TODO) Compute average Load over all instances
        if (avgCPUUtilization > 80) {
            this.awsInterface.createInstances(1);
        } else if (avgCPUUtilization < 20 && this.awsInterface.getAliveInstances().size() > 1) {
            this.awsInterface.terminateInstance();
        }
    }
}
