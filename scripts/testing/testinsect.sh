#!/bin/bash

# Syntax:  ./testinsect.sh <ip> <port> <max> <army1> <army2>
# Example: ./testinsect.sh 127.0.0.1 8000 1000 10 10
HOST=$1
PORT=$2
max=$3
army1=$4
army2=$5

function test_batch_requests {
	REQUESTS=3
	CONNECTIONS=1
	ab -n $REQUESTS -c $CONNECTIONS $HOST:$PORT/insectwar\?max=$max\&army1=$army1\&army2=$army2
}

function test_single_requests {

	curl $HOST:$PORT/insectwar\?max=$max\&army1=$army1\&army2=$army2
}

test_single_requests
test_batch_requests