package pt.ulisboa.tecnico.cnv.middleware;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class AWSInterface {

    private static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    private static String AMI_ID = System.getenv("AWS_AMI_ID");
    private static String KEY_NAME = System.getenv("AWS_KEYPAR_NAME");
    private static String SEC_GROUP_ID = System.getenv("AWS_SECURITY_GROUP");

    // Time to wait until the instance is terminated (in milliseconds).
    private static long WAIT_TIME = 1000 * 60 * 10; // 10 minutes
    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 20; // 20 minutes
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; // 10 seconds

    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;

    private Set<Instance> aliveInstances = ConcurrentHashMap.newKeySet();
    private AtomicInteger idx = new AtomicInteger(0);

    public AWSInterface() {
        this.ec2 = AmazonEC2ClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        this.cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();

        this.aliveInstances = queryAliveInstances();
    }

    public Set<Instance> getAliveInstances() {
        return this.aliveInstances;
    }

    public int updateAndGetIdx() {
        return this.idx.updateAndGet(i -> (i + 1) % this.aliveInstances.size());
    }

    /*
     * Blocking
     */
    public List<String> createInstances(int count) {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
        runInstancesRequest.withImageId(AMI_ID)
                .withInstanceType("t2.micro")
                .withMinCount(count)
                .withMaxCount(count)
                .withKeyName(KEY_NAME)
                .withSecurityGroupIds(SEC_GROUP_ID);
        RunInstancesResult runInstancesResult = this.ec2.runInstances(runInstancesRequest);
        String reservationId = runInstancesResult.getReservation().getReservationId();

        List<Instance> newInstances = runInstancesResult.getReservation().getInstances();

        if (newInstances.size() != count) {
            throw new RuntimeException("Error creating instances");
        }

        // wait until all instances are running
        while (newInstances.stream().filter(i -> i.getState().getName().equals("running")).count() != count) {
            newInstances = ec2.describeInstances(
                    new DescribeInstancesRequest()
                            .withFilters(new Filter()
                                    .withName("reservation-id")
                                    .withValues(reservationId)))
                    .getReservations().get(0).getInstances();

            newInstances.forEach(i -> System.out.println(i.getState().getName()));

            try {
                System.out
                        .println(String.format("Waiting for instances to spawn for %d seconds", QUERY_COOLDOWN / 1000));
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.aliveInstances.addAll(newInstances);

        // TODO check if healthy?

        return newInstances.stream().map(i -> i.getInstanceId()).collect(Collectors.toList());
    }

    public void terminateInstance() {
        Instance instance = this.aliveInstances.iterator().next();
        if (instance == null) {
            throw new RuntimeException("No instances to terminate");
        }
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        this.ec2.terminateInstances(termInstanceReq);
    }

    public Set<Instance> queryInstances() {
        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances());
        }
        return instances;
    }

    public Set<Instance> queryAliveInstances() {

        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getState().getName().equals("running")) {
                    instances.add(instance);
                }
            }
        }
        return instances;
    }

    public List<Pair<String, Double>> queryCPUUtilization() {
        Dimension instanceDimension = new Dimension();
        instanceDimension.setName("InstanceId");
        List<Dimension> dims = new ArrayList<Dimension>();
        dims.add(instanceDimension);

        List<Pair<String, Double>> results = new ArrayList<Pair<String, Double>>();

        for (Instance instance : this.aliveInstances) {
            String iid = instance.getInstanceId();

            instanceDimension.setValue(iid);
            GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                    .withStartTime(new Date(new Date().getTime() - OBS_TIME))
                    .withNamespace("AWS/EC2")
                    .withPeriod(60) // seconds
                    .withMetricName("CPUUtilization")
                    .withStatistics("Average")
                    .withDimensions(instanceDimension)
                    .withEndTime(new Date());

            double averageCPUUtilization = this.cloudWatch.getMetricStatistics(request).getDatapoints().stream()
                    .mapToDouble(Datapoint::getAverage).average().orElse(Double.NaN);

            results.add(new Pair<>(iid, averageCPUUtilization));
        }

        return results;
    }

}
