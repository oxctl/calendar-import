const PREFIX = "LTI:"

export const SET_TOKEN = PREFIX + "SET_TOKEN"
export const SET_SERVER = PREFIX + "SET_SERVER"

export const setToken = (token) => {
    return {
        type: SET_TOKEN,
        value: token
    }
}

export const setServer = (server) => {
    return {
        type: SET_SERVER,
        value: server
    }
}

