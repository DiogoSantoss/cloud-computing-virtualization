package pt.ulisboa.tecnico.cnv.middleware;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;

import pt.ulisboa.tecnico.cnv.middleware.Utils.Pair;

public class AWSInterface {

    private final CustomLogger LOGGER = new CustomLogger(AWSInterface.class.getName());

    private final String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    private final String AMI_ID = System.getenv("AWS_AMI_ID");
    private final String KEY_NAME = System.getenv("AWS_KEYPAR_NAME");
    private final String SEC_GROUP_ID = System.getenv("AWS_SECURITY_GROUP");
    private final String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");

    // Total observation time in milliseconds.
    private static long OBS_TIME = 1000 * 60 * 10; // 10 minutes
    // Time between each query for instance state
    private static long QUERY_COOLDOWN = 1000 * 10; // 10 seconds

    private AmazonEC2 ec2;
    private AmazonCloudWatch cloudWatch;
    // private LambdaClient lambdaClient;
    private AmazonDynamoDB dynamoDB;

    private Set<InstanceInfo> aliveInstances = new HashSet<InstanceInfo>();
    private AtomicInteger idx = new AtomicInteger(0);

    private Set<InstanceInfo> suspectedInstances = new HashSet<InstanceInfo>();

    // Cache for statistics
    private final int CACHE_CAPACITY = 512;
    private final Queue<Statistics> lruCache = new ConcurrentLinkedQueue<>();
    private final Map<String, Statistics> cacheItems = new ConcurrentHashMap<>();

    // maybe for lambda calls
    // private Set<InstanceInfo> pendingInstances = new HashSet<InstanceInfo>();

    public AWSInterface() {
        this.ec2 = AmazonEC2ClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        this.cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        // this.lambdaClient =
        // LambdaClient.builder().credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();

        this.createTableIfNotExists();

        this.aliveInstances = queryAliveInstances();
        LOGGER.log("Alive instances: " + this.aliveInstances.size());
    }

    public Set<InstanceInfo> getAliveInstances() {
        return this.aliveInstances;
    }

    public void addAliveInstance(InstanceInfo instance) {
        this.aliveInstances.add(instance);
    }

    public void removeAliveInstance(InstanceInfo instance) {
        this.aliveInstances.remove(instance);
    }

    public Set<InstanceInfo> getSuspectedInstances() {
        return this.suspectedInstances;
    }

    public void addSuspectedInstance(InstanceInfo instance) {
        this.suspectedInstances.add(instance);
    }

    public void removeSuspectedInstance(InstanceInfo instance) {
        this.suspectedInstances.remove(instance);
    }

    public int updateAndGetIdx() {
        return this.idx.updateAndGet(i -> (i + 1) % this.aliveInstances.size());
    }

    /*
     * Create count instances and blocks the execution until they are created
     */
    public List<String> createInstances(int count) {

        LOGGER.log("Creating " + count + " instances");

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
        while (newInstances.stream().filter(i -> i.getState().getName().equals("pending")).count() != 0) {

            List<Reservation> reservations = this.ec2.describeInstances(
                    new DescribeInstancesRequest()
                            .withFilters(new Filter()
                                    .withName("reservation-id")
                                    .withValues(reservationId)))
                    .getReservations();

            if (reservations.size() > 0)
                newInstances = reservations.get(0).getInstances();
            else
                break;

            try {
                Thread.sleep(QUERY_COOLDOWN);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<InstanceInfo> newInstancesInfo = newInstances.stream().map(i -> new InstanceInfo(i))
                .collect(Collectors.toList());

        this.aliveInstances.addAll(newInstancesInfo);

        LOGGER.log("Total instances: " + this.aliveInstances.size());

        return newInstances.stream().map(i -> i.getInstanceId()).collect(Collectors.toList());
    }

    public void terminateInstance(InstanceInfo instance) {
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstance().getInstanceId());
        this.ec2.terminateInstances(termInstanceReq);
    }

    public void terminateInstances(int count) {

        LOGGER.log("Terminating " + count + " instances");

        // find 'count' least loaded instances
        Set<InstanceInfo> instancesToTerminate = this.getAliveInstances().stream().sorted((i1, i2) -> {
            return Double.compare(i1.getLoad(), i2.getLoad());
        }).limit(count).collect(Collectors.toSet());

        // Remove from aliveInstances to prevent new requests
        this.aliveInstances.removeAll(instancesToTerminate);

        // Wait for instances to finish requests and terminate the instances
        while (instancesToTerminate.size() != 0) {
            for (InstanceInfo instance : instancesToTerminate) {
                // Safe to terminate
                if (instance.getRequests().size() == 0) {
                    TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
                    termInstanceReq.withInstanceIds(instance.getInstance().getInstanceId());
                    this.ec2.terminateInstances(termInstanceReq);
                    instancesToTerminate.remove(instance);
                }
            }
        }
    }

    public Set<Instance> queryInstances() {
        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances());
        }
        return instances;
    }

    public Set<InstanceInfo> queryAliveInstances() {

        Set<InstanceInfo> instances = new HashSet<InstanceInfo>();
        for (Reservation reservation : this.ec2.describeInstances().getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getState().getName().equals("running") && instance.getImageId().equals(AMI_ID)) {
                    instances.add(new InstanceInfo(instance));
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

        for (InstanceInfo instance : this.aliveInstances) {
            String iid = instance.getInstance().getInstanceId();

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

    public InvokeResult callLambda(String functionName, String json) {

        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(json);

        try {
            AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
                    .withCredentials(new EnvironmentVariableCredentialsProvider())
                    .withRegion(AWS_REGION)
                    .build();

            return awsLambda.invoke(invokeRequest);

        } catch (ServiceException e) {
            LOGGER.log(e.getMessage());
            return null;
        }
    }

    /*
     * Create DynamoDB table if it does not exists yet
     * Stores statistics about each request
     */
    public void createTableIfNotExists() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(DYNAMO_DB_TABLE_NAME)
                .withAttributeDefinitions(new AttributeDefinition()
                        .withAttributeName("RequestParams")
                        .withAttributeType("S"))
                .withKeySchema(new KeySchemaElement()
                        .withAttributeName("RequestParams")
                        .withKeyType("HASH"))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L))
                .withTableClass("STANDARD");

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        try {
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, DYNAMO_DB_TABLE_NAME);
        } catch (InterruptedException e) {
            LOGGER.log(e.getMessage());
        }
    }

    private void addToCache(Statistics stat) {
        // non-thread safe, should only be called while a mutex is acquired
        if (cacheItems.containsKey(stat.getRequestParams()))
            return;
        while (lruCache.size() >= CACHE_CAPACITY) {
            // we are assuming FIFO
            Statistics item = lruCache.remove();
            cacheItems.remove(item.getRequestParams());
        }
        cacheItems.put(stat.getRequestParams(), stat);
        lruCache.add(stat);
    }

    public Optional<Statistics> getFromCache(Request request) {
        synchronized (this) {
            Statistics item = cacheItems.get(request.getURI());
            return item == null ? Optional.empty() : Optional.of(item);
        }
    }

    /*
     * Creates a new thread to query the database
     * Updates the cache if it find a blue
     */
    public void getFromStatistics(Request request) {

        Thread thread = new Thread() {
            public void run() {
                LOGGER.log("Fetching statistics from database in the background...");

                Map<String, Condition> filter = new HashMap<>();

                Condition endpointCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
                        .withAttributeValueList(new AttributeValue().withS(request.getEndpoint().toString()));

                filter.put("endpoint", endpointCondition);

                switch (request.getEndpoint()) {
                    case SIMULATION:
                        Condition worldCondition = new Condition()
                                .withComparisonOperator(ComparisonOperator.EQ.toString())
                                .withAttributeValueList(
                                        new AttributeValue().withN(request.getArguments().get(1)));
                        filter.put("world", worldCondition);
                        Condition generationCondition = new Condition()
                                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                                .withAttributeValueList(
                                        new AttributeValue().withN(
                                                String.valueOf(Integer.parseInt(request.getArguments().get(0)) - 50)),
                                        new AttributeValue().withN(
                                                String.valueOf(Integer.parseInt(request.getArguments().get(0) + 50))));
                        filter.put("generations", generationCondition);
                        Condition scenarioCondition = new Condition()
                                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                                .withAttributeValueList(
                                        new AttributeValue().withN("1"),
                                        new AttributeValue().withN("4"));
                        filter.put("scenario", scenarioCondition);

                        break;
                    case WAR:
                        break;

                    case COMPRESSION:
                        break;

                    default:
                }

                ScanRequest scanRequest = new ScanRequest()
                        .withTableName(DYNAMO_DB_TABLE_NAME)
                        .withScanFilter(filter);

                ScanResult scanResult = dynamoDB.scan(scanRequest);

                List<Map<String, AttributeValue>> items = scanResult.getItems();
                if (items == null || items.size() == 0) {
                    LOGGER.log("No statistics found for request: " + request.getURI());
                    return;
                }

                synchronized (this) {
                    items.stream().forEach(item -> {
                        // Assuming every field is correct
                        Statistics stat = new Statistics(item.get("RequestParams").getS(),
                                Long.parseLong(item.get("InstCount").getN()),
                                Long.parseLong(item.get("BasicBlockCount").getN()));

                        LOGGER.log("Statistics found for request: " + request.getURI() + " with "
                                + stat.getInstructionCount() + " instructions");

                        addToCache(stat);
                    });
                }

                Optional<Statistics> stat = getFromCache(request);
                if (stat.isPresent()) {
                    request.setEstimatedCost(stat.get().getInstructionCount());
                } else {
                    items.stream().mapToDouble(item -> Double.parseDouble(item.get("InstructionCount").getS()))
                            .average().ifPresent(avg -> {
                                request.setEstimatedCost(avg);
                            });
                }
            }
        };

        thread.start();
    }
}
