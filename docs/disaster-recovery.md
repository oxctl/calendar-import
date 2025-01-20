# Disaster Recovery

If something has gone wrong, and we need to recover the service from scratch then this document outlines the steps.

## Overview

The whole application is deployed through CloudFormation and this is launched from GitHub Actions. To test
recovery of the service we create a branch, enable builds for that branch and push it to GitHub. This should cause
GitHub Actions to build the application and deploy it to AWS using CloudFormation.

## Requirements

 - you have a login to an AWS account where you can access shared DB snapshots (if needed).
 - the low-level infrastructure has been deployed to an AWS account (e.g. VPC).
 - GitHub Actions are able to connect to the AWS account (should be if infrastructure is there).

## Steps

### Deploy the application

1. Checkout the code at the point you wish to deploy the repository; this will probably be the latest commit but can be earlier in the repository history.
2. Create a new branch named with "dr-" at the start e.g. `dr-appName`
   - **if the branch does not start with "dr-" then the DR process will not work**
3. Find the RDS backup you want to restore in AWS Backup Vault and add the arn to [dr.json](../aws/dr.json) e.g.:
    ```yaml
    "snapshotToUse={snapshot arn}"`
    ```
4. You shouldn't need to make any other changes to the file, but note:
   - the `appName` should be unique - if it uses the same name as an existing app it will overwrite it 
   - it should only use lowercase alphanumeric characters and hyphens
   - it should have a maximum of 20 characters
   - the first character must be a letter, and it cannot end with a hyphen or contain two consecutive hyphens
5. Commit changes and push the new branch. The GitHub actions will start the build automatically (provided the branch name starts with "dr-").
6. Update the secret in AWS Secrets Manager called `${appName}/${envType}/eb-env/config` (e.g., `signup-dr/prod/eb-env/config`) and set values as set in 1password (changing credentials/URLs if doing a test).

## S3 buckets

5. To restore the S3 bucket contents, first find the S3 bucket you wish to restore to and enable ACLs (this is to allow backups to work).
   - In the AWS Console, go to Amazon S3 -> Buckets -> click into the bucket -> Permissions tab -> Object Ownership -> Edit and change to "ACLs enabled".
   - (This will be in the DR region.)
6. Locate the S3 backups in the AWS Backup Vault and restore to the existing S3 buckets.
   - AWS Backup -> Vaults -> {Vault name} -> {Recovery point} -> Restore
   - Restore type -> Restore entire bucket
   - Restore destination -> Use existing bucket -> Bucket name {the bucket you enabled ACLs for}
   - Restore role Info -> Choose an IAM role -> Role name: `cad-aws-account-backup-BackupRole...`
7. Finally, disable the ACLs that you enabled in step 5.

### Tear things down

If something didn't work correctly, and you wish to start again there is a GitHub action called 'Delete Stack' that will attempt to clean-up everything associated with a deployment. Before removing all the CloudFormation stacks it will empty the created S3 buckets so that the CloudFormation stacks can be successfully deleted (CF will refuse to delete a non-empty S3 bucket).

**Note: the prod RDS instance is created with Deletion protection - this needs to be unchecked before Delete Stack will work**
