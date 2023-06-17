#!/bin/sh

echo "Removing previous estimates"
rm estimates.csv 2> /dev/null
echo "Running monitoring webserver..."
java -cp webserver/build/libs/webserver.jar \
         pt.ulisboa.tecnico.cnv.webserver.WebServer
