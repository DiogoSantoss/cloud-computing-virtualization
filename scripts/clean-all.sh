#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh

./clean-monitoring.sh
./clean-worker.sh
./lambda-deregister.sh
./delete-db.sh

