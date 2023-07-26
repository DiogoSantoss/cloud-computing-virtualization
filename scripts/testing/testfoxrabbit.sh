#!/usr/bin/env bash

# Syntax:  ./testfoxrabbit.sh <ip> <port> <generations> <world> <scenario>
# Example: ./testfoxrabbit.sh 127.0.0.1 8000 1000 4 2
HOST=$1
PORT=$2
GENERATIONS=$3
WORLD=$4
SCENARIO=$5
TIMEOUT=30 #default is 30s

function test_batch_requests {
	REQUESTS=100
	CONNECTIONS=1
	ab -s $TIMEOUT -n $REQUESTS -c $CONNECTIONS $HOST:$PORT/simulate\?generations=$GENERATIONS\&world=$WORLD\&scenario=$SCENARIO
}

function test_single_requests {

	curl $HOST:$PORT/simulate\?generations=$GENERATIONS\&world=$WORLD\&scenario=$SCENARIO
}

test_single_requests
test_batch_requests
