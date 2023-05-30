package pt.ulisboa.tecnico.cnv.webserver;

import pt.ulisboa.tecnico.cnv.javassist.tools.Metrics;

import java.util.*;
import java.util.logging.Logger;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;


public class Uploader implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Uploader.class.getName());

    private static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    private static String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");
 
    private AmazonDynamoDB dynamoDB;

    public Uploader() {
        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    @Override
    public void run() {
        this.createTableIfNotExists();
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.consumeMetricsQueue();
        }
    }

    /*
     * Create DynamoDB table if it does not exists yet
     * Stores statistics about each request
     */
    public void createTableIfNotExists() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(DYNAMO_DB_TABLE_NAME)
                .withKeySchema(new KeySchemaElement().withAttributeName("request").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName("request").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(
                        new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        try {
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, DYNAMO_DB_TABLE_NAME);
        } catch(InterruptedException e) {
            LOGGER.info(e.getMessage());
        }
    }

    /*
     * Consume metrics from javassist to DynamoDB
     */
    public void consumeMetricsQueue() {
        while(!Metrics.getQueue().isEmpty()) {
            Metrics.Pair<String, Metrics.Statistics> pair = Metrics.getQueue().poll();
            
            if (pair == null) break; // Sanity check

            String request = pair.getLeft();
            Metrics.Statistics statistics = pair.getRight();
            this.uploadStatistics(request, statistics.getInstCount());
        }
    }

    /*
     * Add item to DynamoDB
     */
    public void uploadStatistics(String request, long instructionCount) {
        // Create item
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("request", new AttributeValue(request));
        item.put("instructionCount", new AttributeValue().withN(Long.toString(instructionCount)));

        // Upload item
        dynamoDB.putItem(new PutItemRequest(DYNAMO_DB_TABLE_NAME, item));
    }

    /*
     * Get statistics about a request from DynamoDB
     */
    public long getStatistics(String request) {
        // Create key
        Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
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
