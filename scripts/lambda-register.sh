#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh


aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'

sleep 5

aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

aws lambda create-function \
	--function-name insectwar-lambda \
	--zip-file fileb://../worker/insect-war/build/libs/insect-war.jar \
	--handler pt.ulisboa.tecnico.cnv.insectwar.WarSimulationHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role >> /dev/null

sleep 5

aws lambda create-function \
	--function-name simulate-lambda \
	--zip-file fileb://../worker/foxes-rabbits/build/libs/foxes-rabbits.jar \
	--handler pt.ulisboa.tecnico.cnv.foxesrabbits.SimulationHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role >> /dev/null

sleep 5

aws lambda create-function \
	--function-name compressimage-lambda \
	--zip-file fileb://../worker/compression/build/libs/compression.jar \
	--handler pt.ulisboa.tecnico.cnv.compression.BaseCompressingHandler \
	--runtime java11 \
	--timeout 5 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role >> /dev/null