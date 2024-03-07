# Deploying Calendar Import

## Test Docker Setup

If you would just like to get a copy of tool-support up and working to test with there is a docker setup that allows
you to run it without having to build it.

### Prerequisites

To do this you need to have these tools installed:

- tool support - https://github.com/oxctl/tool-support this handles the LTI launch and tokens.
- docker - https://docker.com `docker-compose` is the key part.
- mkcert - https://github.com/FiloSottile/mkcert to generate self-signed certificates.
- nodejs - https://nodejs.org/ LTS version (this is needed for running the configuration tool) version 18+.

### Setup

#### TLS

Before starting up the docker containers you need to create a certificate file to enable HTTPS.

For the Java backend:
```bash
mkcert -pkcs12 -p12-file config/keystore.p12 localhost
```
For the Web frontend:
```bash
mkcert localhost
```

### Configure Tools

To configure the tool first run:

```bash
npx @oxctl/lti-auto-configuration init
```

This will prompt for the tool support server to use and the canvas server to configure the tools in.

Then run:
```bash
npx @oxctl/lti-auto-configuration setup
```

This will prompt for the additional configuration for the tool. The values should be set to:
- `calendar_server_url` - https://localhost:18443
- `proxy_secret` - Secret that calendar import uses to authenticate to the proxy. This needs to be 32 characters and base64url encoded. A simple way to generate this is with `pwgen 32 1  | basenc --base64url`

Then set up the tool:

```bash
npx @oxctl/lti-auto-configuration create
```

This will do the actual adding of the tool to tool support and canvas.

Add this into the file `./config/application.properties` replacing:
```
jwt.issuer=<URL of tool support server>
jwt.jwks.uri=<URL of tool support server>/.well-known/jwks.json

calendar.tenants[0].name=<short name of canvas host>
calendar.tenants[0].url=<URL of canvas host>
calendar.tenants[0].displayName=<display name for canvas host>
calendar.tenants[0].ltiClientId=<client ID of the tool installed to Canvas>
calendar.tenants[0].proxyHmacSecret=<proxy secret>
calendar.tenants[0].proxyHost=<URL of tool support server>
```

This should have installed the tool into tool-support and added it to your Canvas instance and it should be ready to use.

### Run

To start up the containers run:

```bash
docker-compose up
```
This will use a prebuild docker image for the application server that is pulled from GitHub after successfully starting
up you will have:
- The backend service running on: https://localhost:18443
- The frontend service running on: https://localhost:28443
- A MySQL database running (not accessible outside docker)

