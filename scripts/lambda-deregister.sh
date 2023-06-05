#!/bin/bash

source my_config.sh

aws lambda delete-function --function-name worker-lambda

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
