const { test } = require('@playwright/test')
const assert = require('assert')
require('dotenv').config()

const token = process.env.OAUTH_TOKEN
const host = process.env.CANVAS_HOST
const courseId = process.env.COURSE_ID
const toolId = process.env.TOOL_ID

test.beforeAll(async () => {
  assert(token, 'You must set the environmental variable OAUTH_TOKEN')
  assert(host, 'You must set the environmental variable CANVAS_HOST')
  assert(courseId, 'You must set the environmental variable COURSE_ID')
  assert(toolId, 'You must set the environmental variable TOOL_ID')
})

const login = async (request, page) => {
  await Promise.all([
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
  ])
}

const grantAccess = async (context, frameLocator) => {
  const button = await frameLocator.getByRole('button')
  const [newPage] = await Promise.all([
    context.waitForEvent('page'),
    await button.click()
  ])

  const submit = await newPage.getByRole('button', {name: /Authori[sz]e/})
  await submit.click()
  const close = await newPage.getByText('Close', {exact: true})
  await close.click()
}

test('Calendar import loads and shows the previous imports or that there are none.', async ({ context, page, request }) => {
  await login(request, page)
  await page.goto(`${host}/courses/${courseId}/external_tools/${toolId}?launch_type=course_home_sub_navigation`)
  // the frame id now seems to be dynamic on beta, but fixed on live - this should allow it to work for both cases
  const frames = page.frames();
  const frameLocator = frames.find((frame) => frame.name().includes('tool_content'));

  const needsGrantAccess = await Promise.race([
    frameLocator.getByText('Please Grant Access').waitFor()
        .then(() => { return true } ),
    frameLocator.getByLabel('Section', {exact: true}).waitFor()
        .then(() => { return false } )
  ])

  if(needsGrantAccess){
    await grantAccess(context, frameLocator)
  }

  // We could shortcut this if we see an error message, but I think it's reasonable to just wait on the timeout
  // instead of complicating the code.
  
  // Wait until we either load an import or find that there's no imports in the course.
  // These requests depend on the backend so tests both the backend and the frontend availability.
  await Promise.race([
      frameLocator.getByText(/.* imported .* into the/).first().waitFor(),
      frameLocator.getByText("No previous imports found").waitFor(),
  ])
})