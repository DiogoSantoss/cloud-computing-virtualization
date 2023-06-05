package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class DynamoDownloader {

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");

    private final String DYNAMO_DB_TABLE_NAME = System.getenv("DYNAMO_DB_TABLE_NAME");

    private final AmazonDynamoDB dynamoDB;

    private final int CACHE_CAPACITY = 512;
    private final Queue<Statistics> lruCache = new ConcurrentLinkedQueue<>();

    private final Map<String, Statistics> cacheItems = new ConcurrentHashMap<>();

    public DynamoDownloader() {
        if (AWS_REGION == null || DYNAMO_DB_TABLE_NAME == null)
            throw new RuntimeException("AWS_REGION or DYNAMO_DB_TABLE_NAME not set");
        this.dynamoDB = AmazonDynamoDBAsyncClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        this.createTable();
    }

    private void addToCache(Statistics stat) {
        // non-thread safe, should only be called while a mutex is acquired
        if (cacheItems.containsKey(stat.getRequestParams())) return;
        while (lruCache.size() >= CACHE_CAPACITY) {
            // we are assuming FIFO
            Statistics item = lruCache.remove();
            cacheItems.remove(item.getRequestParams());
        }
        cacheItems.put(stat.getRequestParams(), stat);
        lruCache.add(stat);
    }

    public void createTable() {
        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(DYNAMO_DB_TABLE_NAME).withAttributeDefinitions(new AttributeDefinition().withAttributeName("RequestParams").withAttributeType("S")).withKeySchema(new KeySchemaElement().withAttributeName("RequestParams").withKeyType("HASH")).withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L)).withTableClass("STANDARD");

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);

        try {
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, DYNAMO_DB_TABLE_NAME);
        } catch (InterruptedException e) {
            LOGGER.info(e.getMessage());
        }
    }

    public Optional<Statistics> getFromCache(String request) {
        synchronized (this) {
            Statistics item = cacheItems.get(request);
            return item == null ? Optional.empty() : Optional.of(item);
        }
    }

    public Optional<Statistics> getFromStatistics(String request) {
        Map<String, AttributeValue> query = new HashMap<>();
        query.put("RequestParams", new AttributeValue().withS(request));
        GetItemResult queryResult = dynamoDB.getItem(new GetItemRequest().withTableName(DYNAMO_DB_TABLE_NAME).withKey(query));

        Map<String, AttributeValue> item = queryResult.getItem();
        if (queryResult.getItem() == null) {
            LOGGER.info("queryResult.getItem() is null");
            return Optional.empty();
        } else if (queryResult.getItem().size() == 0) {
            LOGGER.info("queryResult.getItem().size() is 0");
            return Optional.empty();
        }

        synchronized (this) {
            // Assuming every field is correct
            Statistics stat = new Statistics(item.get("RequestParams").getS(), Long.parseLong(item.get("InstCount").getN()), Long.parseLong(item.get("BasicBlockCount").getN()));
            addToCache(stat);
            return Optional.of(stat);
        }

    }
}

