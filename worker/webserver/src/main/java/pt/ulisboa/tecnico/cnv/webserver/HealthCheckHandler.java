package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HealthCheckHandler implements HttpHandler, RequestHandler<Map<String, String>, String> {
    
    @Override
    public void handle(HttpExchange r) throws IOException {
        String response = "OK";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    @Override
    public String handleRequest(Map<String, String> event, com.amazonaws.services.lambda.runtime.Context context) {
        return "OK";
    }
}
