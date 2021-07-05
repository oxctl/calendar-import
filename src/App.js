/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 - present Instructure, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import React from 'react'

import jwtDecode from 'jwt-decode'
import {View} from '@instructure/ui-view'
import {Loading} from './Loading'
import Error from './Error'
import {DEV, LOCAL, PROD} from './utils/constants'
import {Heading} from '@instructure/ui-heading'
import {LaunchOAuth, LtiHeightLimit, LtiTokenRetriever} from "@oxctl/ui-lti"
import ImportView from "./ImportView";
import UploadJob from "./UploadJob";
import {Link} from "@instructure/ui-link";
import {Text} from "@instructure/ui-text";
import Messages from "./Messages";
import {setServer, setToken} from "./actions/lti";
import {connect} from "react-redux";
import {addMessage} from "./actions/messages";
import LtiApplyTheme from "./LtiApplyTheme";


const settings = {
    [LOCAL]: {
        'ltiServer': process.env.REACT_APP_SERVER_LTI,
        'proxyServer': process.env.REACT_APP_SERVER_PROXY,
        'calendarServer': process.env.REACT_APP_SERVER_CALENDAR
    },
    [DEV]: {
        'ltiServer': 'https://lti-dev.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy-dev.canvas.ox.ac.uk',
        'calendarServer': 'https://calendar-import-dev.canvas.ox.ac.uk'
    },
    [PROD]: {
        'ltiServer': 'https://lti.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy.canvas.ox.ac.uk',
        'calendarServer': 'https://calendar-import.canvas.ox.ac.uk'
    }
}

// theme.use()

class App extends React.Component {
    state = {
        tryLoading: true,
        comInstructureBrandConfigJsonUrl: null,
        accountId: null,
        accountName: null,
        canvasUrl: null,
        needsToken: false,
        loading: true,
        error: null,
        prompt: false,
    }

    constructor(props, context) {
        super(props, context)
        this.servers = settings[window.location.origin]
        this.props.setServer(this.servers.calendarServer)
    }

    jwt = null
    token = null

    updateToken = (token) => {
        this.token = token
        this.jwt = jwtDecode(token)
        this.setState({
            comInstructureBrandConfigJsonUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].com_instructure_brand_config_json_url,
            canvasUserPrefersHighContrast: (this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_user_prefers_high_contrast === 'true'),
            courseId: parseInt(this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_course_id),
            courseName: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_course_name,
            canvasBaseUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_api_base_url,
            token: token,
            loading: false
        })
        this.props.setToken(token)
    }

    proxyGotToken = () => {
        this.setState({prompt: false})
    }

    handleMessage = (message) => {
        this.setState({messages: [...this.state.messages, message]})
    }

    handleDismiss = (index) => {
        const messages = [...this.state.messages]
        messages.splice(index, 1)
        this.setState({messages})
    }

    render() {
        const {error, courseId, courseName, canvasBaseUrl, comInstructureBrandConfigJsonUrl, canvasUserPrefersHighContrast} = this.state
        const {servers} = this
        return (
            <LtiTokenRetriever ltiServer={servers.ltiServer} handleJwt={this.updateToken}>
                <LtiApplyTheme url={comInstructureBrandConfigJsonUrl} highContrast={canvasUserPrefersHighContrast}>
                    <LtiHeightLimit>
                        <LaunchOAuth accessToken={this.token} promptUserLogin={this.proxyGotToken}
                                     promptLogin={this.state.prompt} server={{proxyServer: 'https://localhost:18443'}}>
                            <View padding="small" as="div">
                                <Error message={error}>
                                    {(this.state.loading) ? <Loading/> : <>
                                        <Messages onDismiss={this.handleDismiss}/>
                                        <Heading level="h1">Import File</Heading>
                                        <Text as='p'>
                                            This tool allows you to import a set of events contained in a CSV file into
                                            the <Link target='_blank'
                                                      href={`${canvasBaseUrl}/calendar?include_contexts=course_${courseId}`}>course
                                            calendar</Link>.
                                            The file has to be specifically formatted for the importer. An <Link
                                            href='example.csv'>example file</Link> can be downloaded, edited locally and
                                            then uploaded again to import (You may delete extraneous columns in the
                                            example
                                            file, if necessary).
                                        </Text>
                                        <UploadJob proxyServer={this.servers.proxyServer}
                                                   calendarServer={this.servers.calendarServer}
                                                   token={this.token}
                                                   handleProxyRefresh={() => this.setState({prompt: true})}
                                                   courseId={courseId} courseName={courseName}
                                                   onMessage={this.props.addMessage}/>
                                        <ImportView server={this.servers.calendarServer} token={this.token}
                                                    onMessage={this.props.addMessage}/>
                                    </>}
                                </Error>
                            </View>
                        </LaunchOAuth>
                    </LtiHeightLimit>
                </LtiApplyTheme>
            </LtiTokenRetriever>
        )
    }
}

const mapStateToProps = state => {
    return {}
}

const mapDispatchToProps = dispatch => {
    return {
        setToken: (token) => dispatch(setToken(token)),
        setServer: (server) => dispatch(setServer(server)),
        addMessage: (message) => dispatch(addMessage(message))
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(App)
