import {DEV, LOCAL, PROD} from "./constants";

const environments = {
    [LOCAL]: {
        'ltiServer': process.env.REACT_APP_SERVER_LTI,
        'proxyServer': process.env.REACT_APP_SERVER_PROXY,
        'calendarServer': process.env.REACT_APP_SERVER_CALENDAR
    },
    [DEV]: {
        'ltiServer': 'https://lti-dev.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy-dev.canvas.ox.ac.uk',
        'calendarServer': 'https://calendar-import-dev.canvas.ox.ac.uk',
        'sentryDsn': 'https://ee57976e480d458d82b46b51133fd4be@o419652.ingest.sentry.io/5861164',
        'sentryEnv': 'dev'
    },
    [PROD]: {
        'ltiServer': 'https://lti.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy.canvas.ox.ac.uk',
        'calendarServer': 'https://calendar-import.canvas.ox.ac.uk',
        'sentryDsn':'https://d9acebae68d143d4a9de99ee1b9fd53d@o419652.ingest.sentry.io/5861164',
        'sentryEnv': 'prod'
    }
}

export const settings = environments[window.location.origin]