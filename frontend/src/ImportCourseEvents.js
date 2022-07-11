import React from 'react'
import Messages from './Messages'
import { Heading } from '@instructure/ui-heading'
import {Text} from '@instructure/ui-text'
import * as PropTypes from 'prop-types'
import { addMessage } from './actions/messages'
import { connect } from 'react-redux'
import { TextInput } from '@instructure/ui-text-input'
import { View } from '@instructure/ui-view'
import { Flex } from '@instructure/ui-flex'
import { Button } from '@instructure/ui-buttons'
import { Spinner } from '@instructure/ui-spinner'
import { Link } from '@instructure/ui-link'

class ImportCourseEvents extends React.Component {

  state = {
    messages: [],
    submitting: false,
    url: "",
    pageName: 'Import "' + this.props.courseName + '" events',
    deepLinkingJwt: ""
  }

  handleUrlChange = (e, value) => {
    this.setState({url: value})
  }

  handlePageNameChange = (e, value) => {
    this.setState({pageName: value})
  }

  isValidUrl = (url) => {
    try {
      new URL(url);
    } catch (e) {
      return false;
    }
    return true;
  }

  isMagicUrl = (url) => {
    return url.startsWith('calendar://')
  }

  VALID_VARIABLE_REGEX = new RegExp(/(?<=\${).+?(?=})/, 'g')
  VALID_VARIABLES = ['course.id', 'user.sis_id']

  hasValidVariables = (url) => {
    const matches = url.match(this.VALID_VARIABLE_REGEX)
    if(!matches) return true
    return matches.every(el => this.VALID_VARIABLES.includes(el))
  }

  validate = () => {
    const {url, pageName} = this.state
    if(!url || !pageName){
      this.props.onMessage({text: 'You must provide a URL and Page name', type: 'error'})
      return false
    }

    if(!this.hasValidVariables(url)){
      this.props.onMessage({text: `Parameterised URL variables must be one of: ${this.VALID_VARIABLES.join(', ')}`, type: 'error'})
      return false
    }

    if(!this.isMagicUrl(url) && !this.isValidUrl(url)){
      this.props.onMessage({text: 'Not a valid URL', type: 'error'})
      return false
    }

    return true
  }

  submitHandler = () => {
    if(!this.validate()) return

    this.setState({
      submitting: true,
      messages: []
    })

    const url = this.props.ltiServer + '/deep-linking'

    const body = {
      "https://purl.imsglobal.org/spec/lti-dl/claim/content_items": [{
        "type": "ltiResourceLink",
        "title": this.state.pageName,
        "url": this.props.targetLinkUri,
        "custom": {
          "url": this.state.url
        }
      }]
    }

    fetch(
      url,
  {
        method: 'POST',
        body: JSON.stringify(body),
        headers: new Headers({
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + this.props.token
        })
      }
    ).then((response) => {
      if (!response.ok) {
        throw Error("" + response.status);
      }
      return response.json()
    }).then((json) => {
      this.setState({deepLinkingJwt: json['jwt']}, () => {
            document.getElementById('deepLinkingForm').submit()
      })
    }).catch((error) => {
      this.props.onMessage({text: 'Failed to add, status: ' + error, type: 'error'})
    }).finally(() => {
      this.setState({submitting: false})
    })
  }

  render() {
    const urlLabel = "Server URL"
    const pageNameLabel = "Page name"
    return <>
      <Messages/>
      <Heading level="h1">Import Course Events</Heading>
      <Text as="p">
        Provides the facility for students to import course-related events into their personal Canvas calendar and receive regular updates. You may parameterise your URL with: $&#123;course.id&#125; and $&#123;user.sis_id&#125;.
        Here is an <Link href="./import_course_events_example.csv">example file</Link> in the necessary format.
      </Text>
      <View as="div" margin="0 0 small 0">
        <TextInput
            renderLabel={urlLabel}
            onChange={this.handleUrlChange}
        />
      </View>
      <View as="div" margin="0 0 small 0">
        <TextInput
            renderLabel={pageNameLabel}
            value={this.state.pageName}
            onChange={this.handlePageNameChange}
        />
      </View>
      <Flex margin="x-small none x-small" direction="row-reverse">
        <Flex.Item>
          <Button interaction={this.state.submitting ? 'disabled' : 'enabled'} color="primary" margin="x-small"
                  onClick={this.submitHandler}>Add</Button>
        </Flex.Item>
        <Flex.Item>
          {this.state.submitting && <Spinner size='small' renderTitle='Adding'/>}
        </Flex.Item>
      </Flex>
      <form name="deepLinkingForm" id="deepLinkingForm" method="post" action={this.props.deepLinkReturnUrl}>
        <input type="hidden" name="JWT" value={this.state.deepLinkingJwt} />
      </form>
    </>
  }
}

ImportCourseEvents.propTypes = {
  token: PropTypes.string,
  courseName: PropTypes.string,
  deepLinkReturnUrl: PropTypes.string,
  ltiServer: PropTypes.string,
  targetLinkUri: PropTypes.string
}

const mapStateToProps = state => {
  return {}
}

const mapDispatchToProps = dispatch => {
  return {
    onMessage: (message) => dispatch(addMessage(message))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ImportCourseEvents)