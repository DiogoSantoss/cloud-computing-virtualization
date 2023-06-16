package pt.ulisboa.tecnico.cnv.webserver;

import pt.ulisboa.tecnico.cnv.middleware.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.middleware.AutoScaler;
import pt.ulisboa.tecnico.cnv.middleware.HealthChecker;
import pt.ulisboa.tecnico.cnv.middleware.AWSInterface;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    public static void main(String[] args) throws Exception {

        // AWS Interface
        AWSInterface awsInterface = new AWSInterface();

        // LoadBalancer
        LoadBalancerHandler loadBalancer = new LoadBalancerHandler(awsInterface);

        // Health Checker
        HealthChecker healthChecker = new HealthChecker(awsInterface, loadBalancer);
        Thread healthCheckerThread = new Thread(healthChecker);
        healthCheckerThread.start();

        // Auto Scaler
        AutoScaler autoScaler = new AutoScaler(awsInterface);
        Thread autoScalerThread = new Thread(autoScaler);
        autoScalerThread.start();

        // Load Balancer
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", loadBalancer);
        server.start();
        System.out.println("LoadBalancer started on port 8000...");
    }
}
