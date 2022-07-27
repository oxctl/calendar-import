import React from 'react'
import { connect } from 'react-redux'
import * as PropTypes from 'prop-types'

import { Button } from '@instructure/ui-buttons'
import { Checkbox } from '@instructure/ui-checkbox'
import { Flex } from '@instructure/ui-flex'
import { Heading } from '@instructure/ui-heading'
import { Link } from '@instructure/ui-link'
import { Spinner } from '@instructure/ui-spinner'
import { Text } from '@instructure/ui-text'
import { View } from '@instructure/ui-view'

import Messages from './Messages'
import { addMessage } from './actions/messages'
import { Loading } from './Loading'

class AuthoriseCalendarEvents extends React.Component {

  state = {
    message: [],
    // Is the user currently subscribed to the calendar?
    subscribed: true,
    // Is the user want to subscribe to the calendar?
    subscribedToggle: true,
    // Are we currently loading data from the server?
    loading: false,
    // Are we currently saving data to the server?
    saving: false
  }

  handleSubscribeChanged = () => {
    this.setState(prevState => ({subscribedToggle: !prevState.subscribedToggle}))
  }

  getUserSubscribed = () => {
    return fetch(`${this.props.calendarServer}/api/isUserSubscribed`, {
        headers: {
          Accept: 'application/json',
          'Authorization': 'Bearer ' + this.props.token
        }
      }
    ).then((response) => {
      if (!response.ok) {
        throw Error("" + response.status);
      }
      return response.json()
    }).then((json) => {
      this.setState({
        subscribed: json,
        subscribedToggle: json
      })
    }).catch((error) => {
      this.props.onMessage({text: 'Failed to get data, status: ' + error, type: 'error'})
      throw error
    })
  }

  componentDidMount() {
    const {proxyServer, token} = this.props
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
      if (response.status === 401 || response.type === 'opaqueredirect') this.props.onMissingToken()
    }).then(
        this.getUserSubscribed
    ).finally(() => {
      this.setState({loading: false})
    })
  }

  renderSuccessMessage = () => {
    return <Text>
      {this.state.subscribedToggle ? 'Subscribed to' : 'Unsubscribed from'} calendar. <Link href={this.props.returnUrl}>Click here to return to the previous page.</Link>
    </Text>
  }

  submitHandler = () => {
    this.setState({
      saving: true,
      messages: []
    })
    fetch(`${this.props.calendarServer}/api/${this.state.subscribedToggle ? 'subscribe' : 'unsubscribe'}`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Authorization': 'Bearer ' + this.props.token
        }
      }
    ).then((response) => {
      if (response.ok) {
        this.props.onMessage({text: this.renderSuccessMessage(), type: 'info'})
        this.setState({subscribed: this.state.subscribedToggle})
      } else {
        this.props.onMessage({text: `Failed to ${this.state.subscribe ? 'subscribe to' : 'unsubscribe from'} calendar, status: ` + response.status, type: 'error'})
      }
    }).finally(() => {
      this.setState({saving: false})
    })
  }

  render() {
    if(this.state.loading){
      return <Loading/>
    }
    return <>
      <Messages/>
      <Heading level="h1">Import Events Into Personal Calendar</Heading>
      <Text as="p">
        Selecting will import events in your <Link target='_top' href={this.props.personalCalendarLink}>personal Canvas calendar</Link>. These events will be regularly updated. If you deselect this it will remove the events from your personal Canvas calendar.
      </Text>
      <View as="div" margin="0 0 small 0">
        <Checkbox variant="toggle" label="Import events" checked={this.state.subscribedToggle} onChange={this.handleSubscribeChanged}/>
      </View>
      <Flex margin="x-small none x-small" direction="row-reverse">
        <Flex.Item>
          <Button interaction={this.state.saving || this.state.subscribed === this.state.subscribedToggle ? 'disabled' : 'enabled'} color="primary" margin="x-small"
                  onClick={this.submitHandler}>Save</Button>
        </Flex.Item>
        <Flex.Item>
          {this.state.saving && <Spinner size='small' renderTitle='Saving'/>}
        </Flex.Item>
        <Flex.Item>
          <Button interaction={this.state.saving || !this.props.returnUrl ? 'disabled' : 'enabled'} color="secondary"
                  margin="x-small" onClick={() => window.location = this.props.returnUrl}>Cancel</Button>
        </Flex.Item>
      </Flex>
    </>
  }
}

AuthoriseCalendarEvents.propTypes = {
  calendarServer: PropTypes.string,
  personalCalendarLink: PropTypes.string,
  proxyServer: PropTypes.string,
  returnUrl: PropTypes.string,
  token: PropTypes.string,
}

const mapStateToProps = state => {
  return {}
}

const mapDispatchToProps = dispatch => {
  return {
    onMessage: (message) => dispatch(addMessage(message))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(AuthoriseCalendarEvents)