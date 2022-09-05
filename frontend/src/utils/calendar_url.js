const isValidUrl = (url) => {
    try {
        new URL(url);
    } catch (e) {
        return false;
    }
    return true;
}

const isMagicUrl = (url) => {
    return url.startsWith('calendar://')
}

const VALID_VARIABLE_REGEX = new RegExp(/(?<=\${).+?(?=})/, 'g')
const VALID_VARIABLES = ['course.id', 'user.sis_id']

const hasValidVariables = (url) => {
    const matches = url.match(VALID_VARIABLE_REGEX)
    if(!matches) return true
    return matches.every(el => VALID_VARIABLES.includes(el))
}

const validVariables = () => {
    return [...VALID_VARIABLES]
}

export {isValidUrl, isMagicUrl, hasValidVariables, validVariables}