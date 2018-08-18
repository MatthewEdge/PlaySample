# CI/CD Pipeline

Current implementation consists of a set of `AWS CodeBuild` jobs which builds up a `Docker` image from the compiled code. This
image is then deployed to an `AWS ECR` container repository. The `AWS ECS` service which deploys the Docker Image is 
then updated to force deployment of the new image.

The deployment steps themselves are found in the `docker-deploy.sh` file and the associated `buildspec.yml` files. 
Inputs are fed in by CodeBuild at build time.

CloudFormation templates are provided for all CodeBuild jobs and most ECS infrastructure (other than pre-created infra like VPCs).

## Building Infrastructure

**Tl;DR:** Create ECR Repo -> Create S3 Bucket for Swagger -> Run `af-services-codebuild.yml` CFT -> Run CodeBuild Job for
desired environment -> Environment will be created fresh.

### Building new environments

1) Create the prerequisite ECR Repository (`audience-finder`) and S3 Bucket (`audience-finder-${CFN_ENV}-swagger`)
2) Run the `af-services-codebuild.yml` CFT (from CLI or console) which creates the CodeBuild jobs for the project.
3) Run the CodeBuild job with the desired starting Git Hash / Git Tag. This will build and deploy the Docker image, package and upload the `conf/swagger.yml` file, and _then_ run the `af-ecs-infra.yml` CloudFormation template. The first time this is run it will take quite a while as some infrastructure items take a while to build. 

After these steps are run - if applicable - follow the steps in the `## CodeBuild` section of this document.

From this point forward any subsequent builds should be much faster as CloudFormation does not require all resources to be 
rebuilt every run. 

**Note:** The build process includes a `aws cloudformation package` step to upload the `conf/swagger.yml` file to S3. 
`aws cloudformation package` uploads local artifacts (swagger.yml in our case) to S3 automatically which is required to 
create the API Gateway resources as they read from this S3 bucket to get the Swagger file. From the template:

```
ApiGAPI:
    Type: AWS::ApiGateway::RestApi
    DependsOn: NetworkLoadBalancer
    Properties:
        Name: audience-finder
        ApiKeySourceType: HEADER
        Description: Audience Finder API Gateway
        FailOnWarnings: True
        EndpointConfiguration:
            Types:
            - EDGE
        BodyS3Location: ../swagger.yml
```

The `BodyS3Location: ../swagger.yml` line will be transformed by the `aws cloudformation package` command to an S3 location after upload.

## Standard Deployment

A standard deployment runs through a couple steps as defined in the `buildspec-build.yml`:

1) Syncronize the `swagger.yml` file to S3 as mentioned earlier. This file is used to build the API Gateway definition
2) Build the Docker image for the application
3) Upload the Docker image created in the previous step to ECR
4) Run the `af-ecs-infra.yml` CloudFormation stack. After initial run this is mostly to synchronize API Gateway and the ECS Service resources as required.
5) Restart the ECS Service to force-deploy the new Docker image

## CodeBuild

CodeBuild definitions are stored in the `af-services-codebuild.yml` CloudFormation template. Unless parameters to the jobs 
changes you need only run this CFT once.

After the CloudFormation template completes there are a couple additional "do once" manual steps.

First - for each CodeBuild job created:
    - Navigate to the CodeBuild builds page
    - Click `Edit Project`
    - If applicable for the job: Check the `Rebuild every time a code change is pushed to this repository` checkbox. See below for WebHook rules
    - Scroll down and click the `Update Project` button

While seemingly redundant - AWS wants to create an IAM `Managed Policy` for CodeBuild jobs that gives CodeBuild access to logs and S3 for 
output artifacts. It is currently not possible to automate this through CFN as AWS created the policy regardless. This prevents a class of errors
when trying to run the job.

The following rules are currently used for WebHooks:
    - For `AudienceFinder-Services-dev` the `Branch Filter` should be `dev`
    - For `AudienceFinder-Services-qa` there should be no WebHook. This environment is manually promoted when the `qa` Git tag is created / moved
    - For `AudienceFinder-Services-uat` the `Branch Filter` should be `master`
    - For `AudienceFinder-Services-prod` there should be no WebHook as this is a manual promotion job only (at this time)

Each CodeBuild job has a couple environment variables that is proxied to the `docker-deploy.sh` script:
    - `CFN_ENV` - contains the CloudFormation environment value (see the EnvironmentName parameter for valid values)
    - `REPO_NAME` - contains the name of the created prerequisite ECR repository
    - `SERVICE_NAME` - contains the name of the ECS Service to deploy to
    - `INSTANCE_COUNT` - contains the number of ECS instances to run for the given environment
    - `CLUSTER_NAME` - contains the name of the ECS Cluster the services are deployed to
    - `GIT_TAG` - A specific Git Tag to check out before building. This is used for the `qa`, `uat`, and `prod` jobs

An optional `IMAGE_TAG` parameter may also be specified manually if you wish to append a custom tag to the Docker Images.
Currently the `Git Short Hash`, `Version from build.sbt`, and `CFN_ENV` values are appended to the image

### Build Overrides (DEV Job Mostly)

CodeBuild allows you to override the Git Commit that is checked out before running. 

1. Log in to CodeBuild and select the `AudienceFinder-Services-ENV` job from the `Build Projects` screen (where ENV is the environment you're deploying to)
2. Click the `Start Build` button
3. In the `Source Version` text box provide the Git Commit Hash of the desired commit to deploy
    - This can be any commit from any branch so be mindful here
4. The job will automatically tag the commit with the branch it's associated to and the `latest` tag. If a `SERVICE_NAME` and
`CLUSTER_NAME` variable are set on the build it will force redeploy the ECS Service.

### docker-deploy.sh

A shell script which contains the build, tag, and deployment steps for Docker -> ECR. Keeps the CodeBuild YAML files 
cleaner and allows for non-CodeBuild steps to be run easier. Accepts arguments as build flags. Run the script with
`-h` or `--help` to see the usage details.

### Buildspec YAML Files

CodeBuild step definition files. These define what steps are actually run in the build. Currently there are three variants:

1. `buildspec-test.yml` - Runs `sbt test` only which is run on all Pull Requests
2. `buildspec-build-dev.yml` - Builds, tags, and deploys the app's Docker image. Run on commits to the `dev` branch
3. `buildspec-promote-upper.yml` - Same as `buildspec-build.yml` but requires an `IMAGE_TAG` be set.
    - This is currently a manual promotion step per Business requirements
    - UAT deploys a fixed tag, `uat`, which is defined in the CodeBuild job's ENV variables for UAT
    - PROD requires this tag be defined before running the build manually.

### Tests

The Test-only job is run on every pushed commit to GitHub. If you open a Pull Request for that commit
the build status will show up in the `Checks` section of the PR. Currently CodeBuild does _not_ pass back
exact failures. The CloudWatch logs for that build must be accessed which is linked in the GitHub check. Alternatively
just run `sbt test` locally which should produce the same result.
