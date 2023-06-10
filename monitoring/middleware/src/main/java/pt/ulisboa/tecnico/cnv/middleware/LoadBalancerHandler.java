package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;

public class LoadBalancerHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(LoadBalancerHandler.class.getName());

    private AWSInterface awsInterface;

    private DynamoDownloader downloader;

    private Estimator estimator;

    public LoadBalancerHandler(AWSInterface awsInterface) {
        super();
        this.awsInterface = awsInterface;
        this.downloader = new DynamoDownloader();
        //this.estimator = new Estimator();
    }

    /*
     * Round Robin algorithm to select the next instance to forward the request to.
     */
    public Optional<InstanceInfo> getNextInstance() {

        List<InstanceInfo> instances = new ArrayList<>(this.awsInterface.getAliveInstances());

        if (instances.size() == 0)
            return Optional.empty();

        int idx = this.awsInterface.updateAndGetIdx();
        return Optional.of(instances.get(idx));
    }

    /*
     * Select the instance with the lowest load to forward the request to.
     * The load is temporarily defined as the number of requests to that instance.
     * NOTE: Ideally, this would still use round robin between equally loaded instances.
     */
    public Optional<InstanceInfo> getLowestLoadedInstance(Request request) {

        List<InstanceInfo> instances = new ArrayList<InstanceInfo>(this.awsInterface.getAliveInstances());

        if (instances.size() == 0)
            return Optional.empty();

        double min = Double.MAX_VALUE;
        InstanceInfo minInstance = null;

        for (InstanceInfo instance: instances) {
            double size = instance.getRequests().stream().mapToDouble(r -> r.getEstimatedCost()).sum();
            if (size < min) {
                min = size;
                minInstance = instance;
            }
        }

        if (minInstance == null)
            return Optional.empty();
        else 
            return Optional.of(minInstance);
    }

    @Override
    public void handle(HttpExchange t) {

        try {

            LOGGER.info("Received request: " + t.getRequestURI().toString());
    
            Request request = new Request(t.getRequestURI().toString());

            // Get request (estimated or real) cost
            Optional<Statistics> cachedStatistics = this.downloader.getFromCache(request);
            if (cachedStatistics.isPresent()) {
                request.setEstimatedCost(cachedStatistics.get().getInstructionCount());
                LOGGER.info("Cache hit for " + request.getURI() + " with cost " + request.getEstimatedCost());
            } else {
                double estimate = this.estimator.estimate(request);
                request.setEstimatedCost(estimate);
                LOGGER.info("Cache miss for " + request.getURI() + " with (estimated) cost " + request.getEstimatedCost());

                // In the background fetch the statistics from DynamoDB
                Optional<Statistics> realCost = this.downloader.getFromStatistics(request);
                if (realCost.isPresent()) {
                    request.setEstimatedCost(realCost.get().getInstructionCount());
                    LOGGER.info("Fetch real cost for " + request.getURI() + " with cost " + request.getEstimatedCost());
                } else {
                    LOGGER.info("Failed to fetch real cost for " + request.getURI());
                }
            }

            //Call lambda - example
            // String response = awsInterface.callLambda(request.getLambdaName(), request.getLambdaRequest());

            // t.sendResponseHeaders(200, response.length());
            // t.getResponseBody().write(response.getBytes());
            // t.close();

            Optional<InstanceInfo> optInstance = this.getLowestLoadedInstance(request);
            if (optInstance.isEmpty()) {
               //Maybe launch new instance or call lambda function
               LOGGER.info("No instances available to handle request.");
               t.sendResponseHeaders(500, 0);
               t.close();
               return;
            }
            InstanceInfo instance = optInstance.get();
    
            instance.getRequests().add(request);
    
            LOGGER.info("Forwarding request to instance: " + instance.getInstance().getInstanceId());
    
            HttpURLConnection con = sendRequestToWorker(instance, request, t);
            
            instance.getRequests().remove(request);

            replyToClient(con, t);

        } catch (Exception e) {
            LOGGER.info("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private HttpURLConnection sendRequestToWorker(InstanceInfo instance, Request request, HttpExchange t) throws IOException {
        URL url = new URL("http://" + instance.getInstance().getPublicDnsName() + ":8000" + request.getURI());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(t.getRequestMethod());
        return con;
    }

    private void replyToClient(HttpURLConnection con, HttpExchange t) throws IOException {
        if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();

            t.sendResponseHeaders(200, response.length());
            t.getResponseBody().write(response.toString().getBytes());
            t.close();
        } else {
            t.sendResponseHeaders(500, 0);
            t.close();
        }
    }
}
