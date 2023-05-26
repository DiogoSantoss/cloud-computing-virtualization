/*
package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.Region;

public class Uploader {

    // TODO: should be from my_config.sh (aka env var)
    private static String AWS_REGION = Region.US_EAST_1;
    // TODO
    private static String DYNAMO_DB_TABLE_NAME = "";

    private DynamoDB dynamoDB;

    private void initDynamoDBCliente() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(AWS_REGION));
        this.dynamoDB = new DynamoDB(client);
    }
}
*/