package pt.ulisboa.tecnico.cnv.webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.logging.Logger;

// Doesn't implement RequestHandler because we don't need to measure
// CPU in lambdas
public class LoadAvgHandler implements HttpHandler, Runnable {

    private final Logger LOGGER = Logger.getLogger(LoadAvgHandler.class.getName());

    private final int QUERY_TIMER = 10_000;

    private final Thread worker;

    private String loadAvg = "0";

    public LoadAvgHandler() {
        this.worker = new Thread(this);
        this.worker.start();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, loadAvg.length());
        OutputStream os = exchange.getResponseBody();
        os.write(loadAvg.getBytes());
        os.close();
    }

    @Override
    public void run() {
        for (;;) {
            try {
                
                BufferedReader reader = new BufferedReader(new FileReader("/proc/loadavg"));
                String line = reader.readLine();
                reader.close();
                String[] averages = line.split(" ");
                synchronized (this) {
                    loadAvg = averages[0];
                }
                Thread.sleep(QUERY_TIMER);

            } catch (Exception e) {
                LOGGER.info("This should not happen");
                e.printStackTrace();
            }
        }
    }
}
