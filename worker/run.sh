#!/bin/sh

echo "Go to https://grupos.ist.utl.pt/meic-cnv/project/index-22-23.html to send requests"
java \
  -cp webserver/build/libs/webserver.jar \
  -Xbootclasspath/a://javassist/build/libs/javassist.jar \
  -javaagent:webserver/build/libs/webserver.jar=Metrics:pt.ulisboa.tecnico.cnv,javax.imageio:output \
  pt.ulisboa.tecnico.cnv.webserver.WebServer

