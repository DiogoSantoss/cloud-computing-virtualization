#!/usr/bin/env bash

# only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh

echo "Creating monitoring instance..."

# Run new instance
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > monitoring.id
echo "New instance with id $(cat monitoring.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat monitoring.id)
echo "New instance with id $(cat monitoring.id) is now running."

# Extract DNS nane.
aws ec2 describe-instances \
	--instance-ids $(cat monitoring.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > monitoring.dns
echo "New instance with id $(cat monitoring.id) has address $(cat monitoring.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat monitoring.dns) 22; do
	echo "Waiting for $(cat monitoring.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat monitoring.id) is ready for SSH access."

# Step 2: install software in the VM instance.

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat monitoring.dns) $cmd

# Copy webserver jar
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../monitoring/webserver/build/libs/webserver.jar ec2-user@$(cat monitoring.dns):

# Copy config file
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../scripts/my_config.sh ec2-user@$(cat monitoring.dns):

# Copy the image id
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../scripts/image.id ec2-user@$(cat monitoring.dns):

# Start webserver
cmd="source my_config.sh && java -cp /home/ec2-user/webserver.jar pt.ulisboa.tecnico.cnv.webserver.WebServer"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat monitoring.dns) $cmd &
