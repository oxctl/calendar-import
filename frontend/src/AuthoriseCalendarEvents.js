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
    subscribe: true,
    loading: false,
    saving: false
  }

  handleSubscribeChanged = () => {
    this.setState(prevState => ({subscribe: !prevState.subscribe}))
  }

  componentDidMount() {
    this.setState({loading: true})
    fetch(`${this.props.calendarServer}/api/is-user-subscribed`, {
        headers: {
          Accept: 'application/json',
          'Authorization': 'Bearer ' + this.props.token
        }
      }
    )
    .then((response) => {
      if (!response.ok) {
        throw Error("" + response.status);
      }
      return response.json()
    }).then((json) => {
      this.setState({subscribe: json['isUserSubscribed']})
    }).catch((error) => {
      this.props.onMessage({text: 'Failed to get data, status: ' + error, type: 'error'})
      throw error
    }).finally(() => {
      this.setState({loading: false})
    })
  }

  renderSuccessMessage = () => {
    return <Text>
      {this.state.subscribe ? 'Subscribed to' : 'Unsubscribed from'} calendar. <Link href={this.props.returnUrl}>Click here to return to the previous page.</Link>
    </Text>
  }

  submitHandler = () => {
    this.setState({
      saving: true,
      messages: []
    })
    fetch(`${this.props.calendarServer}/api/${this.state.subscribe ? 'subscribe' : 'unsubscribe'}`, {
        headers: {
          Accept: 'application/json',
          'Authorization': 'Bearer ' + this.props.token
        }
      }
    ).then((response) => {
      if (response.ok) {
        this.props.onMessage({text: this.renderSuccessMessage(), type: 'info'})
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
        <Checkbox variant="toggle" label="Import events" checked={this.state.subscribe} onChange={this.handleSubscribeChanged}/>
      </View>
      <Flex margin="x-small none x-small" direction="row-reverse">
        <Flex.Item>
          <Button interaction={this.state.saving ? 'disabled' : 'enabled'} color="primary" margin="x-small"
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