#!/usr/bin/env bash

# only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# Delete table if already exists
echo "Deleting any pre-existing table with the same name"
aws dynamodb delete-table \
  --region $AWS_DEFAULT_REGION \
  --table-name $DYNAMO_DB_TABLE_NAME 2>> /dev/null

echo "Waiting until table is deleted"
aws dynamodb wait table-not-exists \
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

