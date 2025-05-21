/* Tool specific config. */
import 'dotenv/config.js'

export const token = import.meta.env.OAUTH_TOKEN
export const host = import.meta.env.CANVAS_HOST
export const toolId = import.meta.env.TOOL_ID
export const courseId = 186641
export const networkPreset = import.meta.env.NETWORK_PRESET
export const toolAnchorMethod = 'getByText'
export const toolAnchorText = 'Section'
export const toolAnchorOptions = { exact: true }
export const toolUrl = `${host}/courses/${courseId}/external_tools/${toolId}?launch_type=course_home_sub_navigation`