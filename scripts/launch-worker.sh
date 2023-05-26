#!/bin/bash

source my_config.sh

echo "Creating worker instance..."

# Run new instance
aws ec2 run-instances \
	--image-id $(cat image.id) \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > instance.id

echo "New instance with id $(cat instance.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat instance.id)
echo "New instance with id $(cat instance.id) is now running."

# Extract DNS nane.
aws ec2 describe-instances \
	--instance-ids $(cat instance.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > instance.dns
echo "New instance with id $(cat instance.id) has address $(cat instance.dns)."

