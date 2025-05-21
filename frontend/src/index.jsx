import React from 'react'
import { createRoot } from 'react-dom/client'
import {Provider} from 'react-redux'
import * as Sentry from "@sentry/react";
import App from './App'
import store from './store'

{
    // Load sentry setup if defined.
    // This is done early in the application to catch as much as possible.
    const dsn = import.meta.env.VITE_SENTRY_DSN
    if (dsn) {
        const environment = import.meta.env.VITE_SENTRY_ENV
        Sentry.init({
            dsn: dsn,
            environment,
            integrations: [Sentry.browserTracingIntegration()],

            // Set tracesSampleRate to 1.0 to capture 100%
            // of transactions for performance monitoring.
            // We recommend adjusting this value in production
            tracesSampleRate: 1.0
        })
    } else {
        console.info("Failed to load Sentry config as dsn was not provided.")
    }
}

const container = document.getElementById('app')
const root = createRoot(container)
root.render(<Provider store={store}><App /></Provider>);
