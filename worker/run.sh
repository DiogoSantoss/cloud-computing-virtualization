#!/bin/sh

echo "Go to https://grupos.ist.utl.pt/meic-cnv/project/index-22-23.html to send requests"
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar=Metrics:pt.ulisboa.tecnico.cnv:output pt.ulisboa.tecnico.cnv.webserver.WebServer
