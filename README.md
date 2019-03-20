
# Java

This currently runs with Java 11.

## LTI Configuration

When the server is up and running there is a public URL of `/config.xml` that hosts the XML needed to configure the tool. This pulls the launch URL from the current request and so can be used for any host.

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
    
then merge the files into a keystore:

    openssl pkcs12 -export -inkey calendar.local-key.pem -in calendar.local.pem -name tomcat -out keystore.p12
    
    
On macOS you can add this additional hostname to the DNS:

    dns-sd -P calendar _http._tcp local 8080 calendar.local 127.0.0.1



    
    
    
   

