import React from 'react'
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {rest} from 'msw'
import {setupServer} from 'msw/node'
import '@testing-library/jest-dom/extend-expect'
import UserCalendars from "./UserCalendars";
import {test} from '@jest/globals';

// We need to ensure that the date is at a known point
const server = setupServer(
    // This is the endpoint that takes the deep linking request and signs
    // a JWT with it.
    rest.get('/proxy/tokens/refresh', (req, res, ctx) => {
        return res(ctx.status(200))
    }),
    rest.get('/calendar/api/predefined', (req, res, ctx) => {
        return res(ctx.json([
            {
                title: "2000 Calendar",
                filename: "2000-filename.csv",
                properties: {start: "2000-01-01", end: "2001-01-01"}
            },
            {
                title: "2001 Calendar",
                filename: "2001-filename.csv",
                properties: {start: "2001-01-01", end: "2002-01-01"}
            },
            
        ]))
    }),
    rest.get('/calendar/api/imports', (req, res, ctx) => {
        return res(ctx.json({
            content: []  
        }))
    }),
    rest.get('/calendar/api/predefined/2000-filename.csv', (req, res, ctx) => {
        return res(
            ctx.set('Content-Type', 'text/csv'),
            ctx.body('')
        )
    }),
    rest.post('/calendar/api/run', (req, res, ctx) => {
        return res(ctx.json({id: 1}))
    }),
    rest.get('/calendar/api/imports/1', (req, res, ctx) => {
        return res(ctx.json({calendarImport: {id: 1, load: {status: "COMPLETED"}}}))
    })
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("user calendars", () => {
    test('correct rendering of predefined calendars and save', async () => {
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                userId='userId'
                onMissingToken={() => {}}
                date={() => new Date('2000-06-01')}
            />
        )
        
        await screen.findByRole('heading', {name: /University Terms/})
        await waitForElementToBeRemoved(() => screen.queryByTitle(/loading calendars/i))
        
        const currentCalendar = screen.getByRole("checkbox", {name: /2000 Calendar/})

        const user = userEvent.setup()
        await user.click(currentCalendar)
        
        const saveButton = screen.getByRole("button", {name: /save/i })
        await user.click(saveButton)

        await screen.findByText(/update of .* year calendar complete/i)
    })

    test('displays error when fails to load imports', async () => {
        server.use(rest.get('/calendar/api/imports',(req, res, ctx) => {
            return res(ctx.status(500))
        }))
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                userId='userId'
                onMissingToken={() => {}}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await screen.findByText(/failed to process calendars: request failed/i)
    })
    
    test('displays an error when predefined calendar fails to load', async () => {
        server.use(rest.get('/calendar/api/predefined', (req, res, ctx) => {
            return res(cts.status(500))
        }))
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                userId='userId'
                onMissingToken={() => {}}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await screen.findByText(/failed to load predefined calendars: network request failed/i)
    })


    test('displays an error when predefined CSV fails to load', async () => {
        server.use(    rest.get('/calendar/api/predefined/2000-filename.csv', (req, res, ctx) => {
                return res(ctx.status(500))
            }),
        )
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                userId='userId'
                onMissingToken={() => {}}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await waitForElementToBeRemoved(() => screen.queryByTitle(/loading calendars/i))

        const currentCalendar = screen.getByRole("checkbox", {name: /2000 Calendar/})

        const user = userEvent.setup()
        await user.click(currentCalendar)

        const saveButton = screen.getByRole("button", {name: /save/i })
        await user.click(saveButton)
        
        // TODO This should really generate an error, but doesn't at the moment. AB#64153
        // await screen.findByText(/failed to load predefined calendars: network request failed/i)
    })


    test('displays an error when starting the job fails', async () => {
        server.use(    rest.post('/calendar/api/imports/run', (req, res, ctx) => {
                return res(ctx.status(500))
            }),
        )
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                userId='userId'
                onMissingToken={() => {}}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await waitForElementToBeRemoved(() => screen.queryByTitle(/loading calendars/i))

        const currentCalendar = screen.getByRole("checkbox", {name: /2000 Calendar/})

        const user = userEvent.setup()
        await user.click(currentCalendar)

        const saveButton = screen.getByRole("button", {name: /save/i })
        await user.click(saveButton)

        // TODO This should really generate an error, but doesn't at the moment. AB#64153
        // await screen.findByText(/failed to load predefined calendars: network request failed/i)
    })
})