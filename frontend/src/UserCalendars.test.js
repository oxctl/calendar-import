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
    }),
    rest.post('/proxy/api/v1/calendar_events/save_enabled_account_calendars', (req, res, ctx) => {
        return res(ctx.json({ok: "ok"}))
    }),
    rest.post('/proxy/api/v1/calendar_events/save_selected_contexts', (req, res, ctx) => {
        return res(ctx.json({ok: "ok"}))
    }),
    rest.get('/proxy/api/v1/account_calendars', (req, res, ctx) => {
        return res(ctx.json(
          {account_calendars:[{id:"140",name:"2B Medical Sciences",parent_account_id:"1",root_account_id:"1",visible:true,asset_string:"account_140",type:"account",calendar_event_url:"/accounts/140/calendar_events/%7B%7B%20id%20%7D%7D",can_create_calendar_events:true,create_calendar_event_url:"/accounts/140/calendar_events",new_calendar_event_url:"/accounts/140/calendar_events/new"},{id:"1",name:"Oxford Evaluation",parent_account_id:null,root_account_id:"0",visible:true,asset_string:"account_1",type:"account",calendar_event_url:"/accounts/1/calendar_events/%7B%7B%20id%20%7D%7D",can_create_calendar_events:true,create_calendar_event_url:"/accounts/1/calendar_events",new_calendar_event_url:"/accounts/1/calendar_events/new"}],total_results:2}
        ))
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
                onMissingToken={() => {}}
                disableCalendarImport={false}
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
                onMissingToken={() => {}}
                disableCalendarImport={false}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await screen.findByText(/network request failed/i)
    })
    
    test('displays an error when predefined calendar fails to load', async () => {
        server.use(rest.get('/calendar/api/predefined', (req, res) => {
            return res(cts.status(500))
        }))
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                onMissingToken={() => {}}
                disableCalendarImport={false}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await screen.findByText(/network request failed/i)
    })


    test('displays an error when predefined CSV fails to load', async () => {
        server.use(rest.get('/calendar/api/predefined/2000-filename.csv', (req, res, ctx) => {
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
                onMissingToken={() => {}}
                disableCalendarImport={false}
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

        await screen.findByText(/network request failed/i)
    })


    test('displays an error when starting the job fails', async () => {
        server.use(rest.post('/calendar/api/run', (req, res, ctx) => {
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
                onMissingToken={() => {}}
                disableCalendarImport={false}
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

        await screen.findByText(/network request failed/i)
        expect(saveButton).not.toBeDisabled()
    })

    test('displays an error when polling fails', async () => {
        server.use(
            rest.get('/calendar/api/imports/1', (req, res, ctx) => {
                return res(ctx.status(401))
            })
        )
        render(
            <UserCalendars
                token='token'
                returnUrl='/return-url'
                proxyServer='/proxy'
                calendarServer='/calendar'
                canvasId='canvasId'
                onMissingToken={() => {}}
                disableCalendarImport={false}
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

        await screen.findByText(/session has timed out, please relaunch the tool/i)
    })


    test('Calendar Import cannot be enabled if it is disabled', async () => {
        server.use(rest.get('/calendar/api/predefined/2000-filename.csv', (req, res, ctx) => {
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
                onMissingToken={() => {}}
                disableCalendarImport={true}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await waitForElementToBeRemoved(() => screen.queryByTitle(/loading calendars/i))

        const currentCalendar = screen.getByRole("checkbox", {name: /2000 Calendar/})
        expect(currentCalendar).toBeDisabled()

        const nextCalendar = screen.getByRole("checkbox", {name: /2001 Calendar/})
        expect(nextCalendar).toBeDisabled()

        const saveButton = screen.getByRole("button", {name: /save/i })
        expect(saveButton).toBeDisabled()
    })


    test('Disabling calendar import shows an alert', async () => {
        server.use(rest.get('/calendar/api/predefined/2000-filename.csv', (req, res, ctx) => {
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
                onMissingToken={() => {}}
                disableCalendarImport={true}
                date={() => new Date('2000-06-01')}
            />
        )

        await screen.findByRole('heading', {name: /University Terms/})
        await waitForElementToBeRemoved(() => screen.queryByTitle(/loading calendars/i))

        // It's checking the text of the instructions.
        const alertItem = screen.getByText(/If you have previously imported/i)
        expect(alertItem).toBeDefined()

    })

})