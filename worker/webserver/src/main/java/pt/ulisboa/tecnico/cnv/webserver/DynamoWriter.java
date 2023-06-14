package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import pt.ulisboa.tecnico.cnv.javassist.tools.Metrics;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;

public class DynamoWriter implements Runnable {

    private final Logger LOGGER = Logger.getLogger(DynamoWriter.class.getName());

    private final String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    private final String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");

    private final AmazonDynamoDB dynamoDB;

    private final List<Metrics.Pair<String, Metrics.Statistics>> workQueue = new ArrayList<>();

    public DynamoWriter() {
        if (AWS_REGION == null || DYNAMO_DB_TABLE_NAME == null)
            throw new RuntimeException("AWS_REGION or DYNAMO_DB_TABLE_NAME not set");

        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        this.createTable();
    }

    @Override
    public void run() {
        for (;;) {
            try {
                LOGGER.info("Starting 10 second wait");
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (Metrics.dynamoQueue) {
                // Read first 25 elements from queue
                workQueue.addAll(Metrics.dynamoQueue.subList(0, Math.min(25, Metrics.dynamoQueue.size())));
                Metrics.dynamoQueue = Metrics.dynamoQueue.subList(Math.min(25, Metrics.dynamoQueue.size()),
                        Metrics.dynamoQueue.size());
            }

            LOGGER.info("Running scheduled DynamoDB write");
            if (workQueue.isEmpty()) {
                LOGGER.info("No metrics to write");
                continue;
            }

            Optional<List<PutItemRequest>> requests = Optional.of(this.workQueue.stream().map(m -> {

                String requestParameters = m.getLeft();
                String firstSplit[] = requestParameters.split("\\?");
                String secondSplit[] = firstSplit[1].split("&");

                String endpoint = firstSplit[0].split("/")[1];
                List<String> parameters = Arrays.stream(secondSplit).map(s -> s.split("=")[1])
                        .collect(Collectors.toList());

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("RequestParams", new AttributeValue().withS(m.getLeft()));
                item.put("InstructionCount", new AttributeValue().withN(Long.toString(m.getRight().getInstCount())));
                item.put("BasicBlockCount",
                        new AttributeValue().withN(Long.toString(m.getRight().getBasicBlockCount())));

                item.put("endpoint", new AttributeValue().withS(endpoint));
                switch (endpoint) {
                    case "simulate":
                        item.put("generations", new AttributeValue().withS(parameters.get(0)));
                        item.put("world", new AttributeValue().withS(parameters.get(1)));
                        item.put("scenario", new AttributeValue().withS(parameters.get(2)));
                        break;
                    case "insectwar":
                        item.put("max", new AttributeValue().withS(parameters.get(0)));
                        item.put("army1", new AttributeValue().withS(parameters.get(1)));
                        item.put("army2", new AttributeValue().withS(parameters.get(2)));
                        break;
                    case "compressimage":
                        item.put("resolution", new AttributeValue().withS(parameters.get(0)));
                        item.put("targetFormat", new AttributeValue().withS(parameters.get(1)));
                        item.put("compressionFactor", new AttributeValue().withS(parameters.get(2)));
                        break;
                }

                return new PutItemRequest().withItem(item).withTableName(DYNAMO_DB_TABLE_NAME);
            }).collect(Collectors.toList()));

            workQueue.clear();

            requests.ifPresent(w -> {
                w.forEach(r -> {
                    LOGGER.info("Writing to dynamo");
                    dynamoDB.putItem(r);
                    LOGGER.info("Wrote to dynamo");
                });
            });

            LOGGER.info("Exited dynamo write");
        }
    }

    /*
     * Create DynamoDB table if it does not exists yet
     * Stores statistics about each request
     * 
     */
    public void createTable() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(DYNAMO_DB_TABLE_NAME)
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName("RequestParams").withAttributeType("S"))
                .withKeySchema(new KeySchemaElement().withAttributeName("RequestParams").withKeyType("HASH"))
                .withProvisionedThroughput(
                        new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))
                .withTableClass("STANDARD");

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        try {
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, DYNAMO_DB_TABLE_NAME);
        } catch (InterruptedException e) {
            LOGGER.info(e.getMessage());
        }
    }

    /*
     * Get statistics about a request from DynamoDB
     */
    public long getStatistics(String request) {
        // Create key
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("request", new AttributeValue(request));

        // Get item
        GetItemResult item = dynamoDB.getItem(new GetItemRequest(DYNAMO_DB_TABLE_NAME, key));

        // Return instruction count
        return Long.parseLong(item.getItem().get("instructionCount").getN());
    }

    /*
     * Print all statistics stored in DynamoDB
     */
    public void printStatistics() {
        ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMO_DB_TABLE_NAME);
        ScanResult result = dynamoDB.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()) {
            System.out.println(item);
        }
    }
}
