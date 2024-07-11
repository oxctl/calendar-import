# Calendar Import Backend

## Java

This currently runs with Java 17.

## Docker

We use [jib](https://github.com/GoogleContainerTools/jib) to build our docker images. This means you can't use the standard docker tools to build our images, however we can have our images made available to a local docker daemon, todo this run:
```bash
mvn compile jib:dockerBuild
```
This will build the project and have the image available. Watch out though as jib tries to create repeatable builds which means the timestamp of the image is always UNIX epoch. We have a docker-compose file in the project which allows it to startup without.

Published images are stored in an AWS ECR repository before going to production. To push images to this respository install the [Amazon ECR Docker Credential Helper](https://github.com/awslabs/amazon-ecr-credential-helper), this will use your credentials setup for AWS for pushing to the ECR repository. With the credential helper installed you can then push an image, if you need to you can also specify which AWS profile to use:

```shell
AWS_PROFILE=ouit mvn compile jib:build
```

## Development

### Spring profiles

By default we have a H2 SQL DB setup, however the DB gets thrown away when the JVM is restarted. The simplest way to have it persist is to write out the contents to a file by enabling the `h2` Spring profile.

There's also some example/useful config in [backend/config/application-dev.properties](backend/config/application-dev.properties) that can be activated by starting the application with the `dev` profile.

### Local SSL

The simplest way to enable SSL for development is to install [mkcert](https://github.com/FiloSottile/mkcert) and the create a test SSL cert.
```bash
mkcert -pkcs12 -p12-file config/keystore.p12 localhost
```

## Deployment

### AWS

This service is deployed to AWS using elastic beanstalk. This is managed by GitHub Actions which is able todo the deployments to the development instance and then if everything looks ok a build can be deployed to the production environment.

### Secrets

When running in AWS some values are loaded from the Secret Manager. The secrets are created by the cloudformation templates,
but they are left empty. To update a secret use a command similar to the following (with `name`, `description` and `secret-string` updated to appropriate values). The name of the secret is referenced in the Elastic Beanstalk environment. 

To update and existing secret:

```bash
aws secretsmanager update-secret \
  --secret-id 'calendar-import/beta/eb-env/config' \
  --secret-string '{
    "canvas.token":"token",
    "canvas.url":"https://universityofoxford.beta.instructure.com",
    "sentry.dsn":"URL",
    "sentry.environment":"dev/prod",
    "hmac.secret":"secret",
    "spring.security.oauth2.client.registration.terms.authorization-grant-type":"client_credentials",
    "spring.security.oauth2.client.registration.terms.client-id":"terms client id",
    "spring.security.oauth2.client.registration.terms.client-secret":"terms client secret",
    "spring.security.oauth2.client.registration.terms.scope":"terms scope",
    "spring.security.oauth2.client.provider.terms.token-uri":"terms token uri",
    "dynamics.year.url":"academic year data url",
    "dynamics.term.url":"term data url",
    "calendar.tenants[0].name":"canvas",
    "calendar.tenants[0].url":"https://oxeval.instructure.com",
    "calendar.tenants[0].displayName":"University of Oxford Canvas",
    "calendar.tenants[0].ltiSecret":"LTI secret",
    "calendar.tenants[0].oauth2Id":"OAuth2 id",
    "calendar.tenants[0].oauth2Secret":"OAuth2 secret",
    "calendar.url.predefined.test.url":"calendar url",
    "calendar.url.predefined.test.username":"calendar username",
    "calendar.url.predefined.test.password":"calendar password"
  }'
```

## Notes

You can't have HTML in raw descriptions, but you can put it in another property, need to check what Canvas does: https://stackoverflow.com/questions/854036/html-in-ical-attachment    
