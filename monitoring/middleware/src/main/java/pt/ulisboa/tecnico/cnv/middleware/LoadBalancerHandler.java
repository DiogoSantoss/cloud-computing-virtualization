package pt.ulisboa.tecnico.cnv.middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoadBalancerHandler implements HttpHandler {

    private final CustomLogger LOGGER = new CustomLogger(LoadBalancerHandler.class.getName());

    private final int MAX_TRIES = 3;

    private final AWSInterface awsInterface;
    private final Estimator estimator;

    public LoadBalancerHandler(AWSInterface awsInterface) {
        super();
        this.awsInterface = awsInterface;
        this.estimator = new Estimator();
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
                t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,OPTIONS,POST");
                t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization,API-Key");
                t.sendResponseHeaders(204, -1);
                return;
            }

            LOGGER.log("Handling request: " + t.getRequestURI().toString());

            Request request = new Request(t.getRequestURI().toString(), t.getRequestBody());

            // Get request (estimated or real) cost
            this.estimateRequestCost(request);

            int tries = MAX_TRIES;
            while (!executeRequest(request, t)) {
                tries--;
                if (tries <= 0) {
                    LOGGER.log("Failed to execute request.");
                    t.sendResponseHeaders(500, -1);
                    break;
                }
            }

            t.close();

        } catch (Exception e) {
            LOGGER.log("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean executeRequest(Request request, HttpExchange t) {

        Optional<InstanceInfo> optInstance = this.getLowestLoadedInstance(request);
        try {
            if (optInstance.isEmpty()) {

                LOGGER.log("No instances available to handle request.");
                LOGGER.log("Calling lambda function");

                String content = request.getLambdaRequest();
                Optional<Pair<String,Integer>> answer = awsInterface.callLambda(request.getLambdaName(), content);

                if (answer.isEmpty()) {
                    return false;
                }

                String ans = answer.get().getFirst();
                Integer responseCode = answer.get().getSecond();

                t.sendResponseHeaders(responseCode, ans.length());
                OutputStream os = t.getResponseBody();
                os.write(ans.toString().getBytes());
                os.close();

                return true;

            } else {
                InstanceInfo instance = optInstance.get();
                instance.getRequests().add(request);

                LOGGER.log("Forwarding request to instance: " + instance.getInstance().getInstanceId());

                HttpURLConnection con = sendRequestToWorker(instance, request, t);
                boolean successful =  replyToClient(con, t);
                instance.getRequests().remove(request);
                return successful;
            }

        } catch (Exception e) {
            LOGGER.log("Error: retrying request");
            e.printStackTrace();
            return false;
        }
    }

    private void estimateRequestCost(Request request) {
        Optional<Statistics> cachedStatistics = this.awsInterface.getFromCache(request);

        if (cachedStatistics.isPresent()) {
            LOGGER.log("Cache hit for " + request.getURI());
            double estimate = cachedStatistics.get().getInstructionCount();
            request.setEstimatedCost(estimate);
            LOGGER.log("Real cost: " + estimate);

        } else {
            LOGGER.log("Cache miss for " + request.getURI());
            double estimate = this.estimator.estimate(request);
            request.setEstimatedCost(estimate);
            LOGGER.log("Estimated cost: " + estimate);

            // Fetch from db in the background (non-blocking)
            this.awsInterface.getFromStatistics(request);
        }

    }

    private HttpURLConnection sendRequestToWorker(InstanceInfo instance, Request request, HttpExchange t)
            throws IOException {
        URL url = new URL("http://" + instance.getInstance().getPublicDnsName() + ":8000" + request.getURI());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(t.getRequestMethod());

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            con.setRequestProperty("Accept", "*");
            OutputStream os = con.getOutputStream();
            os.write(request.getBody());
            os.flush();
            os.close();
        }

        return con;
    }

    private boolean replyToClient(HttpURLConnection con, HttpExchange t) {        
        try {
            int responseCode = con.getResponseCode();

            if (responseCode / 100 != 5) {
                LOGGER.log("Request handled successfully, replying to client...");
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer response = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                }
                rd.close();
    
                t.sendResponseHeaders(responseCode, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.toString().getBytes());
                os.close();
    
                return true;
            }
    
            return false;

        } catch (IOException e) {
            LOGGER.log("Error: retrying request");
            e.printStackTrace();
            return true;
        }
    }
}
