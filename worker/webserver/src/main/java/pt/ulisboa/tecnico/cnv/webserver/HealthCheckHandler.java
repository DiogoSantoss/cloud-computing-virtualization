package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

// Doesn't implement RequestHandler because we don't need to measure
// CPU in lambdas

public class HealthCheckHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange r) throws IOException {
        String response = "OK";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}