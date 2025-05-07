import { test, expect } from '@playwright/test'
import { goToTool, addNetworkThrottle, NETWORK_PRESETS, screenshot, getToolAnchor } from './test-utils'
import { networkPreset } from './constants'
import path from 'path'
test('Calendar import deployment tests', async ({context, page}, testInfo) => {

  let ltiToolFrame

  await test.step('Load the tool', async () => {
    if (networkPreset) {
      await addNetworkThrottle(context, page, NETWORK_PRESETS[networkPreset])
    }
    ltiToolFrame = await goToTool(page)
    if (page.url().includes('beta')) {
      // the warning banner on beta interferes with some click operations so dismiss it
      await page.getByRole('button', {name: 'Close warning'}).click()
    }
  })

  await test.step('Tool loads', async () => {
    const title = await getToolAnchor(ltiToolFrame)
    await expect(title).toBeVisible()
  })

  await test.step('Shows imports or no imports', async () => {
    const imports = ltiToolFrame.getByText(/.* imported .* into the/)
    const noImports = ltiToolFrame.getByText("No previous imports found")
    await expect(imports.first().or(noImports)).toBeVisible();
  })


  await test.step('Course and section are selectable', async () => {
    const sectionCombo = ltiToolFrame.getByRole('combobox')
    await sectionCombo.click()
    const listOpts = ltiToolFrame.getByRole('listbox')
    await expect(listOpts).toContainText('Course: Deployment Testing')
    await expect(listOpts).toContainText('Section: Deployment Testing')

    const courseOpt = listOpts.getByRole('option', {name: 'Course: Deployment Testing'})
    await expect(courseOpt).toHaveCount(1)
    await expect(courseOpt).toBeEnabled()

    const sectionOpt = listOpts.getByRole('option', {name: 'Section: Deployment Testing'})
    await expect(sectionOpt).toHaveCount(1)
    await expect(sectionOpt).toBeEnabled()
  })

  let importButton
  await test.step('Import without file shows warning', async () => {
    importButton = ltiToolFrame.getByRole('button', {name: 'Import'})
    await importButton.click()
    await expect(ltiToolFrame.getByText('You must provide a file')).toBeVisible()
  })

  await test.step('Can upload a file and upload starts', async () => {
    const fileChooserPromise = page.waitForEvent('filechooser')
    await ltiToolFrame.getByText('browse your files').click()
    const fileChooser = await fileChooserPromise
    await fileChooser.setFiles(path.join(__dirname, 'resources', 'example.csv'))
    await expect(ltiToolFrame.getByText('Selected: example.csv')).toBeVisible()
    await importButton.click()

    await expect(ltiToolFrame.getByText('Calendar import started, click "Reload" button to follow its progress.')).toBeVisible();

    /* ideally we would test for Import Completed
    but complicated as there will be an audit of all previous imports an no date on Import: Completed
    We should be able to use the purge endpoint to clear out the previous imports */
    // however as we're moving away from this sort of in-depth testing in the deployment tests, we should just make sure it's covered by whatever replaces them
  })
})