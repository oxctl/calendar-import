# Deploying Calendar Import

## Test Docker Setup

If you would just like to get a copy of tool-support up and working to test with there is a docker setup that allows
you to run it without having to build it.

### Prerequisites

To do this you need to have these tools installed:

- tool support - https://github.com/oxctl/tool-support this handles the LTI launch and tokens.
- docker - https://docker.com `docker-compose` is the key part.
- mkcert - https://github.com/FiloSottile/mkcert to generate self-signed certificates.
- nodejs - https://nodejs.org/ LTS version (this is needed for running the configuration tool).

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


#### Configuration

The tool needs configuration for the Canvas instance to work with. The calendar import tool reads its config from the database, but supports importing config into the database on startup.

```bash
cat <<EOF > backend/config/application.properties
# Allow frontend origin
frontend.origins=https://localhost:3000

# Configuration
calendar.tenants[0].name={hostname.instructure.com}
calendar.tenants[0].url={https://hostname.instructure.com}
calendar.tenants[0].displayName={Display Name}
calendar.tenants[0].ltiClientId={12345....}
calendar.tenants[0].proxyHost=https://localhost:18443
calendar.tenants[0].proxyHmacSecret={Secret....}
EOF
```

### Run

To start up the containers run:

```bash
docker-compose up
```

This should start up the mysql database and the application server listening on https://localhost:18443

This will use a prebuild docker image for the application server that is pulled from GitHub.

### Configure Tools

With the calendar import tool running you can now configure tool-support and Canvas to use it.
This configuration can be automated with https://github.com/oxctl/lti-auto-configuration

Copy the sample config:

```bash
cd tool-config
cp -n local-example.json local.json
```

Then edit the values:
- `canvas_url` - The URL to a Canvas instance to install the tool on.
- `tool_support_url` - The URL of a tool support instance that will handle the LTI launches.
- `proxy_server_url` - The URL of a tool support instance that will handle the API proxy request.
- `canvas_token` - An admin token that is allowed to edit developer keys and install LTI tools.
- `tool_support_username` - Username for API requests to tool support.
- `tool_support_password` - Password for API requests to tool support.
- `proxy_secret` - Secret that calendar import uses to authenticate to the proxy when user isn't present.

Then set up the tool:

```bash
npx @oxctl/lti-auto-configuration -c -t ./tool-config/tool-config.json -s ./tool-config/local.json -ss ./tool-config/local.json  -X "lti_tool_url=https://localhost:28443" 
```

This should have installed the tool into tool-support and added it to your Canvas instance and it should be ready to use.


