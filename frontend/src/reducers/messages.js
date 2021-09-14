import {ADD, DELETE} from "../actions/messages";

const initialState = {
    // The list of messages to display
    data: []
}

export default (state = initialState, action) => {
    switch (action.type) {
        case ADD:
            return {
                ...state,
                data: [...state.data, action.value]
            }
        case DELETE:
            return {
                ...state,
                data: state.data.filter((value, index) => index !== action.value)
            }
        default:
            return state
    }
}