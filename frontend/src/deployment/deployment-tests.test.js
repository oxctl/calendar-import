import {afterEach, beforeAll, beforeEach} from '@jest/globals'
import adapter from 'axios/lib/adapters/http'
const puppeteer = require('puppeteer')
const axios = require('axios').default
const {expect, test, describe} = require('@jest/globals')
const dotenv = require('dotenv')

const options = {
  args: [
    '--disable-web-security',
    '--disable-features=IsolateOrigins,site-per-process'
  ],
}

dotenv.config()

const token = process.env.OAUTH_TOKEN
const host = process.env.CANVAS_HOST

jest.setTimeout(1200000)

describe('Test that the correct data is present.', () => {
  let browser

  beforeAll(async () => {
    browser = await puppeteer.launch(options)
    // TODO: we probably need something to test whether the canvas environment is down for maintenance
    await login()

    const page = (await browser.pages())[0]
    const frame = await getFrame(page)

    // Need to wait here - 'Import File' appears briefly before the spinner so it will always be first.
    // TODO: This should probably be fixed
    await new Promise(r => setTimeout(r, 2000))

    const startElementHandle = await Promise.race([
      frame.waitForXPath('//*[contains(text(), "Please Grant Access")]', {timeout: 30000})
          .catch(),
      frame.waitForXPath('//*[contains(text(), "Import File")]', {timeout: 30000})
          .catch(),
    ])

    const pageStartText = await startElementHandle.evaluate(e => e.textContent)

    if(pageStartText === 'Please Grant Access') {
      // Can't find a "cleaner" way of locating the button, so this is dependent on the class not changing
      const button = await frame.waitForSelector('.css-z0pc6h-view-billboard')

      const pageTarget = page.target()
      await button.click()
      const newTarget = await browser.waitForTarget(target => target.opener() === pageTarget)
      const newPage = await newTarget.page()

      const submit = await newPage.waitForSelector("input[name='commit'")
      await submit.click()
      const close = await newPage.waitForSelector('a.Button')
      await close.click()
    }

    await browser.close()
  })

  beforeEach(async () => {
    browser = await puppeteer.launch(options)
    await login()
  })

  afterEach(async () => {
    await browser.close()
  })

  const login = async () => {
    const page = (await browser.pages())[0]
    await page.setDefaultNavigationTimeout(0)
    await page.setDefaultTimeout(90000)
    await Promise.all([
      page.waitForNavigation(),
      axios.get(`${host}/login/session_token`, {
        adapter: adapter,
        headers: {
          ContentType: 'application/json',
          Authorization: `Bearer ${token}`
        }
      }).then((response) => {
        return page.goto(response.data.session_url)
      })
    ])
  }

  const getFrame = async (page) => {
    await page.setDefaultNavigationTimeout(0)
    await page.setDefaultTimeout(90000)

    await page.goto(host + '/courses/39056/external_tools/38603?launch_type=course_home_sub_navigation')

    const elementHandle = await page.$('div.tool_content_wrapper iframe')
    return await elementHandle.contentFrame()
  }

  test('Test that Import Complete data is present.', async () => {
    const page = (await browser.pages())[0]
    const frame = await getFrame(page)

    // Need to wait here, possibly for spinner to disappear and data to load?
    // TODO: Find a better approach than sleep
    await new Promise(r => setTimeout(r, 30000))
    const content = await frame.content()

    await expect(content.match('Import File')).not.toBeNull()
    await expect(content.match('Previous Imports')).not.toBeNull()
    // TODO: this is placeholder data against course 39056 - change when live test data is set up
    await expect(content.match('Import: Completed')).not.toBeNull()
    await expect(content.match('Completed import, found 2 events, imported 2 events into calendar.')).not.toBeNull()
  })
})