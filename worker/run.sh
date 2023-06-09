#!/bin/sh

echo "Accepting requests at http://localhost:8000"
echo "Go to https://grupos.ist.utl.pt/meic-cnv/project/index-22-23.html to send requests"

java -cp webserver/build/libs/webserver.jar \
  -Xbootclasspath/a:javassist/build/libs/javassist.jar \
  -javaagent:javassist/build/libs/javassist.jar=Metrics:pt.ulisboa.tecnico.cnv,javax.imageio:output \
  pt.ulisboa.tecnico.cnv.webserver.WebServer

# java -cp webserver/build/libs/webserver.jar -javaagent:webserver/build/libs/webserver.jar=Metrics:pt.ulisboa.tecnico.cnv:output pt.ulisboa.tecnico.cnv.webserver.WebServer

# java \
#   -cp webserver/build/libs/webserver.jar \
#   -Xbootclasspath/a://javassist/build/libs/javassist.jar \
#   -javaagent:webserver/build/libs/webserver.jar=Metrics:pt.ulisboa.tecnico.cnv,javax.imageio:output \
#   pt.ulisboa.tecnico.cnv.webserver.WebServer

#java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xbootclasspath/a://javassist/target/JavassistWrapper-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:javassist/target/JavassistWrapper-1.0.0-SNAPSHOT-jar-with-dependencies.jar=Metrics:pt.ulisboa.tecnico.cnv,javax.imageio:output pt.ulisboa.tecnico.cnv.webserver.WebServer
