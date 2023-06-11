package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Request {

    public enum Endpoint {

        COMPRESSION("compressimage"),
        SIMULATION("simulate"),
        WAR("insectwar");

        private String endpoint;

        Endpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return this.endpoint;
        }
    }

    private String originalURI;
    private Endpoint endpoint;
    private List<String> arguments;
    private double estimatedCost;

    /*
     * Receive a URI (e.g. /compression?arg1=val1&arg2=val2&arg3=val3) 
     * and parse it into a Request object.
     */
    public Request(String URI, InputStream body) {

        this.arguments = new ArrayList<String>();
        this.originalURI = URI;

        String[] parts = URI.split("\\?");

        switch (parts[0]) {
            case "/compressimage":
                this.endpoint = Endpoint.COMPRESSION;
                this.parseArgumentsBody(body);
                break;
            case "/simulate":
                this.endpoint = Endpoint.SIMULATION;
                this.parseArgumentsURI(parts[1]);   
                break;
            case "/insectwar":
                this.endpoint = Endpoint.WAR;
                this.parseArgumentsURI(parts[1]);
                break;
        }
    }

    private void parseArgumentsURI(String argumentsURI) {
        String[] unparsedArguments = argumentsURI.split("&");

        for (String part : unparsedArguments) {
            this.arguments.add(part.split("=")[1]);
        }
    }

    private void parseArgumentsBody(InputStream bodyStream) {
        // Result syntax: targetFormat:<targetFormat>;compressionFactor:<factor>;data:image/<currentFormat>;base64,<encoded image>
        String result = new BufferedReader(new InputStreamReader(bodyStream)).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");

        this.arguments.add(resultSplits[1]); // encoded image
        this.arguments.add(resultSplits[0].split(":")[1].split(";")[0]); // targetFormat
        this.arguments.add(resultSplits[0].split(":")[2].split(";")[0]); // compressionFactor
    }

    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public String getURI() {
        return this.toString();
    }

    public double getEstimatedCost() {
        return this.estimatedCost;
    }

    public void setEstimatedCost(double cost) {
        this.estimatedCost = cost;
    }

    public String getLambdaName() {
        return this.endpoint.toString() + "-lambda";
        
    }

    public String getLambdaRequest(){
        
        switch (this.endpoint.toString()) {
            case "compressimage":
                return String.format("{\"body\": \"%s\", \"targetFormat\": \"%s\", \"compressionFactor\": \"%s\"}", 
                    this.arguments.get(0), this.arguments.get(1), this.arguments.get(2));
            
            case "insectwar":
                return String.format("{\"max\": \"%s\", \"army1\": \"%s\", \"army2\": \"%s\"}", 
                    this.arguments.get(0), this.arguments.get(1), this.arguments.get(2));
            
            case "simulate":
                return String.format("{\"generations\": \"%s\", \"world\": \"%s\", \"scenario\": \"%s\"}", 
                    this.arguments.get(0), this.arguments.get(1), this.arguments.get(2));
            
            default:
                return "";
        }  
    }

    @Override
    public String toString() {
        return this.originalURI;
    }
}
