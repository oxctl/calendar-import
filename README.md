
# Java

This currently works with Java 8 and fails to start with Java 11. It should be updated to run with Java 11 at some point.

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


Canvas OAuth documentation doesn't mention that you can use replace_tokens to indicate you want older tokens removed. (https://canvas.instructure.com/doc/api/file.oauth.html)

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

There's no pagination in Quartz for finding triggers so we don't want to use any of the searching methods and hence we need our own database table to associate a trigger to a user.

Want to veto some jobs if the user has allot of jobs outstanding.

use exceptions from the job to indicate if the job should be re-tried and use different types for retry and non-retryable.

Store upload somewhere and then pass in the URL to it. How to remove after processing? Trigger listener? 

