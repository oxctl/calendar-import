const puppeteer = require('puppeteer')
const axios = require('axios');
// const {test} = require("@jest/globals");

const options = {
  // executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  headless: false,
  defaultViewport: null,
  args: [
    '--window-size=1920,1080',
    // '--incognito',
    '--disable-web-security',
    '--disable-features=IsolateOrigins,site-per-process'],
};

const host = "https://universityofoxford.beta.instructure.com";
const token = "";

// jest.setTimeout(1200000);

test("testing", async () => {
  const browser = await puppeteer.launch(options)
  let pages = await browser.pages()
  const page = pages[0]
  await page.setDefaultNavigationTimeout(0)
  await page.setDefaultTimeout(90000);
  await Promise.all([
    page.waitForNavigation(),
    axios.get(`${host}/login/session_token`, {headers: {'Authorization': 'Bearer ' + token}})
        .then((response) => {
          return page.goto(response.data.session_url);
        })
  ]);

  await page.goto(host + '/courses/39056/external_tools/38603?launch_type=course_home_sub_navigation')

  await page.evaluate(() => {})

  await page.waitForSelector("[data-tool-id='lti-dev.canvas.ox.ac.uk']");

  const elementHandle = await page.$('div.tool_content_wrapper iframe');
  const frame = await elementHandle.contentFrame();
  const button = await frame.waitForSelector(".css-z0pc6h-view-billboard")

  const pageTarget = page.target();
  await button.click()
  const newTarget = await browser.waitForTarget(target => target.opener() === pageTarget);
  const newPage = await newTarget.page();

  // await new Promise(r => setTimeout(r, 2000));

  const submit = await newPage.waitForSelector("input[name='commit");
  await submit.click()
  const close = await newPage.waitForSelector("a.Button")
  await close.click()

  await new Promise(r => setTimeout(r, 2000));

  const content = await page.content()
  await expect(content.match('Previous Imports')).not.toBeNull()
  await expect(content.match("Ana-Diamond Aaba Atach")).not.toBeNull()

  await page.screenshot({
    path: 'screenshot.jpg',
    fullPage: true
  });

  await browser.close()
})