package pt.ulisboa.tecnico.cnv.middleware;

import java.util.List;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class AutoScaler implements Runnable {
 
    private AWSInterface awsInterface;

    private static final int TIMER = 10000;

    public AutoScaler(AWSInterface awsInterface) {
        this.awsInterface = awsInterface;
        if(awsInterface.getAliveInstances().size() == 0) 
            awsInterface.createInstances(1);
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.scale();
        }
    }

    /*
     * Decide whether to scale up or down based on the average CPU utilization and 
     * average load of all instances.
     */
    public void scale() {

        // Average CPU utilization for each instance
        List<Pair<String, Double>> results = this.awsInterface.queryCPUUtilization();

        // (TODO) Average Load for each instance (based on requests)

        // Compute average CPU utilization over all instances
        double avgCPUUtilization = 0;
        for(Pair<String, Double> result : results) {
            avgCPUUtilization += result.second;
        }
        avgCPUUtilization /= results.size();
        
        // (TODO) Compute average Load over all instances

        if (avgCPUUtilization > 0.8) {
            this.awsInterface.createInstances(1);
        } else if (avgCPUUtilization < 0.2 && this.awsInterface.getAliveInstances().size() > 1) {
            this.awsInterface.terminateInstance();
        }
    }
}
