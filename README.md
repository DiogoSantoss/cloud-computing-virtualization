# EcoWorld@Cloud

Set of services (Compression, Foxes-Rabbits and Insect-War) hosted on AWS (EC2 and Lambda) with self designed Load Balancer and Auto-Scaling algorithms that rely on the instruction count of each request measured by Javassist to ensure low expenditure and high availability.
Further details can be found in the [report](/report/report.pdf), where we explain the Architecture, Instrumentation Metrics, Data Structures, Request Cost Estimation, Scheduling Algorithm, Auto-Scaling Algorithm and Fault-Tolerance in great detail.

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