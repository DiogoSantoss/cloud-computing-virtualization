package pt.ulisboa.tecnico.cnv.webserver;

import pt.ulisboa.tecnico.cnv.middleware.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.middleware.AutoScaler;
import pt.ulisboa.tecnico.cnv.middleware.AWSInterface;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class WebServer {
 
    public static void main(String[] args) throws Exception {

        AWSInterface awsInterface = new AWSInterface();

        // Auto Scaler
        AutoScaler autoScaler = new AutoScaler(awsInterface);
        Thread autoScalerThread = new Thread(autoScaler);
        autoScalerThread.start();
        System.out.println("AutoScaler started...");

        // Load Balancer
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new LoadBalancerHandler(awsInterface));
        server.start();
        System.out.println("LoadBalancer started on port 8000...");
    }
}
