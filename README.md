# EcoWorld@Cloud

## Architecture

### Monitoring
Contains the load balancer which will receive requests from the public internet and forward them to the worker.
Contains the auto-scaler which will scale up or down the number of workers based on the load.

### Worker
Contains the webserver which will process requests proxied by the load balancer.

### Scripts
Contains scripts for deploying and testing the system.

## Environment
Create `my_config.sh` from `config.sh` and populate with AWS credentials.

## Development
```bash
cd worker
mvn clean install
./run.sh

cd monitoring/webserver
./gradlew build
./run.sh
```

## Deployment
```bash
cd worker
./gradlew build

cd monitoring/webserver
./gradlew build

cd scripts
./launch-all.sh
```