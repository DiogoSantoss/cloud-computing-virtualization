#!/usr/bin/env bash

# Only source script if hostname is not chord (vasco)
[ "$(hostname)" != "chord" ] && source my_config.sh
echo "Creating Worker Image..."

# Step 1: launch a vm instance.

# Run new instance
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
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

# Wait for instance to have SSH ready.
while ! nc -z $(cat instance.dns) 22; do
	echo "Waiting for $(cat instance.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat instance.id) is ready for SSH access."

# Step 2: install software in the VM instance.

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Copy webserver jar
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../worker/webserver/build/libs/webserver.jar ec2-user@$(cat instance.dns):

# Copy javassist jar
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ../worker/javassist/build/libs/javassist.jar ec2-user@$(cat instance.dns):

# Setup web server to start on instance launch.
cmd="echo \"java -cp /home/ec2-user/webserver.jar -Xbootclasspath/a:/home/ec2-user/javassist.jar -javaagent:/home/ec2-user/javassist.jar=Metrics:pt.ulisboa.tecnico.cnv,javax.imageio:output pt.ulisboa.tecnico.cnv.webserver.WebServer\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Step 3: test VM instance.

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance.id)
echo "Rebooting instance to test web server auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8000 to become available.
while ! nc -z $(cat instance.dns) 8000; do
	echo "Waiting for $(cat instance.dns):8000..."
	sleep 0.5
done

# Sending a query!
echo "Sending a simulation query!"
curl $(cat instance.dns):8000/simulate\?generations=1\&world=1\&scenario=1

# Step 4: create VM image (AIM).
aws ec2 create-image --instance-id $(cat instance.id) --name Worker-Image | jq -r .ImageId > image.id

# Step 5: Wait for image to become available.
echo "Waiting for image to be ready... (this can take a couple of minutes)"
aws ec2 wait image-available --filters Name=name,Values=Worker-Image

# Step 6: terminate the vm instance.
aws ec2 terminate-instances --instance-ids $(cat instance.id)
