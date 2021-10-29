
# Java

This currently runs with Java 11.

# docker

We use [jib](https://github.com/GoogleContainerTools/jib) to build our docker images. This means you can't use the standard docker tools to build our images, however we can have our images made available to a local docker daemon, todo this run:

    mvn compile jib:dockerBuild
  
This will build the project and have the image available. Watch out though as jib tries to create repeatable builds which means the timestamp of the image is always UNIX epoch. We have a docker-compose file in the project which allows it to startup without.

Published images are stored in an AWS ECR repository before going to production. To push images to this respository install the [Amazon ECR Docker Credential Helper](https://github.com/awslabs/amazon-ecr-credential-helper), this will use your credentials setup for AWS for pushing to the ECR repository. With the credential helper installed you can then push an image, if you need to you can also specify which AWS profile to use:

    AWS_PROFILE=ouit mvn compile jib:build

# Deployment

This project is deploying onto AWS using Elastic Beanstalk (EB). There is a small EC2 instance that hosts the docker container and provides HTTPS termination through nginx. SSL certificates are put into a S3 bucket and copied onto the host at deployment time using the ec2's role ([AWS Docs](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/https-storingprivatekeys.html)). 

## SSL Rotation

Currently this service requires manual SSL regeneration and setup. The certificates should be placed into the S3 bucket and then a replacement instance deployed which will copy out the new certificates and start using them. When building the certificates any additional certificates should be added so they can be served.

    cat www.example.com.crt bundle.crt > www.example.com.chained.crt
    
## Development H2 DB

By default we have a H2 SQL DB setup, however the DB gets thrown away when the JVM is restarted. The simplest way to have it persist is to write out the contents to a file by enabling the `h2` Spring profile.

## Notes

You can't have HTML in raw descriptions, but you can put it in another property, need to check what Canvas does: https://stackoverflow.com/questions/854036/html-in-ical-attachment    

## SSL

### Development

The simplest way to enable SSL for development is to install [mkcert](https://github.com/FiloSottile/mkcert) and the create a test SSL cert.

    mkcert -pkcs12 -p12-file config/keystore.p12 localhost
   
### AWS

For AWS we need to have a SSL key/certificate, these are hosted in a S3 bucket and then copied into the EC2 instance at deployment time. The `openssl` configuration should be stored in a file matching the hostname and then a key and certificate signing request should be generated.

    openssl req -nodes -new -keyout calendar-import.canvas.ox.ac.uk-key.pem -out calendar-import.canvas.ox.ac.uk.csr -config calendar-import.cfg -batch -verbose

The CSR can then be uploaded to request the certificate.

## Deployment

### AWS

This service is deployed to AWS using elastic beanstalk. This is managed by GitHub Actions which is able todo the deployments to the development instance and then if everything looks ok a build can be deployed to the production environment. The files for doing this are in the folder [elasticbeanstalk](elasticbeanstalk).

#### Config

The additional config for the clients that are registered is loaded from a file stored in S3. This file is only reloaded when the application is re-deployed (not restarted). So to change this file you typically download it:

    aws s3 cp s3://elasticbeanstalk-eu-west-1-211318693510/files/calendar-import.canvas.ox.ac.uk-client.properties .

then you can edit the file and upload it to S3 again:

    aws s3 cp calendar-import.canvas.ox.ac.uk-client.properties s3://elasticbeanstalk-eu-west-1-211318693510/files/
