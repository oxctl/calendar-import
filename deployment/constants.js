/* Tool specific config. */
import 'dotenv/config.js'

export const token = process.env.OAUTH_TOKEN
export const host = process.env.CANVAS_HOST
export const toolId = process.env.TOOL_ID
export const courseId = 186641
export const networkPreset = process.env.NETWORK_PRESET
export const toolAnchorMethod = 'getByText'
export const toolAnchorText = 'Import File'
export const toolAnchorOptions = { exact: true }
export const toolUrl = `${host}/courses/${courseId}/external_tools/${toolId}?launch_type=course_home_sub_navigation`