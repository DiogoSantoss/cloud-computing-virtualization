#!/usr/bin/env bash

# Delete table if already exists
echo "Deleting any pre-existing table with the same name"
aws dynamodb delete-table \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME 2>> /dev/null

echo "Waiting until table is deleted"
aws dynamodb wait table-not-exists \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME

echo "Creating the table"
aws dynamodb create-table \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME \
  --attribute-definition \
    AttributeName=RequestParams,AttributeType=S \
  --key-schema \
    AttributeName=RequestParams,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=1,WriteCapacityUnits=1 \
  --table-class STANDARD 2>&1 | jq .

echo "Waiting until table is available"
aws dynamodb wait table-exists \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME

# echo "Testing Writing to the Table"
# aws dynamodb put-item \
#     --region $AWS_DEFAULT_REGION \
#     --table-name $DYNAMO_DB_TABLE_NAME  \
#     --item \
#         '{"RequestParams": {"S": "random_request"}, "BasicBlockCount": {"N": "10"}, "InstCount": {"N": "11"}}'
# 
# echo "Testing Reading from the Table"
# aws dynamodb get-item \
#   --region $AWS_DEFAULT_REGION \
#   --table-name $DYNAMO_DB_TABLE_NAME \
#   --key '{"RequestParams": {"S": "random_request"}}'

