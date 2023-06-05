#!/bin/sh

echo "Go to https://grupos.ist.utl.pt/meic-cnv/project/index-22-23.html to send requests"
java -cp webserver/build/libs/webserver.jar -javaagent:webserver/build/libs/webserver.jar=Metrics:pt.ulisboa.tecnico.cnv:output pt.ulisboa.tecnico.cnv.webserver.WebServer

