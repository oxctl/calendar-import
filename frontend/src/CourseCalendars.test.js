import React from 'react'
import {screen} from '@testing-library/react'
import {rest} from 'msw'
import {setupServer} from 'msw/node'
import '@testing-library/jest-dom/extend-expect'
import {renderWithProviders} from "./utils/test-utils";
import {jest, test} from '@jest/globals';
import CourseCalendars from "./CourseCalendars";

const server = setupServer(
    // This is the endpoint that takes the deep linking request and signs
    // a JWT with it.
    rest.get('/api/imports', (req, res, ctx) => {
        return res(ctx.json([]));
    }),
    rest.get('/api/v1/courses/1/sections', (req, res, ctx) => {
        return res(ctx.json([]))
    })
)


beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("course calendars", () => {
    test('first render', async () => {
        renderWithProviders(
            <CourseCalendars
                token='token'
                courseName='Course Name'
                courseId={1}
                servers={{proxyServer: '', calendarServer: ''}}
                canvasBaseUrl='https://canvas.instructure.com'
            />
        ,{preloadedState: {lti: {server: ''}}})
        const formSubmit = jest.fn()
        screen.findByTitle('Import File')
        // We don't have any imports so nothing to see.
        screen.findByTitle('No previous imports found')
    })
})