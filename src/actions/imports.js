import {addMessage} from "./messages";

const PREFIX = "IMPORTS:"

export const LOAD = PREFIX + "LOAD"
export const LOADING = PREFIX + "LOADING"
export const DATA = PREFIX + "DATA"
// Pagination actions
export const PAGE = PREFIX + "PAGE"

export const load = () => {
    return (dispatch, getState) => {
        const {lti: {token, server}, imports: {size, page}} = getState()
        dispatch(setLoading(true))
        fetch(`${server}/api/imports?page=${page}&size=${size}`, {
            headers: {
                'Accept': 'application/json',
                'Authorization': 'Bearer ' + token
            }
        }).then(response => {
            if (!response.ok) {
                throw new Error('Bad response. (status: ' + response.status + ')')
            }
            return response.json()
        }).then(json => {
            dispatch(setData(json))
        }).catch(reason => {
            dispatch(addMessage({text: 'Failed to load calendar imports: '+ reason.message, type: 'error'}))
            console.log(reason)
        }).finally(() => dispatch(setLoading(false)))
       
    }

}

export const setLoading = (loading) => {
    return {
        type: LOADING,
        value: loading
    }
}

export const setData = (data) => {
    return {
        type: DATA,
        value: data
    }
}

export const setPage = (page) => {
    return {
        type: PAGE,
        value: page
    }
}