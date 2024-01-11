import {DEV, PROD} from "./constants";

const environments = {
    [DEV]: {
        'sentryDsn': 'https://ee57976e480d458d82b46b51133fd4be@o419652.ingest.sentry.io/5861164',
        'sentryEnv': 'dev'
    },
    [PROD]: {
        'sentryDsn':'https://d9acebae68d143d4a9de99ee1b9fd53d@o419652.ingest.sentry.io/5861164',
        'sentryEnv': 'prod'
    }
}

export const settings = environments[window.location.origin]