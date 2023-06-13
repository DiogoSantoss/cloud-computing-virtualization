package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class HealthChecker implements Runnable {

    private final CustomLogger LOGGER = new CustomLogger(HealthChecker.class.getName());

    private static final int TIMER = 10000;

    private AWSInterface awsInterface;

    public HealthChecker(AWSInterface awsInterface) {
        this.awsInterface = awsInterface;
    }

    @Override
    public void run() {
        while (true) {
            this.ping();
            try {
                Thread.sleep(TIMER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void ping() {

        LOGGER.log("Running health check...");

        // Health check every known instance
        Set<InstanceInfo> instances = new HashSet<>();
        instances.addAll(this.awsInterface.getAliveInstances());
        instances.addAll(this.awsInterface.getSuspectedInstances());

        instances.stream().forEach(instance -> {
            try {
                URL url = new URL("http://" + instance.getInstance().getPublicDnsName() + ":8000/test");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuffer response = new StringBuffer();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                    }
                    rd.close();

                    if (response.toString().equals("OK")) {

                        if (this.awsInterface.getSuspectedInstances().contains(instance)) {
                            this.awsInterface.removeSuspectedInstance(instance);
                            this.awsInterface.addAliveInstance(instance);
                            instance.resetMissedHealthChecks();
                            LOGGER.log(
                                    "Instance " + instance.getInstance().getInstanceId() + " is no longer suspected.");
                        }

                    } else {

                        if (this.awsInterface.getAliveInstances().contains(instance)) {
                            LOGGER.log("Instance " + instance.getInstance().getInstanceId() + " is now suspected.");
                            this.awsInterface.removeAliveInstance(instance);
                            this.awsInterface.addSuspectedInstance(instance);
                            instance.incrementMissedHealthChecks();
                        } else {
                            LOGGER.log("Instance " + instance.getInstance().getInstanceId() + " is still suspected.");
                            instance.incrementMissedHealthChecks();
                        }

                    }
                }
            } catch (Exception e) {

                if (this.awsInterface.getAliveInstances().contains(instance)) {
                    LOGGER.log("Instance " + instance.getInstance().getInstanceId() + " is now suspected.");
                    this.awsInterface.removeAliveInstance(instance);
                    this.awsInterface.addSuspectedInstance(instance);
                    instance.incrementMissedHealthChecks();
                } else {
                    LOGGER.log("Instance " + instance.getInstance().getInstanceId() + " is still suspected.");
                    instance.incrementMissedHealthChecks();
                }

            }

            // Terminate instances that have missed 3 health checks
            if (instance.getMissedHealthChecks() >= 3) {
                LOGGER.log(
                        "Terminating instance " + instance.getInstance().getInstanceId() + ", missed 3 health checks.");
                this.awsInterface.terminateInstance(instance);

                // BIG TODO: Redirect requests to other instances
            }
        });

        LOGGER.log("Finished health check. Healthy instances: " + this.awsInterface.getAliveInstances().size()
                + " Suspected instances: " + this.awsInterface.getSuspectedInstances().size() + ".");
    }
}
