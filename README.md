# Calendar Import

A tool to import calendar events in Canvas.

 - [![Backend DEV](https://github.com/oxctl/calendar-import/actions/workflows/backend_build.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/backend_build.yml)
 - [![Frontend DEV](https://github.com/oxctl/calendar-import/actions/workflows/frontend_dev.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/frontend_dev.yml)

## Overview

This tool is split into 2  parts:

 - [backend](backend) The Java REST backend that acts as an OAuth2 Resource Server
 - [frontend](frontend) The Instructure UI/React static frontend.

## History

Originally this tool was written as a Java MVC tool and then later refactored to have a React frontend and a REST backend.
This means had we been starting from scratch some things would have been done differently.

## Dependent Services

This tool depends on the LTI launch service to handle the LTI launch to React frontend flow. It then uses the proxy
service to handle requests to load the sections and then all calendar import requests are made through the proxy. This
means we don't hold any of the user's tokens in the calendar import tool itself.

## Canvas Configuration

Both a LTI developer key and an API developer key need to be created for this tool to function.

### LTI Developer Key

* Key name:
    - prod: Calendar Import
    - dev: Calendar Import (dev) or (myname)
* Owner email:
    - prod: acit-sys-apps@maillist.ox.ac.uk
    - dev: personal.email@address
* Redirect URIs:
    - prod: https://lti.canvas.ox.ac.uk/lti/login
    - dev: https://lti-dev.canvas.ox.ac.uk/lti/login or https://localhost:18443/lti/login
* Method: Manual entry
* Title:
    - prod: Calendar Import
    - dev: Calendar Import (dev) or (myname)
* Description: Allows the import of events into the Canvas calendar.
* Target Link URI:
    - prod: https://static.canvas.ox.ac.uk/calendar-import/
    - dev : https://static-dev.canvas.ox.ac.uk/calendar-import/ or https://localhost:3000
* OpenID Connect Initiation URL:
    - prod: https://lti.canvas.ox.ac.uk/lti/login_initiation/universityofoxford-ci-prod
    - dev: https://lti-dev.canvas.ox.ac.uk/lti/login_initiation/<instance>-ci-<dev|yourFirstName>
* JWK Method:
    - prod: Public JWK URL
    - dev: Public JWK URL or switch to Public JWK and paste in JWK
* Public JWK
    - prod: https://lti.canvas.ox.ac.uk/.well-known/jwks.json
    - dev: https://lti-dev.canvas.ox.ac.uk/.well-known/jwks.json
* Additional Settings: Custom fields:
    ```
    canvas_course_id=$Canvas.course.id
    canvas_user_id=$Canvas.user.id
    canvas_course_name=$Canvas.course.name
    canvas_api_base_url=$Canvas.api.baseUrl
    person_address_timezone=$Person.address.timezone
    com_instructure_brand_config_json_url=$com.instructure.brandConfigJSON.url
    canvas_user_prefers_high_contrast=$Canvas.user.prefersHighContrast
    ```
* Privacy Level: Public
* Placements: Course Home Sub Navigation, User Navigation (this will only be set when we want to show the oxford terms)

Then once the key is entered switch to the JSON view and update the placement to include an icon:

    "placements": [
          {
          "placement": "course_home_sub_navigation",
          "message_type": "LtiResourceLinkRequest",
          "canvas_icon_class": "icon-calendar-add"
          }
    ]

### API Key

This tool uses the Canvas Proxy to make requests against the Canvas API, to do this it needs a developer key:

* Key name:
    - prod: Calendar Import
    - dev: Calendar Import (dev) or (myname)
* Owner email:
    - prod: acit-sys-apps@maillist.ox.ac.uk
    - dev: personal.email@address
* Redirect URIs:
    - prod: https://proxy.canvas.ox.ac.uk/login/oauth2/code/universityofoxford-ci-prod
    - dev: https://proxy-dev.canvas.ox.ac.uk/login/oauth2/code/<instance>-ci-<dev|yourFirstName>
* Enforce Scopes: Enabled
* Scopes:
  ```
  url:GET|/api/v1/calendar_events
  url:POST|/api/v1/calendar_events
  url:DELETE|/api/v1/calendar_events/:id
  url:GET|/api/v1/courses/:course_id/sections
  ```

When in development it is easier to disable the enforced scopes, however in production/test we must limit scopes to reduce exposure in the event of a compromise.


### Installing the tool
- Go to https://oxeval.instructure.com/
- Click on course, If there is no course create a course
- Click on Settings at the bottom left tab
- Click on Apps from the top menu tab, then "View App Configurations"
- Click "+ App", Select "By client ID" from configuration type and add your developer key ID  (it must have been created from if your lti    creation key was successful)
- Click Add, then install, and you should have a (course management) tool - with the same name as the LTI key
- This should now appear on the left hand side of the list 

### Configuring Predefined Calendars

This tool has support for reading term data from a feed produced from Azure Dynamics. Two feeds are used, one to get the data on the academic years that are available and when they run (to work out the current and next year). Then a second feed is used to work out the weeks based on the term data. The URLs that the JSON can be downloaded from are protected by OAuth2 and credentials are needed that work against the Azure AD are needed. To then configure the application spring properties like these need to be set:

    # The OAuth2 grant type (always client_credentials)
    spring.security.oauth2.client.registration.terms.authorization-grant-type=client_credentials
    # The client ID, must be provided
    spring.security.oauth2.client.registration.terms.client-id=1234....
    # The client secret, must be provided
    spring.security.oauth2.client.registration.terms.client-secret=top-secret....
    # The scope, must be provided
    spring.security.oauth2.client.registration.terms.scope=api://....

    # The token URI, must be provided
    spring.security.oauth2.client.provider.terms.token-uri=https://login...

    # The URL to get the academic year data from (will be different for prod/test/uat)
    dynamics.year.url=https://...
    # The URL to get the term data from (will be different for prod/test/uat)
    dynamics.term.url=https://...

If you don't supply this configuration, the tool will startup, but the predefined terms won't be defined.

## Releasing
 
This tool should be released in one step, updating the maven versions and updating the npm versions.
To make a release use the `release.sh` script in the repository and specify the new version to release.

    ./release.sh 1.2.3

This will increment the version in the [frontend](frontend) and [backend](backend) projects to the new version, commit the changes, tag the new release and then increment to a new snapshot in the backend. If everything looks ok and seems to have worked you can then push the changes.

   git push
   git push --tags

And then GitHub Actions should build the new tag and it can be deployed to production once tested.
 
## Sentry

Application errors are reported using https://sentry.io for this application. There are 2 DSNs, one for development and one for production. There's no DSN for local development. Sentry is setup as early as possible in the application to capture as many errors as possible.

The frontend and backend have different sentry projects.
