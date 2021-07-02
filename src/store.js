import {createStore, combineReducers, applyMiddleware, compose} from 'redux';
import importsReducer from './reducers/imports'
import ltiReducer from './reducers/lti'
import messagesReducer from './reducers/messages'
import thunk from "redux-thunk";

// redux reducers combined into a single option (makes easily accessible when debugging)
const reducer = combineReducers({
    imports: importsReducer,
    lti: ltiReducer,
    messages: messagesReducer
})

// enable redux debugging
const composeEnhancers = (typeof window === "object" && window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ ) || compose;

const store = createStore(reducer, composeEnhancers(applyMiddleware(thunk)));
export default store;