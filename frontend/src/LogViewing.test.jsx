import React from 'react'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '@testing-library/jest-dom'
import AuthoriseCalendarEvents from './AuthoriseCalendarEvents'
import ImportView from './ImportView'
import { renderWithProviders } from './utils/test-utils'

const calendarServer = 'http://calendar.test'
beforeEach(() => {
  vi.spyOn(window, 'scroll').mockImplementation(() => {})
  vi.stubGlobal('fetch', vi.fn(async url => {
    switch (new URL(url).pathname) {
      case '/api/imports':
        return Response.json({
          content: [{
            id: 42,
            calendarImport: {
              id: 7,
              user: {name: 'Test User'},
              filename: 'events.csv',
              created: '2026-09-01T12:00:00Z',
              type: 'CSV',
              load: {status: 'COMPLETED', lastMessage: 'Imported'},
              delete: {status: 'COMPLETED', lastMessage: 'Deleted'}
            }
          }],
          totalPages: 1
        })
      case '/api/getUserSubscription':
        return Response.json({
          id: 7,
          load: {status: 'COMPLETED', lastMessage: 'Imported'}
        })
      case '/api/log/42/load':
        return new Response('load log content')
      case '/api/log/42/delete':
        return new Response('delete log content')
      case '/api/log/7/loadByCalendarImportId':
        return new Response('Personal calendar log')
      default:
        throw new Error(`Unexpected request: ${url}`)
    }
  }))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

const logRequestOptions = {
  method: 'GET',
  headers: {Authorization: 'Bearer test-token'},
  signal: expect.any(AbortSignal)
}

test('previous imports opens load and delete logs in a modal and restores focus on close', async () => {
  renderWithProviders(
    <ImportView server={calendarServer} token='test-token' onMessage={vi.fn()}/>,
    {preloadedState: {lti: {server: calendarServer, token: 'test-token'}}}
  )
  const user = userEvent.setup()
  const links = await screen.findAllByRole('button', {name: 'logfile'})

  await user.click(links[0])
  let dialog = await screen.findByRole('dialog', {name: 'Load logfile'})
  expect(await within(dialog).findByText('load log content')).toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith(`${calendarServer}/api/log/42/load`, logRequestOptions)
  await user.click(within(dialog).getByRole('button', {name: 'Close'}))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  await waitFor(() => expect(links[0]).toHaveFocus())

  await user.click(links[1])
  dialog = await screen.findByRole('dialog', {name: 'Delete logfile'})
  expect(await within(dialog).findByText('delete log content')).toBeInTheDocument()
  expect(within(dialog).queryByText('load log content')).not.toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith(`${calendarServer}/api/log/42/delete`, logRequestOptions)
  await waitFor(() => expect(within(dialog).getByRole('button', {name: 'Close'})).toHaveFocus())
  // Instructure's focus manager uses the legacy keyCode property.
  fireEvent.keyUp(document.activeElement, {key: 'Escape', keyCode: 27})
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

  await user.click(links[0])
  expect(await screen.findByText('load log content')).toBeInTheDocument()
  expect(fetch.mock.calls.filter(([url]) => url === `${calendarServer}/api/log/42/load`)).toHaveLength(2)
})

test('personal calendar opens its log using the calendar import ID endpoint', async () => {
  renderWithProviders(
    <AuthoriseCalendarEvents calendarServer={calendarServer} token='test-token'
                             personalCalendarLink='https://canvas.test/calendar'/>
  )
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', {name: 'logfile'}))

  const dialog = await screen.findByRole('dialog', {name: 'Load logfile'})
  expect(await within(dialog).findByText('Personal calendar log')).toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith(`${calendarServer}/api/log/7/loadByCalendarImportId`, logRequestOptions)
  await user.click(within(dialog).getByRole('button', {name: 'Close'}))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
})
