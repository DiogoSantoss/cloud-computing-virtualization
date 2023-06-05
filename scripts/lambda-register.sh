#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# TODO: acho que podemos só martelar isto com role de root

aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

sleep 5

aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

aws lambda create-function \
	--function-name worker-lambda \
	--zip-file fileb://../target/lab-faas-1.0-SNAPSHOT-jar-with-dependencies.jar \ ###### CHANGE THIS
	--handler pt.ulisboa.tecnico.cnv.faas.Handler \  ###### CHANGE THIS
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role
