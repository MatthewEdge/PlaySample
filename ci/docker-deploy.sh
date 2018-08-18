#!/usr/bin/env bash

# Builds, tags, and uploads Docker Images to ECR. Must be run local to the Dockerfile being built.
# Uses Build-Time Arguments to bake in environment configuration to the Image before deployment
#
# If the --service-name and --cluster-name fields are passed in then this script will force a
# re-deployment of the service. This is achieved by using the `latest` tag which is what the Task
# Definition is currently set to.
#
# Author: Matthew Edge - Levvel, LLC

set -exo pipefail

printUsage() {
	echo "$0"
	echo " "
	echo "options:"
	echo "-h, --help        Show brief help"
	echo "--account-id      AWS Account ID of the running user"
	echo "--region          AWS Region resources reside in"
	echo " "
	echo "--service-name    Optional: Name of the ECS Service to redeploy. Must be used with --cluster-name"
	echo "--cluster-name    Optional: Name of the ECS Cluster. Must be used with --service-name"
	echo "--instance-count  Number of instances to deploy. Defaults to 1 if not provided"
	echo " "
	echo "--env             Environment being deployed to (value from CloudFormation template)"
	echo "--version         App Version being deployed (for debug)"
	echo "--branch          App Branch being deployed"
	echo "--git-hash        Git Short Hash being built. Tagged on the image for tracking purposes"
	echo "--repo            ECR Repository name to upload Docker image to"
	echo "--tag             Additional custom tag to add to Docker image (along with APP_VERSION and GIT_HASH)"
}

# errIfMissing "$VAR", "ERROR_MESSAGE"
errIfMissing() {
	if [ -z "$1" ]; then
		echo "$2"
		exit 1
	fi
}

# Parse flags
while test $# -gt 0; do
	case "$1" in
	-h | --help)
		printUsage
		exit 0
		;;
	--account-id)
		shift
		export AWS_ACCOUNT_ID=$(echo $1)
		shift
		;;
	--repo)
		shift
		export REPO_NAME=$(echo $1)
		shift
		;;
	--instance-count)
		shift
		export INSTANCE_COUNT=$(echo $1)
		shift
		;;
	--service-name)
		shift
		export SERVICE_NAME=$(echo $1)
		shift
		;;
	--cluster-name)
		shift
		export CLUSTER_NAME=$(echo $1)
		shift
		;;
	--tag)
		shift
		export IMAGE_TAG=$(echo $1)
		shift
		;;
	--region)
		shift
		export AWS_REGION=$(echo $1)
		shift
		;;
	--env)
		shift
		export CFN_ENV=$(echo $1)
		shift
		;;
	--version)
		shift
		export APP_VERSION=$(echo $1)
		shift
		;;
	--git-hash)
		shift
		export GIT_HASH=$(echo $1)
		shift
		;;
	*)
		echo "Invalid option $1"
		printUsage
		exit 1
		;;
	esac
done

# Validate required params
errIfMissing "${AWS_ACCOUNT_ID}" "AWS Account ID (--account-id) is null/empty"
errIfMissing "${AWS_REGION}" "AWS Region (--region) is null/empty"
errIfMissing "${REPO_NAME}" "ECR Repository (--repo) is null/empty"
errIfMissing "${CFN_ENV}" "Environment (--env) is null/empty"

# ECR Upload
echo "ECR login"
$(aws ecr get-login --no-include-email --region ${AWS_REGION})

# Checkout the given Git Tag if available
if [ ! -z "${GIT_TAG}" ]; then
	git checkout tags/${GIT_TAG}
fi

echo "Building image for ${GIT_TAG}"
docker build -t ${REPO_NAME}:build .

echo "Publishing tags [${APP_VERSION} | ${GIT_HASH} | ${CFN_ENV} | latest] to ECR"

if [ ! -z "${IMAGE_TAG}" ]; then
	echo "Adding [${IMAGE_TAG}] tag to image"
	docker tag ${REPO_NAME}:build ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${IMAGE_TAG}
	docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${IMAGE_TAG}
fi

# Tack on APP_VERSION, GIT_HASH, and CFN_ENV as tags for tracking purposes
docker tag ${REPO_NAME}:build ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${APP_VERSION}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${APP_VERSION}

docker tag ${REPO_NAME}:build ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${GIT_HASH}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${GIT_HASH}

docker tag ${REPO_NAME}:build ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${CFN_ENV}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:${CFN_ENV}

# Finally - tack latest
docker tag ${REPO_NAME}:build ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:latest
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}:latest

# If a SERVICE_NAME and CLUSTER_NAME are set - service redeploy
# Note: Requires IAM Policy ecs:UpdateService
if [ ! -z "${SERVICE_NAME}" ] && [ ! -z "${CLUSTER_NAME}" ]; then
	echo "Re-deploying service ${SERVICE_NAME} on cluster ${CLUSTER_NAME}"

	# Force desired-count to match to account for new CloudFormation deployment which defaults count to 0
	COUNT=${INSTANCE_COUNT:-1}
	aws ecs update-service --cluster ${CLUSTER_NAME} --service ${SERVICE_NAME} --force-new-deployment --desired-count ${COUNT}
else
	echo "No SERVICE_NAME and/or CLUSTER_NAME set. Skipping Service Redeployment"
	echo "SERVICE_NAME: ${SERVICE_NAME} | CLUSTER_NAME: ${CLUSTER_NAME}"
	exit 0
fi
