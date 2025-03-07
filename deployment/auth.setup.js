import { test as setup } from '@playwright/test'
import path from 'path'

import { goToTool, getToolAnchor } from './test-utils'
import { token, host } from './constants'

const authFile = path.join(__dirname, './user.json')


/* This is a setup function that will run before any tests in this file. This should stay the same across projects. */
setup('authenticate', async ({ context, page, request }) => {
  await login(request, page)
  await grantAccessIfNeeded(page, context)

  await page.context().storageState({ path: authFile })
})

const grantAccessIfNeeded = async(page, context) => {
  const frameLocator = await goToTool(page)

  // this is something static on the page that will only show if authentication has happened
  const toolAnchor = await getToolAnchor(frameLocator)

  const needsGrantAccess = await Promise.race([
    frameLocator.getByText('Please Grant Access').waitFor()
      .then(() => { return true } ),
    toolAnchor.waitFor()
      .then(() => { return false } )
  ])

  if(needsGrantAccess){
    await grantAccess(context, frameLocator)
  }
}

const login = async (request, page) => {
  await Promise.resolve(
    await request.get(`${host}/login/session_token`, {
      headers: {
        ContentType: 'application/json',
        Authorization: `Bearer ${token}`
      }
    }).then(async (response) => {
      const json = await response.json()
      const sessionUrl = json.session_url
      return page.goto(sessionUrl)
    })
  )
}

const grantAccess = async (context, frameLocator) => {
  const button = await frameLocator.getByRole('button')
  const [newPage] = await Promise.all([
    context.waitForEvent('page'),
    button.click()
  ])

  const submit = await newPage.getByRole('button', {name: /Authori[sz]e/})
  await submit.click()
  const close = await newPage.getByText('Close', {exact: true})
  await close.click()
}