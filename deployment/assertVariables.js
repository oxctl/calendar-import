import assert from 'assert'
import { host, token, toolId } from './constants'
import {test as assertVariables} from '@playwright/test'

/* The variables that are required to be set. */
assertVariables('check variables', async ({ context, page, request }) => {
  assert(token, 'You must set the environmental variable OAUTH_TOKEN')
  assert(host, 'You must set the environmental variable CANVAS_HOST')
  assert(toolId, 'You must set the environmental variable TOOL_ID')
})