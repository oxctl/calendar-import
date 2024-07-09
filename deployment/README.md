# Calendar Import Deployment Tests

The deployment tests use [Playwright](https://playwright.dev/) to check that the tool has been deployed successfully.

They run automatically after deploying to DEV and PROD, and they can also run manually using Github actions and selecting the environment.

The following variables are required in Github as environment secrets (DEV, PROD) or locally in a '.env' file.

```properties
OAUTH_TOKEN=****This is a secret token****
COURSE_ID=123456
TOOL_ID=123456
CANVAS_HOST=https://url.to.canvas.instance
BACKEND_URL=https://url.to.the.tool.backend
```
