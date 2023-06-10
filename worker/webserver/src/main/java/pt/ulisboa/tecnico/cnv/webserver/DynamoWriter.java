package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.partitions.model.Region;
import com.amazonaws.regions.Regions;
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
        for(;;) {
            try {
                LOGGER.info("Starting 10 second wait");
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            synchronized(Metrics.dynamoQueue) {
                workQueue.addAll(Metrics.dynamoQueue);
                Metrics.dynamoQueue.clear();
            }

            // TODO: Only submit metrics in batches of 25
            
            LOGGER.info("Running scheduled DynamoDB write");
            if (workQueue.isEmpty()) {
                LOGGER.info("No metrics to write");
                continue;
            }

            Optional<List<PutItemRequest>> requests = Optional.of(this.workQueue.stream().map(m -> {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("RequestParams", new AttributeValue().withS(m.getLeft()));
                item.put("InstructionCount", new AttributeValue().withN(Long.toString(m.getRight().getInstCount())));
                item.put("BasicBlockCount", new AttributeValue().withN(Long.toString(m.getRight().getBasicBlockCount())));
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