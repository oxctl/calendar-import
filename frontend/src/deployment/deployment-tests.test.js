import {afterEach, beforeAll, beforeEach} from '@jest/globals'
import adapter from 'axios/lib/adapters/http'
import puppeteer from 'puppeteer'
const axios = require('axios').default
const {expect, test, describe} = require('@jest/globals')
const dotenv = require('dotenv')

const options = {
  args: [
    // these are needed for cross frame access as LTI tools are in an iFrame
    '--disable-web-security',
    '--disable-features=IsolateOrigins,site-per-process'
  ],
}

dotenv.config()

const token = process.env.OAUTH_TOKEN
const host = process.env.CANVAS_HOST
const courseId = process.env.COURSE_ID
const toolId = process.env.TOOL_ID

jest.setTimeout(300000)

describe('Test that the correct data is present.', () => {
  let browser

  beforeAll(async () => {
    browser = await puppeteer.launch(options)
    // TODO: we probably need something to test whether the canvas environment is down for maintenance
    await login()

    const page = (await browser.pages())[0]
    const frame = await getFrame(page)

    await frame.waitForXPath('//*[contains(text(), "Loading sections...")]', {timeout: 30000})
    await frame.waitForXPath('//*[contains(text(), "Loading sections...")]', {hidden: true, timeout: 30000})

    const startElementHandle = await Promise.race([
      frame.waitForXPath('//*[contains(text(), "Please Grant Access")]', {timeout: 30000})
          .catch(),
      frame.waitForXPath('//*[contains(text(), "Import File")]', {timeout: 30000})
          .catch(),
    ])

    const pageStartText = await startElementHandle.evaluate(e => e.textContent)

    if(pageStartText === 'Please Grant Access') {
      const button = await frame.waitForXPath('//*[@id="app"]/div/div/button')
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

    await page.goto(`${host}/courses/${courseId}/external_tools/${toolId}?launch_type=course_home_sub_navigation`)

    const elementHandle = await page.$('div.tool_content_wrapper iframe')
    return await elementHandle.contentFrame()
  }

  test('Test that Import Complete data is present.', async () => {
    const page = (await browser.pages())[0]
    const frame = await getFrame(page)

    const importEl = await frame.waitForXPath("//*[@id=\"app\"]/div/div/div[3]/div[1]/div/div[1]")
    const importText = await importEl.evaluate(e => e.textContent)
    const loadTitleEl = await frame.waitForXPath("//*[@id=\"app\"]/div/div/div[3]/div[2]/span/div[1]/div[1]")
    const loadTitleText = await loadTitleEl.evaluate(e => e.textContent)
    const loadMessageEl = await frame.waitForXPath("//*[@id=\"app\"]/div/div/div[3]/div[2]/span/div[1]/div[2]")
    const loadMessageText = await loadMessageEl.evaluate(e => e.textContent)

    await expect(importText).toBe("Deployment Tester imported example.csv into the course")
    await expect(loadTitleText).toBe("Import: Completed")
    await expect(loadMessageText).toBe("Last message: Completed import, found 2 events, imported 2 events into calendar.")
  })
})