package pt.ulisboa.tecnico.cnv.middleware;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Base64;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

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
    private byte[] body;
    private Endpoint endpoint;
    private List<String> arguments;
    private double estimatedCost;

    private HttpExchange exchange;

    /*
     * Receive a URI (e.g. /compression?arg1=val1&arg2=val2&arg3=val3)
     * and parse it into a Request object.
     */
    public Request(String URI, InputStream body, HttpExchange exchange) {

        this.arguments = new ArrayList<String>();
        this.originalURI = URI;
        this.exchange = exchange;
        this.estimatedCost = 0;

        String[] parts = URI.split("\\?");

        switch (parts[0]) {
            case "/compressimage":
                this.endpoint = Endpoint.COMPRESSION;
                this.parseArgumentsBody(body);
                this.originalURI += "?size=" + this.arguments.get(4) + "x" + this.arguments.get(5) + "&format="
                        + this.arguments.get(1) + "&compression=" + this.arguments.get(2);
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
        // Result syntax:
        // targetFormat:<targetFormat>;compressionFactor:<factor>;data:image/<currentFormat>;base64,<encoded
        // image>
        String result = new BufferedReader(new InputStreamReader(bodyStream)).lines().collect(Collectors.joining("\n"));
        this.body = result.getBytes();
        String[] resultSplits = result.split(",");

        int imagePixeis;
        int width, height;
        String encodedImage = resultSplits[1];
        byte[] decodedImage = Base64.getDecoder().decode(encodedImage);

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedImage);
            BufferedImage bi = ImageIO.read(bais);
            width = bi.getWidth();
            height = bi.getHeight();
            imagePixeis = width * height;
        } catch (IOException e) {
            e.printStackTrace();
            imagePixeis = 0;
            width = 0;
            height = 0;
        }

        this.arguments.add(resultSplits[1]); // encoded image
        this.arguments.add(resultSplits[0].split(":")[1].split(";")[0]); // targetFormat
        this.arguments.add(resultSplits[0].split(":")[2].split(";")[0]); // compressionFactor
        this.arguments.add(String.valueOf(imagePixeis)); // imagePixeis
        this.arguments.add(String.valueOf(width)); // width
        this.arguments.add(String.valueOf(height)); // height
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

    public String getLambdaRequest() {

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

    public byte[] getBody() {
        return body;
    }

    public HttpExchange getExchange() {
        return exchange;
    }
}
