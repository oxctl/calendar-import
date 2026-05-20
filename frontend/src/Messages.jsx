import React from 'react'
import { Alert, Flex, View } from '@instructure/ui'
import PropTypes from 'prop-types'
import {connect} from "react-redux";
import {deleteMessage} from "./actions/messages";

/**
 * Displays messages to the user.
 */
class Messages extends React.Component {

  static propTypes = {
    messages: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.oneOf(['info', 'success', 'warning', 'error']),
        text: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
        timeout: PropTypes.number
      })
    ),
    onDismiss: PropTypes.func.isRequired
  }

  static defaultProps = {
    messages: []
  }

  renderAlerts() {
    return this.props.messages.map((message, index) => <Alert
        variant={message.type}
        renderCloseButtonLabel="Close"
        margin="small"
        transition="fade"
        onDismiss={() => this.props.onDismiss(index)}
        key={index}
        timeout={message.timeout}
      >{message.text}</Alert>
    )
  }

  render() {
    // We disable pointer events on the div that puts the items in the centre so that you can click on things
    // under it and then re-enable them so you can dismiss the events.
    return <View stacking='topmost' as='div' position='fixed' width='100%' style={{pointerEvents: 'none'}}>
        <Flex justifyItems="center" alignItems="center">
          <Flex.Item>
          <span style={{ pointerEvents: 'auto' }}>
          {this.renderAlerts()}
          </span>
          </Flex.Item>
        </Flex>
      </View>
  }
}

const mapStateToProps = state => {
  return {
    messages: state.messages.data
  }
}

const mapDispatchToProps = dispatch => {
  return {
    onDismiss: (index) => dispatch(deleteMessage(index))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(Messages)
