import { test, expect } from '@playwright/test'
import { dismissBetaBanner, getLtiIFrame, waitForNoSpinners } from '@oxctl/deployment-test-utils'

const host = process.env.CANVAS_HOST
const url = process.env.URL

test.describe('Test deployment', () => {
  test('The tool should load and either import or no imports should be shown', async ({context, page}) => {
    await page.goto(`${host}/${url}`)
    await dismissBetaBanner(page)
    const ltiIFrame = getLtiIFrame(page)
    await waitForNoSpinners(ltiIFrame)
    const imports = ltiIFrame.getByText(/.* imported .* into the/)
    const noImports = ltiIFrame.getByText("No previous imports found")
    await expect(imports.first().or(noImports)).toBeVisible();
  })
})