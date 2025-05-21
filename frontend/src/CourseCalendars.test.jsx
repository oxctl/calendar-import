import React from 'react'
import {screen} from '@testing-library/react'
import {http, HttpResponse} from 'msw'
import {setupServer} from 'msw/node'
import '@testing-library/jest-dom'
import {renderWithProviders} from "./utils/test-utils"
import {describe, expect, test, beforeAll, afterAll, afterEach, vi} from 'vitest'
import CourseCalendars from "./ContextCalendars"

const server = setupServer(
  http.get('*/api/imports', () => {
    return HttpResponse.json({
      content: [],
      totalPages: 1,
      totalElements: 0,
      size: 0,
      number: 0
    })
  }),
  http.get('*/api/v1/courses/1/sections', () => {
    return HttpResponse.json([])
  })
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

describe("course calendars", () => {
    test('renders no imports when no data exists', async () => {
        renderWithProviders(
            <CourseCalendars
                token='token'
                contextType='course'
                courseName='Course Name'
                courseId={1}
                proxyServer=''
                calendarServer=''
                canvasBaseUrl='https://canvas.instructure.com'
            />,
          {
            preloadedState: {
              lti: {server: ''}
            }
          }
        )
        const formSubmit = vi.fn()
        await screen.findByRole('heading', {name: 'Import File'})
        await screen.findByRole('heading', {name: 'No previous imports found'})
    })

  test('renders import details when data exists', async () => {
      server.use(
          http.get('*/api/imports', () => {
              return HttpResponse.json({
                  content: [{
                      id: 1,
                      calendarImport: {
                          user: { name: 'Test User' },
                          filename: 'test.csv',
                          destinationName: 'Test Section',
                          created: '2024-01-01T12:00:00Z',
                          type: 'CSV',
                          load: { status: 'COMPLETED', lastMessage: 'Success' }
                      }
                  }],
                  totalPages: 1,
                  totalElements: 1,
                  size: 10,
                  number: 0
              })
          })
      )

      renderWithProviders(
          <CourseCalendars
              token='token'
              contextType='course'
              courseName='Course Name'
              courseId={1}
              proxyServer=''
              calendarServer='http://test-server'
              canvasBaseUrl='https://canvas.instructure.com'
          />,
          {
              preloadedState: {
                  lti: {server: ''}
              }
          }
      )

      expect(await screen.findByText(/Test User imported/)).toBeInTheDocument()
      expect(await screen.findByText('test.csv')).toBeInTheDocument()
      expect(await screen.findByText(/into the section: Test Section/)).toBeInTheDocument()
      expect(await screen.findByText(/Created:/)).toBeInTheDocument()
  })
})