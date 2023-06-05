#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# TODO: acho que podemos só martelar isto com role de root

aws lambda delete-function --function-name worker-lambda

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role --role-name lambda-role
