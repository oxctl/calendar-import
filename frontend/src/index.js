import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'react-redux'
import * as Sentry from "@sentry/react";
import { Integrations } from "@sentry/tracing";
import App from './App'
import store from './store'
import {settings} from "./utils/settings";

{
    // Load sentry setup if defined.
    // This is done early in the application to catch as much as possible.
    const dsn = settings?.sentryDsn
    if (dsn) {
        Sentry.init({
            dsn,
            environment: settings.sentryEnv?settings.sentryEnv:"unknown",
            integrations: [new Integrations.BrowserTracing()],

            // Set tracesSampleRate to 1.0 to capture 100%
            // of transactions for performance monitoring.
            // We recommend adjusting this value in production
            tracesSampleRate: 1.0,
        });
    }
}

ReactDOM.render(
    <Provider store={store}>
        <App/>
    </Provider>,
    document.getElementById('app'))
