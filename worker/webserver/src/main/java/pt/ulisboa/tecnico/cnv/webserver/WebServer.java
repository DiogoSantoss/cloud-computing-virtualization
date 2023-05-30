package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.util.Scanner;

import pt.ulisboa.tecnico.cnv.foxesrabbits.SimulationHandler;
import pt.ulisboa.tecnico.cnv.compression.CompressImageHandlerImpl;
import pt.ulisboa.tecnico.cnv.insectwar.WarSimulationHandler;
import pt.ulisboa.tecnico.cnv.webserver.Uploader;
import pt.ulisboa.tecnico.cnv.javassist.tools.Metrics;

public class WebServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/simulate", new SimulationHandler());
        server.createContext("/compressimage", new CompressImageHandlerImpl());
        server.createContext("/insectwar", new WarSimulationHandler());
        server.start();

        Uploader uploader = new Uploader();
        Thread uploaderThread = new Thread(uploader);
        uploaderThread.start();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.nextLine();
            switch (input) {
                case "see":
                    Metrics.printStatistics();
                    break;
                case "metrics":
                    Metrics.writeStatisticsToCsv();
                    break;
            }
        }
    }
}
