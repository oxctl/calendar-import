const { test } = require('@playwright/test');
require('dotenv').config()

const token = process.env.OAUTH_TOKEN
const host = process.env.CANVAS_HOST
const courseId = process.env.COURSE_ID
const toolId = process.env.TOOL_ID

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

test('Test that Import Complete data is present.', async ({ context, page, request }) => {
  await login(request, page)
  await page.goto(`${host}/courses/${courseId}/external_tools/${toolId}?launch_type=course_home_sub_navigation`)
  const frameLocator = await page.frameLocator('#tool_content')

  const needsGrantAccess = await Promise.race([
    frameLocator.getByText('Please Grant Access').waitFor()
        .then(() => { return true } ),
    frameLocator.getByLabel('Section', {exact: true}).waitFor()
        .then(() => { return false } )
  ])

  if(needsGrantAccess){
    await grantAccess(context, frameLocator)
  }

  await frameLocator.getByText('Deployment Tester imported example.csv into the course').first().waitFor()
  await frameLocator.getByText('Import: Completed').first().waitFor()
  await frameLocator.getByText('Last message: Completed import, found 2 events, imported 2 events into calendar.').first().waitFor()
})