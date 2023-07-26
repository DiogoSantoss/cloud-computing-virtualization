#!/usr/bin/env bash

# Syntax:  ./testcompress.sh <ip> <port> <input image>
# Example: ./testcompress.sh 127.0.0.1 8000 res/Ct0zUz-XgAAV69z.jpg
HOST=$1
PORT=$2
INPUT=$3

TARGET_FORMAT="jpeg"
COMPRESSION_FACTOR="0.1"

function test_batch_requests {
	REQUESTS=3
	CONNECTIONS=1
	echo "targetFormat:$TARGET_FORMAT;compressionFactor:$COMPRESSION_FACTOR;data:image/jpeg;base64,$(base64 --wrap=0 $INPUT)" > /tmp/image.body
	ab -n $REQUESTS -c $CONNECTIONS -p /tmp/image.body "$HOST:$PORT/compressimage"
}

function test_single_requests {
	BODY=$(base64 --wrap=0 $INPUT)
	BODY=$(echo "targetFormat:$TARGET_FORMAT;compressionFactor:$COMPRESSION_FACTOR;data:image/jpeg;base64,$BODY")

	curl -s -d $BODY $HOST:$PORT/compressimage -o /tmp/$TARGET_FORMAT-image.dat

	OUTPUT=$(cat /tmp/$TARGET_FORMAT-image.dat)   # read raw output
	OUTPUT=${OUTPUT#*,}                           # parse output after comma
	echo $OUTPUT > /tmp/$TARGET_FORMAT-image.dat  # write pure Base64 output to a file
	cat /tmp/$TARGET_FORMAT-image.dat | base64 -d > /tmp/$TARGET_FORMAT-image.$TARGET_FORMAT
}

test_single_requests
test_batch_requests
