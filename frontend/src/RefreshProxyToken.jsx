import React from "react"
import {Loading} from "./Loading";
import {addMessage} from "./actions/messages";
import {connect} from "react-redux";
import PropTypes from "prop-types";

class RefreshProxyToken extends React.Component {
    
    static propTypes = {
        onMessage: PropTypes.func.isRequired,
        onMissingToken: PropTypes.func.isRequired,
        proxyServer: PropTypes.string.isRequired,
        token: PropTypes.string.isRequired
    }
    
    state = {
        loading: false
    }

    componentDidMount() {
        const {proxyServer, token, onMissingToken, onMessage} = this.props
        this.setState({loading: true})

        fetch(proxyServer + '/tokens/refresh?force=true', {
            // We need this so that we don't get redirected to grant access, but instead detect that we need to go to the grant access page.
            redirect: 'manual',
            headers: {
                Accept: 'application/json',
                Authorization: 'Bearer ' + token
            }
        }).then(response => {
            // When expired we will get a 401 back.
            if (response.status === 401 || response.type === 'opaqueredirect') onMissingToken()
        }).catch(error => {
            onMessage({text: 'Failed to check access, please relaunch the tool. Error: '+ error.message, type: 'error'})
        }).finally(() => {
            this.setState({loading: false})
        })
    }
    
    render () {
        return <Loading loading={this.state.loading}>
            {this.props.children}
        </Loading>
    }
}

const mapStateToProps = state => {
    return {}
}

const mapDispatchToProps = dispatch => {
    return {
        onMessage: (message) => dispatch(addMessage(message))
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RefreshProxyToken)