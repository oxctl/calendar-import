import { test, expect } from '@playwright/test'
import { dismissBetaBanner, getLtiIFrame, waitForNoSpinners, TEST_URL, grantAccessIfNeeded } from '@oxctl/deployment-test-utils'

test.describe('Test deployment', () => {
  test('The tool should load and either import or no imports should be shown', async ({context, page}) => {
    await page.goto(TEST_URL)
    await dismissBetaBanner(page)
    const ltiIFrame = getLtiIFrame(page)
    await waitForNoSpinners(ltiIFrame)

    // check for page content
    const imports = ltiIFrame.getByText(/.* imported .* into the/)
    const noImports = ltiIFrame.getByText("No previous imports found")
    await expect(imports.first().or(noImports)).toBeVisible();
  })
})