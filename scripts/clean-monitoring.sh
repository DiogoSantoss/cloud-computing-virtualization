#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# Terminate worker instance
aws ec2 terminate-instances --instance-ids $(cat monitoring.id)
