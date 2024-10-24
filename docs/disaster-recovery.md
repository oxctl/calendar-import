# Disaster Recovery

If something has gone wrong, and we need to recover the service from scratch then this document outlines the steps.

## Overview

The whole signup application is deployed through CloudFormation and this is launched from GitHub Actions. To test
recovery of the service we create a branch, enable builds for that branch and push it to GitHub. This should cause
GitHub Actions to build the application and deploy it to AWS using CloudFormation.

## Requirements

- you have a login to an AWS account where you can access shared DB snapshots (if needed).
- the low-level infrastructure has been deployed to an AWS account (e.g. VPC).
- GitHub Actions are able to connect to the AWS account (should be if infrastructure is there).

## Steps

### Deploy the application

1. Checkout the code at the point you wish to deploy the repository; this will probably be the latest commit but can be earlier in the repository history.
2. Create a new branch onto which the deployment changes will be made (branch name isn't critical, e.g. `dr-branch` and can be deleted after being needed).
3. Set the `appName` in the referenced `AWS_PARAMETERS` file to be unique (you can't run 2 apps with the same name). This file is in the `./aws` folder and referenced in the workflow files. For example, for prod, edit `aws/211125465582.json` and add
    ```yaml
    "appName=myApp-dr-branch"
    ```
   **Note: The `appName` is interpolated into various resource names which have restrictions - so as a rule of thumb stick to these rules:**
   * **Only lowercase alphanumeric characters and hyphens.**
   * **Maximum of 20 characters.**
   * **First character must be a letter, cannot end with a hyphen or contain two consecutive hyphens.**


4. Find the RDS backup you want to restore in AWS Backup Vault and add the arn to the parameters file i.e.
    ```yaml
    "snapshotToUse={snapshot arn}"`
    ```
5. To build on push, update the GitHub actions in `.github/workflows` (`backend_dev/prod.yml`, `frontend_dev/prod.yml`) to build on the new branch. E.g.
```yaml
on:
  push:
    branches: [ 'master', 'dr-branch' ]
```
6. Commit changes and push the new branch.
7. If the actions were updated (step 5), the GitHub actions will start the build automatically. Otherwise, manually run the `Release` action on the new branch you created.

### Resotre S3 backups

8. To restore the S3 bucket contents, first find the new S3 bucket(s) that were created by CloudFormation and enable ACLs (this is to allow backups to work).
  * In the AWS Console, click into the bucket, then Permissions tab -> Object Ownership -> Edit -> change to "ACLs enabled". 
  * Repeat for all S3 buckets that need restoring.
9. Click into the S3 backup(s) in the AWS Backup Vault and click restore
  * Restore destination -> Use existing bucket
  * this should be the existing production bucket (not the DR one)
  * you must select to use the IAM role cad-aws-account-backup.... to handle the restore as the default one doesn't have enough permissions
  * In testing restores take about 10 minutes.
10. Disable the ACLs on the S3 buckets (this would get removed next time cloudformation updates the stack).

### Other stuff

11. Update the secret in AWS Secrets Manager called `${appName}/${envType}/eb-env/config` (e.g., `myApp/prod/eb-env/config`) and set values as set in 1password (changing credentials/URLs if doing a test).


### Tear things down

If something didn't work correctly, and you wish to start again there is a GitHub action called 'Delete Stack' that will attempt to clean-up everything associated with a deployment. Before removing all the CloudFormation stacks it will empty the created S3 buckets so that the CloudFormation stacks can be successfully deleted (CF will refuse to delete a non-empty S3 bucket).

**Note: the prod RDS instance is created with Deletion protection - this needs to be unchecked before Delete Stack will work**
