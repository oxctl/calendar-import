const PREFIX = "MESSAGES:"

export const ADD = PREFIX+ "ADD"
export const DELETE = PREFIX+ "DELETE"

export const addMessage = (message) => {
    return {
        type: ADD,
        value: message
    }
}

export const deleteMessage = (index) => {
    return {
        type: DELETE,
        value: index
    }
}