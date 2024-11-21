# Templates README - App Templates - Calendar Import 

- [Templates README - App Templates - Calendar Import](#templates-readme---app-templates---calendar-import)
  - [Prerequisites for using the templates in this directory](#prerequisites-for-using-the-templates-in-this-directory)
  - [Running the templates](#running-the-templates)
  - [region.yaml (AWS Regions: eu-west-1 \& eu-central-1)](#regionyaml-aws-regions-eu-west-1--eu-central-1)
    - [Function](#function-1)
    - [Resources Created](#resources-created-1)
    - [CloudFormation outputs / exports](#cloudformation-outputs--exports-1)
  - [s3-and-cloudfront.yaml (AWS Region: eu-west-1)](#s3-and-cloudfrontyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created)
    - [CloudFormation Parameters](#cloudformation-parameters)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-2)
    - [Notes](#notes-1)
  - [rds.yaml (AWS Region: eu-west-1)](#rdsyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created-1)
    - [CloudFormation Parameters](#cloudformation-parameters-1)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-3)
  - [eb-env.yaml (AWS Region: eu-west-1)](#eb-envyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created-2)
    - [CloudFormation Parameters](#cloudformation-parameters-2)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-4)
    - [SSH into the ec2 instance of the EB env](#ssh-into-the-ec2-instance-of-the-eb-env)


## Prerequisites for using the templates in this directory

Before running any templates in this app

**TODO: this seems out of date - these files don't exist any more**
* Ensure the templates in the following folders have been run to provision resources the templates are dependent on:
  * `..\..\0.cad-account-global`
  * `..\..\0.cad-account-region` - for the region in which we wish to run the app templates
* The prefixed numbers in the template filenames indicate the sequential order of running them.  It is important to maintain this other because of dependencies between templates. 
* Where 2 templates have the same sequence prefix, there are no dependencies and as such they can be run in any order

## Running the templates

For a nested stack to work the templates need to be in S3 this is because the template URI must be a S3 URL. The package step uploads templates to S3 and then re-writes the template to point to the S3 URL.

```bash
aws cloudformation package --template-file stack.yaml --s3-bucket oxcanvas-nonprod-shared --s3-prefix cloudformation-nonprod/calendar-import --output-template-file build/stack.yaml 
```

The stack can then be deployed using the built template:

```bash
aws cloudformation deploy --template-file ./build/stack.yaml --stack-name cad-apps-calendar-import --capabilities CAPABILITY_NAMED_IAM --parameter-overrides file://aws/730335587339.json
```

If things didn't go well then you can remove check the logs with:

```bash
aws cloudformation describe-stack-events --stack-name cad-apps-calendar-import --output table
```

Then delete things with:

```bash
aws cloudformation delete-stack --stack-name cad-apps-calendar-import
aws cloudformation wait stack-delete-complete --stack-name cad-apps-calendar-import
```

## region.yaml (AWS Regions: eu-west-1 & eu-central-1)

### Function

* Create other AWS resources required once for each region in which we intend to deploy calendar-import
* Run in DR region only if we want to test or deploy calendar-import in that region

### Resources Created

* EB application for the app.

### CloudFormation outputs / exports

* EB app name

## s3-and-cloudfront.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* a beta or prod buckett
* Creates a CloudFront distribution for the prod or dev bucket
  * along with the other resources required by CloudFront
  * Enables CloudFront logging to a prefix of the shared bucket
* Creates a route53 alias record pointing to the CloudFront distribution
  * It's able to do this, by importing the route53 ZoneId for the app subdomain exported by an earlier template

### CloudFormation Parameters

* `cfSSLCertARN` - the ARN of the SSL certificate for cloudfront
  * issued in one of the account gloal templates
  * copy that from the exports of the stack built with that template (in Region `us-east-1`)  - `Physical ID` column
* `wafARN` - the ARN of the WA created in the same template kept in account-global folder
* Accept defaults for the rest of the parameters, for calendar-import

### CloudFormation Outputs / Exports

  * `cfOID` .. the Origin Access Id for the cloudfront distribution .. may be useful later
  * `cfDistributionID` .. the ID of the cloudfront distribution 

### Notes

* After the stack builds, one can visit the static bucket at, eg:
  * https://static.calendar-import.apps-nonprod.canvas.ox.ac.uk
  * https://static.calendar-import.apps.canvas.ox.ac.uk
  * https://static.calendar-import.apps-nonprod.canvas.ox.ac.uk
* if there is an index.html file there it will be displayed as a webpage

## rds.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* Creates an RDS instance for app, along with:
  * a DB parameter group
  * A secrets manager record for the auto-generated DB password, and a link between the RDS DB and the secrets manager entry
* Cloudwatch alarms for onitoring the RDS instance
* SNS resources for reporting on the monitoring the RDS instance 

### CloudFormation Parameters

* Accept defaults

### CloudFormation Outputs / Exports

  * `dbSecretProdRegion` .. the secrets manager secret created for the RDS instance
  * The RDS DB's endpoint address and instance ARN

## eb-env.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* Creates an REB env, along with:
  * an application version resource
  * various EB env configuration templates
* A cloudfront distribution for fronting the EB env
  * Traffic to the EB env is restricted to only traffic originating from cloudfront
* A Route53 alias record for the cloudfront distribution, eg:
  * `appname.apps.canvas.ox.ac.uk`
  * `appname.apps-nonprod.canvas.ox.ac.uk`
* SNS resources for monitoring the EB env
* Event rule resources for monitoring the EB env

### CloudFormation Parameters

* Accept defaults
* *NOTE:*
  * To avoid putting back the platform's solution stack for the EB, env, we have set it up as a mapping
  * This means that we have to look uo the latest solution if this is a new environment, just to be sure we are using the latest.
  * After that, in further updates, we would not need to bother with this parameter
  * To look up the solution stack to use, run the following command (once per region of interest .. note, the region is mentioned twice in the command (once as an ARN)
  * In the output of the command, an AMI ID is also listed, feed that into the apt field in the mapping too)
  * ```bash
      aws elasticbeanstalk describe-platform-version --region eu-west-1 \
        --platform-arn "arn:aws:elasticbeanstalk:eu-west-1::platform/Docker running on 64bit Amazon Linux 2023/4.3.0" \
        --query "{AMI: PlatformDescription.CustomAmiList[1].ImageId,solStack: PlatformDescription.SolutionStackName}" \
        --out=table
              ----------------------------------------------------------------------------
              |                          DescribePlatformVersion                         |
              +------------------------+-------------------------------------------------+
              |           AMI          |                    solStack                     |
              +------------------------+-------------------------------------------------+
              |  ami-087e2ccdd3b0de771 |  64bit Amazon Linux 2023 v4.3.0 running Docker  |
              +------------------------+-------------------------------------------------+
      ```

### CloudFormation Outputs / Exports

  * various exports, not imported by any other template

###  SSH into the ec2 instance of the EB env

An example of connecting to the ec2 instance of the EB env

```bash
appName="calendar-import"
envType=beta
region="eu-west-1"

#lookup instance-id
instanceId=$(aws --region ${regIon} ec2 describe-instances --filters "Name=tag:Name,Values=${appName}-${envType}" --query "Reservations[*].Instances[*].[InstanceId]")

# ssh only session
aws ssm start-session \
    --region $region \
    --target $instanceId \
```
