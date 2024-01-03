import * as Sentry from "@sentry/react";
import {Integrations} from "@sentry/tracing";

/**
 * Sets up sentry.
 * @param dsn The DSN (can be null).
 * @param environment The environment (shouldn't be null if DSN set).
 */
function sentryInit(dsn, environment)  {
    // Load sentry setup if defined.
    // This is done early in the application to catch as much as possible.
    if (dsn) {
        Sentry.init({
            dsn,
            environment: environment?environment:"unknown",
            integrations: [new Integrations.BrowserTracing()],

            // Set tracesSampleRate to 1.0 to capture 100%
            // of transactions for performance monitoring.
            // We recommend adjusting this value in production
            tracesSampleRate: 1.0,
        });
    }
}

export {sentryInit}