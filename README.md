# Calendar Import

A tool to import calendar events into Canvas.

[![Backend Dev](https://github.com/oxctl/calendar-import/actions/workflows/backend_dev.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/backend_dev.yml)
[![Frontend Dev](https://github.com/oxctl/calendar-import/actions/workflows/frontend_dev.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/frontend_dev.yml)
[![Backend Prod](https://github.com/oxctl/calendar-import/actions/workflows/backend_pro.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/backend_pro.yml)
[![Frontend Prod](https://github.com/oxctl/calendar-import/actions/workflows/frontend_pro.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/frontend_pro.yml)
[![Create New Release](https://github.com/oxctl/calendar-import/actions/workflows/release.yml/badge.svg)](https://github.com/oxctl/calendar-import/actions/workflows/release.yml)

## Overview

This repository is organised as follows:

- [aws](aws) The AWS Cloudformation templates that are used to deploy development and production. See [README.md](aws/README.md).
- [backend](backend) The Java REST backend that handles the application requests. See [README.md](backend/README.md).
- [deployment](deployment) Automatic tests of the deployed tool.  See [README.md](deployment/README.md).
- [frontend](frontend) The Instructure UI/React static frontend. See [README.md](frontend/README.md).
- [tool-config](tool-config) Configuration files for `lti-auto-configuration` to deploy to Canvas/Tool Support.

This tool uses the Tool Support Service to handle the LTI 1.3 launch and at the end of the launch the user's browser is redirected to the frontend URL and given an `id` in the URL that can be used to retrieve a JWT from the LTI Auth Service. The frontend can then use this token to make requests against the backend (eg to load the list of events). The first request is makes is to check what role the current user has and then dependant on this it either shows the teacher or student UI.

### Automatic Configuration

This tool uses [lti-auto-configuration](https://github.com/oxctl/lti-auto-configuration) to manage its configuration in Tool Support and Canvas.

If you haven't configured `lti-auto-configuration` before then first you need to point it at your installations:

```bash
npx @oxctl/lti-auto-configuration init
```

To deploy this tool some additional values also need to be set, these can be added with:

```bash
npx @oxctl/lti-auto-configuration setup
```

- title_suffix - This is some text to append to the name of the tool, if it's a developer deployment it's helpful to add your name here to distinguish between multiple copies of the same tool.
- frontend_url - The URL that the frontend will be served from. e.g. `https://localhost:3000` when in development.
- backend_url - The URL that the backend can be accessed at. e.g. `https://localhost:8443` when in development.
- lti_registration_id - The ID that the tool is registered with in tool support. Needs to be unique. e.g. `ci-dev-markus`
- user_only_delete - Maps to an LTI custom field that disables the possibility of importing new calendars in the Users' settings since there is a new Canvas feature that has to be used instead.

The tool configuration can then be created:

```bash
npx @oxctl/lti-auto-configuration create
```

`lti-auto-configuration` also allows updating and deleting of the configuration, see the README for details.

## History

Originally this tool was written as a Java MVC tool and then later refactored to have a React frontend and a REST backend.
This means had we been starting from scratch some things would have been done differently.

## Releasing using GitHub actions

There is a GitHub action to [perform releases](https://github.com/oxctl/manage-courses/actions/workflows/release.yml), you can create a Major, Minor or a Patch release, then push it to production.

## Spring Boot Actuator

Calendar Import uses Spring Boot Actuator for monitoring. The `health` endpoint is mapped to `/actuator/health`.

## Deployment

If you are just looking to deploy the service there is documentation on how to do this: [docs/deploy.md](docs/deploy.md).

## Configuration

### Predefined Calendars

This tool has support for reading term data from a feed produced from Azure Dynamics. Two feeds are used, one to get the data on the academic years that are available and when they run (to work out the current and next year). Then a second feed is used to work out the weeks based on the term data. The URLs that the JSON can be downloaded from are protected by OAuth2 and credentials are needed that work against the Azure AD are needed. To then configure the application spring properties like these need to be set:

```properties
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
```

If you don't supply this configuration, the tool will startup, but the predefined terms won't be defined.

### Calendars

The URLs supported by this are http://, https:// and calendar:// . The calendar prefix references a configured calendar which can optionally also include HTTP basic authentication. To configure these calendar add Spring properties under the prefix `calendar.url.predefined` followed by the name of the calendar. For example:

```properties
# Example: BSG MPP calendar accessed as calendar://bsg-mpp
calendar.url.predefined.bsp-mpp.url=https://ict.bsg.ox.ac.uk/canvas/${user.sis_id}.csv
calendar.url.predefined.bsg-mpp.username=canvas
calendar.url.predefined.bsg-mpp.password=secret1234
```

## Importing to Personal Calendars

The calendar import tool can use the Link Selection LTI placement to allow administrators/teachers to configure a calendar from a URL that users can import into their personal calendar. Any user who then visits the URL will get the option to import those events into their personal calendar. The calendar import tool will poll that URL and then update the users calendar with any changes that happen over time.

Then when configuring the calendar import tool from the Modules tool set the URL to `calendar://bsg-mpp` and this will get expanded by the server and the authentication will get added to any requests that are made. NB one cannot parameterise or add 'sub-folders' to this URL in the UI, this must be done in the configurtation 'properties' file - ${course.id} could also be used as a parameter, eg, https://ict.bsg.ox.ac.uk/canvas/${course.id}/${user.sis_id}.csv

## Sentry

Application errors are reported using https://sentry.io for this application. There are 2 DSNs, one for development and one for production. There's no DSN for local development. Sentry is setup as early as possible in the application to capture as many errors as possible.

The frontend and backend have different sentry projects.
