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

// import { theme } from '@instructure/canvas-theme'
import jwtDecode from 'jwt-decode'
import {View} from '@instructure/ui-view'
import {Loading} from './components/loading/Loading'
import Error from './components/error/Error'
import {DEV, LOCAL, PROD, STAG} from './utils/constants'
import {Heading} from '@instructure/ui-heading'
import {LaunchOAuth, LtiHeightLimit, LtiTokenRetriever} from "@oxctl/ui-lti"
import ImportView from "./ImportView";
import UploadJob from "./UploadJob";
import {Link} from "@instructure/ui-link";
import {Text} from "@instructure/ui-text";
import Messages from "./Messages";
// import { ScreenReaderContent } from '@instructure/ui-a11y-content'


const settings = {
    [LOCAL]: {
        'ltiServer': process.env.REACT_APP_SERVER_LTI,
        'proxyServer': "https://proxy-dev.canvas.ox.ac.uk",
        'calendarServer': "https://localhost:8443"
    },
    [DEV]: {
        'ltiServer': 'https://lti.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy.canvas.ox.ac.uk'
    },
    [PROD]: {
        'ltiServer': 'https://lti.canvas.ox.ac.uk',
        'proxyServer': 'https://proxy.canvas.ox.ac.uk'
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
        messages: []
    }

    constructor(props, context) {
        super(props, context)
        this.servers = settings[window.location.origin]

    }

    jwt = null
    token = null

    updateToken = (token) => {
        this.token = token
        this.jwt = jwtDecode(token)
        this.setState({
            comInstructureBrandConfigJsonUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].com_instructure_brand_config_json_url,
            courseId: parseInt(this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_course_id),
            courseName: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_course_name,
            canvasBaseUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_api_base_url,
            token: token,
            loading: false
        })
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
        const {error, courseId, courseName, canvasBaseUrl} = this.state
        const {servers} = this
        return (
            <LtiTokenRetriever ltiServer={servers.ltiServer} handleJwt={this.updateToken}>
                {/*<LtiApplyTheme url={this.state.comInstructureBrandConfigJsonUrl}>*/}
                <LtiHeightLimit>
                    <LaunchOAuth accessToken={this.token} promptUserLogin={this.proxyGotToken}
                                 promptLogin={this.state.prompt} server={{proxyServer: 'https://localhost:18443'}}>
                        <View padding="small" as="div">
                            <Error message={error}>
                                {(this.state.loading) ? <Loading/> : <>
                                    <Messages messages={this.state.messages} onDismiss={this.handleDismiss}/>
                                    <Heading level="h1">Import File</Heading>
                                    <Text as='p'>
                                        This tool allows you to import a set of events contained in a CSV file into
                                        the <Link target='_blank'
                                                  href={`${canvasBaseUrl}/calendar?include_contexts=course_${courseId}`}>course
                                        calendar</Link>.
                                        The file has to be specifically formatted for the importer. An <Link
                                        href='example.csv'>example file</Link> can be downloaded, edited locally and
                                        then uploaded again to import (You may delete extraneous columns in the example
                                        file, if necessary).
                                    </Text>
                                    <UploadJob server={this.servers.calendarServer} token={this.token}
                                               handleProxyRefresh={() => this.setState({prompt: true})}
                                               courseId={courseId} courseName={courseName}
                                               onMessage={this.handleMessage}/>
                                    <ImportView server={this.servers.calendarServer} token={this.token}
                                                onMessage={this.handleMessage}/>
                                </>}
                            </Error>
                        </View>
                        {/*</LtiApplyTheme>*/}
                    </LaunchOAuth>
                </LtiHeightLimit>
            </LtiTokenRetriever>
        )
    }
}

export default App
