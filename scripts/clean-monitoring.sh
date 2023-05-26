#!/bin/bash

source my_config.sh

# Terminate worker instance
aws ec2 terminate-instances --instance-ids $(cat monitoring.id)