

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


For spring 5 it's Spring Security OAuth support that is recommended, there is special spring boot code, but that's aimed at people migrating between old spring boot oauth support and the newer code.


Canvas OAuth documentation doesn't mention that you can use replace_tokens to indicate you want older tokens removed. (https://canvas.instructure.com/doc/api/file.oauth.html)

https://github.com/spring-projects/spring-security/issues/5494

Scope should be optional on client registration but isn't.