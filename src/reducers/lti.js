import {SET_SERVER, SET_TOKEN} from "../actions/lti";

const initialState = {
    token: undefined,
    server: undefined
}

export default (state = initialState, action) => {
    switch (action.type) {
        case SET_SERVER:
            return {
                ...state,
                server: action.value
            }
        case SET_TOKEN:
            return {
                ...state,
                token: action.value
            }
        default:
            return state
        
    }
}