package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class HealthChecker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(HealthChecker.class.getName());

    private static final int TIMER = 1000;

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

        LOGGER.info("Starting health check...");

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
                            LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " became alive.");
                            this.awsInterface.removeSuspectedInstance(instance);
                            instance.resetMissedHealthChecks();
                        } else {
                            LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " is still alive.");
                        }

                    } else {

                        if (this.awsInterface.getAliveInstances().contains(instance)) {
                            LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " is now suspected.");
                            this.awsInterface.removeAliveInstance(instance);
                            this.awsInterface.addSuspectedInstance(instance);
                            instance.incrementMissedHealthChecks();
                        } else {
                            LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " is still suspected.");
                            instance.incrementMissedHealthChecks();
                        }

                    }
                }
            } catch (Exception e) {

                if (this.awsInterface.getAliveInstances().contains(instance)) {
                    LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " is now suspected.");
                    this.awsInterface.removeAliveInstance(instance);
                    this.awsInterface.addSuspectedInstance(instance);
                    instance.incrementMissedHealthChecks();
                } else {
                    LOGGER.info("Instance " + instance.getInstance().getInstanceId() + " is still suspected.");
                    instance.incrementMissedHealthChecks();
                }

            }
        });

        LOGGER.info("Finished health check.");
    }
}
