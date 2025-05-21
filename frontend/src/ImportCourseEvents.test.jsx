import React from 'react'
import {screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {http, HttpResponse} from 'msw'
import {setupServer} from 'msw/node'
import ImportCourseEvents from "./ImportCourseEvents";
import {renderWithProviders} from "./utils/test-utils";
import {describe, expect, test, beforeAll, afterAll, afterEach, vi} from 'vitest'
import '@testing-library/jest-dom'

const server = setupServer(
    // This is the endpoint that takes the deep linking request and signs
    // a JWT with it.
    http.post('/deep-linking', () => {
        return HttpResponse.json({jwt: 'JWT'})
    })
)


beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("setup of calendar link", () => {
    test('successful walk through', async () => {
        renderWithProviders(
            <ImportCourseEvents
                token='token'
                courseName='Course Name'
                deepLinkReturnUrl='/return-url'
                ltiServer=''
                targetLinkUri='http://server.test/target-link'
                timezone='Europe/London'
            />
        )
        // Testing Library encourages using either the text in the page or explicit testing IDs
        // We can't handle page navigation so we have to wait for the submit handler
        const formSubmit = vi.fn()
        const form = screen.getByTestId('deepLinkingForm')
        form.submit = formSubmit

        const user = userEvent.setup()

        const heading = await screen.findByRole('heading')
        expect(heading).toHaveTextContent('Import Course Events')

        const inputUrl = screen.getByLabelText('Server URL')
        await user.type(inputUrl, 'http://server.test/calendar-url.ics')
        await waitFor(() => expect(inputUrl).toHaveValue('http://server.test/calendar-url.ics'))
        const pageName = screen.getByLabelText('Page name');
        await user.type(pageName, 'Import Calendar')

        const button = screen.getByRole('button', {name: 'Add'})
        await user.click(button)

        // We can't check for navigation, so check form submitted. This is
        // implementation detail, but we don't have anything better.
        await waitFor(() => expect(formSubmit).toHaveBeenCalled())
    })

    test('not entering URL gives error message', async () => {
        renderWithProviders(
            <ImportCourseEvents
                token='token'
                courseName='Course Name'
                deepLinkReturnUrl='/return-url'
                ltiServer=''
                targetLinkUri='http://server.test/target-link'
                timezone='Europe/London'
            />
        )

        // We can't handle page navigation so we have to wait for the submit handler
        const formSubmit = vi.fn()
        const form = screen.getByTestId('deepLinkingForm')
        form.submit = formSubmit

        const user = userEvent.setup()
        const heading = await screen.findByRole('heading')
        expect(heading).toHaveTextContent('Import Course Events')

        // We haven't entered a URL at this point so we will get an error
        const button = screen.getByRole('button', {name: 'Add'})
        await user.click(button)

        expect(screen.getByText("You must provide a URL and Page name")).toBeDefined()
    })

    test('display message when deep linking errors.', async () => {
        renderWithProviders(
            <ImportCourseEvents
                token='token'
                courseName='Course Name'
                deepLinkReturnUrl='/return-url'
                ltiServer=''
                targetLinkUri='http://server.test/target-link'
                timezone='Europe/London'
            />
        )
        server.use(
            http.post('/deep-linking', () => {
                return new HttpResponse(null, { status: 500 })
            })
        )

        // We can't handle page navigation so we have to wait for the submit handler
        const formSubmit = vi.fn()
        const form = screen.getByTestId('deepLinkingForm')
        form.submit = formSubmit

        const user = userEvent.setup()

        const inputUrl = screen.getByLabelText('Server URL')
        await user.type(inputUrl, 'http://server.test/calendar-url.ics')
        await waitFor(() => expect(inputUrl).toHaveValue('http://server.test/calendar-url.ics'))
        const pageName = screen.getByLabelText('Page name');
        await user.type(pageName, 'Import Calendar')

        // We haven't entered a URL at this point so we will get an error
        const button = screen.getByRole('button', {name: 'Add'})
        await user.click(button)

        // We should really have a better way to get the errors.
        await waitFor(() => expect(screen.getByText("Failed to add, status: Error: 500")).toBeDefined())
    })
})