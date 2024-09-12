import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'react-redux'
import * as Sentry from "@sentry/react";
import App from './App'
import store from './store'

{
    // Load sentry setup if defined.
    // This is done early in the application to catch as much as possible.
    const dsn = process.env.REACT_APP_SENTRY_DSN
    if (dsn) {
        const environment = process.env.REACT_APP_SENTRY_ENV
        Sentry.init({
            dsn: dsn,
            environment,
            integrations: [Sentry.browserTracingIntegration()],

            // Set tracesSampleRate to 1.0 to capture 100%
            // of transactions for performance monitoring.
            // We recommend adjusting this value in production
            tracesSampleRate: 1.0
        })
        console.info(`Loaded sentry config for dsn ${dsn} and environment ${environment}.`)
    } else {
        console.info("Failed to load Sentry config as dsn was not provided.")
    }
}

ReactDOM.render(
    <Provider store={store}>
        <App/>
    </Provider>,
    document.getElementById('app'))
