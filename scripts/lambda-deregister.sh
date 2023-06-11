#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh

aws lambda delete-function --function-name insectwar-lambda
aws lambda delete-function --function-name simulate-lambda
aws lambda delete-function --function-name compressimage-lambda

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
