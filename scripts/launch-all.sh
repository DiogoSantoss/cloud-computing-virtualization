#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh

./create-worker-image.sh
./lambda-register.sh
./create-db.sh
./launch-monitoring.sh
