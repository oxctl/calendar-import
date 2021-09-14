import {DATA, LOADING, PAGE} from "../actions/imports";

const initialState = {
    // Are we loading the data
    loading: false,
    // The number of items to load on each page
    size: 10,
    // The current page
    page: 0,
    // What data do we have
    data: undefined,
}

export default (state = initialState, action) => {
    switch (action.type) {
        case LOADING: 
            return {
                ...state,
                loading: action.value
            }
        case DATA:
            return {
                ...state,
                data: action.value,
            }
        case PAGE:
            const total = state.data?.totalPages || 0
            return {
                ...state,
                // Sanity check to make sure it never goes negative or beyond the last page.
                page: Math.min(Math.max(action.value, 0), total)
            }
        default:
            return state
    }
}