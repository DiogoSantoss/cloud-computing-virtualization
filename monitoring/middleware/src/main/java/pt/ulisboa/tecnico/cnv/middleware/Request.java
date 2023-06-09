package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;

public class Request {

    public enum Endpoint {

        COMPRESSION("compressImage"),
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
    public Request(String URI) {

        this.originalURI = URI;

        String[] parts = URI.split("\\?");

        switch (parts[0].split("/")[1]) {
            case "compressImage":
                this.endpoint = Endpoint.COMPRESSION;
                break;
            case "simulate":
                this.endpoint = Endpoint.SIMULATION;
                break;
            case "insectwar":
                this.endpoint = Endpoint.WAR;
                break;
        }

        List<String> arguments = new ArrayList<String>();

        String[] unparsedArguments = parts[1].split("&");
        for (String part : unparsedArguments) {
            arguments.add(part.split("=")[1]);
        }

        this.arguments = arguments;
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

    public void setEstimatedCost(double cost) {
        this.estimatedCost = cost;
    }

    public String getLambdaName() {
        return this.endpoint.toString() + "-lambda";
        
    }

    public String getLambdaRequest(){
        
        switch (this.endpoint.toString()) {
            case "compressImage":
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
