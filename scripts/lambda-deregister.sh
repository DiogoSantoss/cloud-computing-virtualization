#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
#[ "$(hostname)" != "chord" ] && source my_config.sh

source my_config.sh

# TODO: acho que podemos só martelar isto com role de root

aws lambda delete-function --function-name insect-war-lambda
aws lambda delete-function --function-name foxes-rabbits-lambda
aws lambda delete-function --function-name compression-lambda

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
