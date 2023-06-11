package pt.ulisboa.tecnico.cnv.middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class LoadBalancerHandler implements HttpHandler {

    private static final int MAX_LAMBDA_REQUESTS = 10;

    private static final Logger LOGGER = Logger.getLogger(LoadBalancerHandler.class.getName());

    private AWSInterface awsInterface;

    private DynamoDownloader downloader;

    private Estimator estimator;

    private AtomicInteger currentLambdaRequests;

    public LoadBalancerHandler(AWSInterface awsInterface) {
        super();
        this.awsInterface = awsInterface;
        this.downloader = new DynamoDownloader();
        this.estimator = new Estimator();
        this.currentLambdaRequests = new AtomicInteger(0);
    }

    /*
     * Round Robin algorithm to select the next instance to forward the request to.
     * Currently not used.
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
     * NOTE: Ideally, this would still use round robin between equally loaded
     * instances.
     */
    public Optional<InstanceInfo> getLowestLoadedInstance(Request request) {

        List<InstanceInfo> instances = new ArrayList<InstanceInfo>(this.awsInterface.getAliveInstances());

        if (instances.size() == 0)
            return Optional.empty();

        double min = Double.MAX_VALUE;
        InstanceInfo minInstance = null;

        for (InstanceInfo instance : instances) {
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

            // Handling CORS
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
                t.sendResponseHeaders(204, -1);
                return;
            }

            LOGGER.info("Received request: " + t.getRequestURI().toString());

            Request request = new Request(t.getRequestURI().toString(), t.getRequestBody());
            
            // Get request (estimated or real) cost
            //this.estimateRequestCost(request);

            Optional<InstanceInfo> optInstance = this.getLowestLoadedInstance(request);
            if (optInstance.isEmpty()) {

                LOGGER.info("No instances available to handle request.");
                
                //TODO: Test if counter is correct
                if (this.currentLambdaRequests.get() >= MAX_LAMBDA_REQUESTS) {
                    LOGGER.info("Max lambda requests reached.");
                    //TODO: Maybe launch new instance
                    
                    t.sendResponseHeaders(500, 0);
                    t.close();
                } else {
                    LOGGER.info("Calling lambda function for request: " + request.getURI());

                    this.currentLambdaRequests.incrementAndGet();

                    String content = request.getLambdaRequest();
                    System.out.println("Content: " + content);

                    String response = awsInterface.callLambda(request.getLambdaName(), content);

                    this.currentLambdaRequests.decrementAndGet();

                    if (response == null) {
                        LOGGER.info("Error calling lambda function.");
                        t.sendResponseHeaders(500, 0);
                        t.close();
                        
                    } else {
                        //LOGGER.info("Lambda function returned: " + response);
                        t.sendResponseHeaders(200, response.length());
                        t.getResponseBody().write(response.getBytes());
                        t.close();
                    }
                }

            } else {
                InstanceInfo instance = optInstance.get();
    
                instance.getRequests().add(request);
        
                LOGGER.info("Forwarding request to instance: " + instance.getInstance().getInstanceId());
        
                HttpURLConnection con = sendRequestToWorker(instance, request, t);

                instance.getRequests().remove(request);

                replyToClient(con, t);   
            }

        } catch (Exception e) {
            LOGGER.info("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void estimateRequestCost(Request request) {
        double estimate;
        Optional<Statistics> cachedStatistics = this.downloader.getFromCache(request);

        if (cachedStatistics.isPresent()) {
            estimate = cachedStatistics.get().getInstructionCount();

            LOGGER.info("Cache hit for " + request.getURI());
        } else {
            estimate = this.estimator.estimate(request);
            LOGGER.info("Cache miss for " + request.getURI());

            // In the background fetch the statistics from DynamoDB
            Optional<Statistics> realCost = this.downloader.getFromStatistics(request);

            if (realCost.isPresent()) {
                estimate = realCost.get().getInstructionCount();

                LOGGER.info("Fetch real cost for " + request.getURI());
            } else {
                LOGGER.info("Failed to fetch real cost for " + request.getURI());
            }
        }

        request.setEstimatedCost(estimate);
        LOGGER.info("Estimated cost: " + request.getEstimatedCost());
    }


    private HttpURLConnection sendRequestToWorker(InstanceInfo instance, Request request, HttpExchange t) throws IOException {
        URL url = new URL("http://" + instance.getInstance().getPublicDnsName() + ":8000" + request.getURI());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(t.getRequestMethod());
        
        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            System.out.println("POST");
            
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            OutputStream os = con.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(t.getRequestBody().toString());
            osw.flush();
            osw.close();
            os.close();
        }

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
            OutputStream os = t.getResponseBody();
            os.write(response.toString().getBytes());
            os.close();
            t.close();
        } else {
            t.sendResponseHeaders(500, 0);
            t.close();
        }
    }
}
