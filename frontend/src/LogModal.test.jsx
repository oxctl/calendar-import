import React from 'react'
import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '@testing-library/jest-dom'
import LogModal from './LogModal'

beforeEach(() => {
  vi.spyOn(window, 'scroll').mockImplementation(() => {})
})
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

test('loads authenticated log content as plain text without opening a window', async () => {
  let resolveFetch
  const fetchMock = vi.fn(() => new Promise(resolve => { resolveFetch = resolve }))
  vi.stubGlobal('fetch', fetchMock)
  const openWindow = vi.spyOn(window, 'open').mockImplementation(() => {})
  const onDismiss = vi.fn()
  render(<LogModal title='Load logfile' url='/api/log/1/load' token='test-token' onDismiss={onDismiss}/>)

  expect(await screen.findByRole('dialog', {name: 'Load logfile'})).toBeInTheDocument()
  expect(screen.getByText('Loading logfile')).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledWith('/api/log/1/load', {
    method: 'GET',
    headers: {Authorization: 'Bearer test-token'},
    signal: expect.any(AbortSignal)
  })

  const content = 'First line\n<script>alert("not HTML")</script>\nLast line'
  await act(async () => resolveFetch(new Response(content)))
  const log = screen.getByText(/First line/)
  expect(log.tagName).toBe('PRE')
  expect(log.textContent).toBe(content)
  expect(log.querySelector('script')).toBeNull()
  expect(screen.queryByText('Loading logfile')).not.toBeInTheDocument()
  expect(openWindow).not.toHaveBeenCalled()
  openWindow.mockRestore()

  await userEvent.setup().click(screen.getByRole('button', {name: 'Close'}))
  expect(onDismiss).toHaveBeenCalledTimes(1)
})

test('displays an empty log message', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('')))
  render(<LogModal url='/api/log/1/load' token='test-token' onDismiss={vi.fn()}/>)

  expect(await screen.findByText('No log content available.')).toBeInTheDocument()
})

test.each([401, 403, 404, 500])('displays HTTP %s errors inside the modal', async status => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', {status})))
  render(<LogModal url='/api/log/1/load' token='test-token' onDismiss={vi.fn()}/>)

  expect(await screen.findByText(`Failed to display log: Failed to fetch file with status ${status}`)).toBeInTheDocument()
  expect(screen.queryByText('No log content available.')).not.toBeInTheDocument()
  expect(screen.queryByText('Loading logfile')).not.toBeInTheDocument()
})

test('displays network failures inside the modal', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
  render(<LogModal url='/api/log/1/load' token='test-token' onDismiss={vi.fn()}/>)

  expect(await screen.findByText('Failed to display log: Failed to fetch')).toBeInTheDocument()
})

test('cancels the pending request when the viewer unmounts', async () => {
  const fetchMock = vi.fn().mockImplementation(() => new Promise(() => {}))
  vi.stubGlobal('fetch', fetchMock)
  const {unmount} = render(<LogModal url='/api/log/1/load' token='test-token' onDismiss={vi.fn()}/>)
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
  const signal = fetchMock.mock.calls[0][1].signal

  expect(signal.aborted).toBe(false)
  unmount()
  expect(signal.aborted).toBe(true)
})

test('ignores an old response after a different log is requested', async () => {
  let resolveOldFetch
  const fetchMock = vi.fn()
    .mockImplementationOnce(() => new Promise(resolve => { resolveOldFetch = resolve }))
    .mockResolvedValueOnce(new Response('New log'))
  vi.stubGlobal('fetch', fetchMock)
  const {rerender} = render(<LogModal url='/api/log/1/load' token='test-token' onDismiss={vi.fn()}/>)

  rerender(<LogModal url='/api/log/2/load' token='test-token' onDismiss={vi.fn()}/>)
  expect(await screen.findByText('New log')).toBeInTheDocument()
  expect(fetchMock.mock.calls[0][1].signal.aborted).toBe(true)

  await act(async () => resolveOldFetch(new Response('Old log')))
  expect(screen.getByText('New log')).toBeInTheDocument()
  expect(screen.queryByText('Old log')).not.toBeInTheDocument()
})
