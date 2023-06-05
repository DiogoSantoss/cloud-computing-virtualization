#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[[ $(hostname) -ne "chord" ]] && source my_config.sh

# Deregister worker image
aws ec2 deregister-image --image-id $(cat image.id)

# Terminate worker instance
aws ec2 terminate-instances --instance-ids $(cat instance.id)
