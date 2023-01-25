import React from 'react'
import { connect } from 'react-redux'
import * as PropTypes from 'prop-types'

import { Button, IconButton } from '@instructure/ui-buttons'
import { Checkbox } from '@instructure/ui-checkbox'
import { Flex } from '@instructure/ui-flex'
import { Heading } from '@instructure/ui-heading'
import { Link } from '@instructure/ui-link'
import { Spinner } from '@instructure/ui-spinner'
import { Text } from '@instructure/ui-text'
import { View } from '@instructure/ui-view'
import { IconRefreshLine } from '@instructure/ui-icons'
import { IconImportLine } from '@instructure/ui-icons'

import Messages from './Messages'
import { addMessage } from './actions/messages'
import { Loading } from './Loading'
import { getRelativeTime } from './relativeTime'

class AuthoriseCalendarEvents extends React.Component {

  state = {
    message: [],
    // Is the user currently subscribed to the calendar?
    subscribed: true,
    // Is the user want to subscribe to the calendar?
    subscribedToggle: true,
    // Are we currently loading data from the server?
    loading: false,
    statusLoading: false,
    // Are we currently saving data to the server?
    saving: false,
    lastCalendarImport: {}
  }

  handleSubscribeChanged = () => {
    this.setState(prevState => ({subscribedToggle: !prevState.subscribedToggle}))
  }

  getUserSubscription = () => {
    return fetch(`${this.props.calendarServer}/api/getUserSubscription`, {
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
      const subscribed = json.load != null
      this.setState({
        subscribed: subscribed,
        subscribedToggle: subscribed,
        lastCalendarImport: json
      })
    }).catch((error) => {
      if(error.message === '401'){
        this.props.onMessage({text: 'Session has timed out, please relaunch the tool. Error: '+ error.message, type: 'error'})
      }else {
        this.props.onMessage({text: 'Failed to get data, status: ' + error, type: 'error'})
        throw error
      }
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
        this.getUserSubscription
    ).catch(error => {
      this.props.onMessage({text: 'Failed to check access, please relaunch the tool. Error: '+ error.message, type: 'error'})
    }).finally(() => {
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
        this.props.onMessage({text: `Failed to ${this.state.subscribedToggle ? 'subscribe to' : 'unsubscribe from'} calendar, status: ` + response.status, type: 'error'})
      }
      this.handleRefresh()
      // If we don't read the response then it's not available in the Chrome debugger.
      return response.text()
    }).catch(error => {
      this.props.onMessage({text: `Failed to ${this.state.subscribedToggle ? 'subscribe to' : 'unsubscribe from'} calendar: ${error.message}`, type: 'error'})
    }).finally(() => {
      this.setState({saving: false})
    })
  }

  renderAgo = (isoDateTime) => {
    return getRelativeTime(Date.parse(isoDateTime))
  }

  renderStatus = (status) => {
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }

  handleRefresh = () => {
    this.setState({statusLoading: true})
    this.getUserSubscription().then(() => {
      this.setState({statusLoading: false})
    })
  }

  renderLoadInfo = (type) => {
    if(this.state.statusLoading) return <Spinner renderTitle="Loading status"/>
    return <View as='div' background='primary' margin='small small small large' borderWidth='small' padding='small'>
      <Flex>
        <Flex.Item>
          <IconImportLine/>
        </Flex.Item>
        <Flex.Item as='div' margin='none small' shouldGrow shouldShrink>
          <Text as='div' weight='bold'>Last Import Status: {this.renderStatus(this.state.lastCalendarImport[type].status)}</Text>
          <View as='div' margin='0 0 0 small'>
            {this.state.lastCalendarImport[type].completed &&
              <>
                <Text weight='bold'>When:</Text> {this.renderAgo(this.state.lastCalendarImport[type].completed)}
                <br/>
              </>
            }
            <Text weight='bold'>Message:</Text> {this.state.lastCalendarImport[type].lastMessage}
            <br/>
            <Text weight='bold'>Logfile:</Text> <Link target='_blank' href={`${this.props.calendarServer}/api/log/${this.state.lastCalendarImport.id}/${type}ByCalendarImportId?access_token=${this.props.token}`}>logfile</Link>
          </View>
        </Flex.Item>
        <Flex.Item>
          <IconButton renderIcon={<IconRefreshLine/>} onClick={this.handleRefresh} screenReaderLabel="Refresh status"/>
        </Flex.Item>
      </Flex>
    </View>
  }

  render() {
    if(this.state.loading){
      return <Loading/>
    }
    return <>
      <Messages/>
      <Heading level="h1">Import Events Into Personal Calendar</Heading>
      <Text as="p">
        Selecting will import events in your <Link target='_top' href={this.props.personalCalendarLink}>personal Canvas calendar</Link>. These events will be regularly updated. If you deselect, this will remove the events from your personal Canvas calendar.
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
                  margin="x-small" onClick={() => window.location = this.props.returnUrl}>Back</Button>
        </Flex.Item>
      </Flex>
      {this.state.lastCalendarImport.load && this.renderLoadInfo("load")}
    </>
  }
}

AuthoriseCalendarEvents.propTypes = {
  // The URL of the calendar backend
  calendarServer: PropTypes.string,
  // Link to the user's personal calendar in Canvas
  personalCalendarLink: PropTypes.string,
  // The URL of the proxy server
  proxyServer: PropTypes.string,
  // The LTI return URL to send the user back to.
  returnUrl: PropTypes.string,
  // The JWT token to authenticate to the calendar/proxy server
  token: PropTypes.string,
  // Function to display messages to the user
  onMessage: PropTypes.func,
  // Handler for requesting user grant access
  onMissingToken: PropTypes.func
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