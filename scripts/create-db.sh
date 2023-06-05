#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# Delete table if already exists
./delete-db.sh

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

