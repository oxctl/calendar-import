
# Java

This currently runs with Java 11.

## LTI Configuration

When the server is up and running there is a public URL of `/config.xml` that hosts the XML needed to configure the tool. This pulls the launch URL from the current request and so can be used for any host.

Currently we just support one Tool Consumer but it's setup to allow multiple TCs to be configured.

# Job Progress

How to deal with Job progress? We need to return a URL that can be polled for changes in state. Want to keep job 
progress reporting detached from where the actual job runs. Have a quartz job listener that updates the coarse progress
of the job.

# LTI Launch

# Where to store Service config (oauth stuff/lti config?)

Could put this on the tenant...
Will want to then store the tenant on the session/jwt.

# Email

We want email address of the user so that if imports repeatedly fail we can contact them.

# Code style

We just use Google Code style for this project. There's  [XML config for Intellij](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml) that shoudl be used.

# docker

We use [jib](https://github.com/GoogleContainerTools/jib) to build our docker images. This means you can't use the standard docker tools to build our images, however we can have our images made available to a local docker daemon, todo this run:

    mvn compile jib:dockerBuild
  
This will build the project and have the image available. Watch out though as jib tries to create repeatable builds which means the timestamp of the image is always UNIX epoch. We have a docker-compose file in the project which allows it to startup without 

Published images are stored in an AWS ECR repository before going to production. To push images to this respository install the [Amazon ECR Docker Credential Helper](https://github.com/awslabs/amazon-ecr-credential-helper), this will use your credentials setup for AWS for pushing to the ECR repository. With the credential helper installed you can then push an image, if you need to you can also specify which AWS profile to use:

    AWS_PROFILE=ouit mvn compile jib:build
    
# Deployment

This project is deploying onto AWS using Elastic Beanstalk (EB). There is a small EC2 instance that hosts the docker container and provides HTTPS termination through nginx. SSL certificates are put into a S3 bucket and copied onto the host at deployment time using the ec2's role ([AWS Docs](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/https-storingprivatekeys.html)). 

## SSL Rotation

Currently this service requires manual SSL regeneration and setup. The certificates should be placed into the S3 bucket and then a replacement instance deployed which will copy out the new certificates and start using them. When building the certificates any additional certificates should be added so they can be served.

    cat www.example.com.crt bundle.crt > www.example.com.chained.crt



# Could maybe get away without LTI and just use OAuth

We need OAuth anyway for updating the calendar entries so what does LTI get us apart from details of the tenant that the user is coming from? Could just embed this in the link on the page and then be able to add an import link to the Calendar page without needing LTI.

LTI does allow for us to share the access token across multiple browsers and only have the user have one token registered against Canvas.

Need to get
- details of the current user.
- list of all courses and group to build list of places to put calendar
- calendar API endpoints to read and update events

In User Workspace as all the work we are doing is based on the user. 


For spring 5 it's Spring Security OAuth support that is recommended, there is special spring boot code, but that's aimed at people migrating between old spring boot oauth support and the newer code.

There should be authentication events, but I don't see them happening.


Canvas OAuth documentation doesn't mention that you can use replace_tokens to indicate you want older tokens removed. (https://canvas.instructure.com/doc/api/file.oauth.html). This is now implemented by having a custom 

https://github.com/spring-projects/spring-security/issues/5494

Scope should be optional on client registration but isn't this is why we have to override the class..

If you don't have an AuthenticationManager bean then the WebSecurityConfigurerAdapter refuses to use the supplied EventListener. It is the AuthenticationManagerBuilder that actually refuses and then this results in getting a

Look like the obly place postProcessors are called is: org.springframework.security.config.annotation.SecurityConfigurerAdapter.postProcess and this is main called from Configurers.

Support for refresh tokens is coming, should be in 5.1, this makes it easier to handle refreshing them when calling an API as a client.
https://github.com/spring-projects/spring-security/issues/4371
If we end up using this then to not have to re-implement refresh handling 

Pulling configuration from the job means we can had different limits for different tenants as we can define different jobs for different tenants.

Should switch to interruptable job so that if we want to we can stop the job. See reference to Thread.interrupt() for reference as well. NIO looks to be a better as blocking calls get interrupted. Does Spring WebClient use NIO?

OkHttp doesn't appear to use NIO :-(

Keeping OAuthe2Authentication in the session might not be the best in the long run as we may want long lived session or session from LTI, in which case the OAuth2 token may be in the database.

There's no pagination in Quartz for finding triggers so we don't want to use any of the searching methods and hence we need our own database table to associate a trigger to a user, we will also want to associate a context to a job.

Want to veto some jobs if the user has allot of jobs outstanding.

use exceptions from the job to indicate if the job should be re-tried and use different types for retry and non-retryable.

Store upload somewhere and then pass in the URL to it. How to remove after processing? Trigger listener? 

With the upgrade to Spring 5.1 this is the token that it failing to parse onto a OAuth2AccessTokenResponse

    {
    "access_token":"token-example",
    "token_type":"Bearer",
    "user":{
       "id":73,
       "name":"Superadmin 03",
       "global_id":"115370000000000073",
       "effective_locale":"en-GB"
    },
    "refresh_token":"token-example",
    "expires_in":3600
    }
    
    
Authorization, have authorized contexts consisting of tenant/context in session. This allows LTI launches from multiple location to work.
    
You can't have HTML in raw descriptions, but you can put it in another property, need to check what Canvas does: https://stackoverflow.com/questions/854036/html-in-ical-attachment    

## SSL

The simplest way to enable SSL for development is to install [mkcert](https://github.com/FiloSottile/mkcert) and the create a test SSL cert.

    mkcert calendar.local
   
then merge the files into a keystore (stored in config folder):

    openssl pkcs12 -export -inkey calendar.local-key.pem -in calendar.local.pem -name tomcat -out config/keystore.p12

On macOS you can add this additional hostname to the DNS:

    dns-sd -P calendar _http._tcp local 8080 calendar.local 127.0.0.1

on other platforms you may be able todo something similar or alternatively, just use localhost or edit your platform's hosts file.


## Cookies

Need to switch to path based session cookie and that way we don't have to manage things ourselves. So we set a cookie for /canvas/1223 and then we don't have to worry about multiple tenants. Could possibly just do it at the top level...

We might be able todo this with a threadlocal and a custom [SessionCookieConfig](https://docs.oracle.com/javaee/6/api/javax/servlet/SessionCookieConfig.html). Spring Session also allows this, however the problem is going to be that the initial launch is to a URL that doesn't already include the path and the session setup is done early in the request.


# Second Login

OAuth2ClientAuthenticationProcessingFilter should have a `successfulAuthentication()` method which doesn't set the security context, but updates the existing context. Then instead it leaves the existing authentication and adds another role.

Maybe we can but the LTI_USER role as a requirement for the oauth filters so that you can't get sent for OAuth2 authentication when your LTI session has expired.

This doesn't work because https://github.com/spring-projects/spring-security/issues/373 caused any previous authentication information to be removed from the securityContext as soon as the user doesn't have enough permission to access a resource. It's the ExceptionTranslationFilter that does this.

    
# OAuth2Client

Instead of using a second login we can just use the OAuth2Client support. This keeps LTI being the primary principal/authentication but allow a method resolver to make sure that the user has a OAuth token before invoking a method that needs the token.

It doesn't appear to allow us to only get the token just in time as the request saving just does a redirect after successful authentication rather than replaying the request/response objects which means that if we capture a POST it will get converted to a GET after the authentication is done.    

# No Cookies

Detect this in JavaScript and display a message on the no-authentication page telling the user.

# TODO

- Blocked cookie detection
- Marking all events as ours.

