package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;

public class Request {

    private enum Endpoint {

        COMPRESSION("compression"),
        SIMULATION("simulation"),
        WAR("war");

        private String endpoint;

        Endpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return this.endpoint;
        }
    }

    private Endpoint endpoint;

    private List<String> arguments;

    /*
     * Receive a URI (e.g. /compression?arg1=val1&arg2=val2&arg3=val3) 
     * and parse it into a Request object.
     */
    public Request(String URI) {

        String[] parts = URI.split("\\?");

        switch (parts[0].split("/")[1]) {
            case "compression":
                this.endpoint = Endpoint.COMPRESSION;
                break;
            case "simulation":
                this.endpoint = Endpoint.SIMULATION;
                break;
            case "war":
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

    public String getURI() {
        return this.toString();
    }

    @Override
    public String toString() {
        return "/" + this.endpoint + "?" + String.join("&", this.arguments);
    }
}
