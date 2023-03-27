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
import {LaunchOAuth, LtiHeightLimit, LtiTokenRetriever} from "@oxctl/ui-lti"
import {setServer, setToken} from "./actions/lti";
import {connect} from "react-redux";
import {addMessage} from "./actions/messages";
import LtiApplyTheme from "./LtiApplyTheme";
import {settings} from "./utils/settings";
import UserCalendars from './UserCalendars'
import ContextCalendars from "./ContextCalendars";
import ImportCourseEvents from './ImportCourseEvents'
import AuthoriseCalendarEvents from './AuthoriseCalendarEvents'


class App extends React.Component {
    state = {
        tryLoading: true,
        comInstructureBrandConfigJsonUrl: null,
        accountId: null,
        accountName: null,
        canvasUrl: null,
        placement: null,
        needsToken: false,
        loading: true,
        error: null,
        prompt: false,
        // The timezone of the current user.
        timezone: null,
    }

    constructor(props, context) {
        super(props, context)
        this.servers = settings
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
            accountId: parseInt(this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_account_id),
            courseName: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_course_name,
            canvasBaseUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_api_base_url,
            placement: this.jwt['https://www.instructure.com/placement'],
            returnUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/launch_presentation'].return_url,
            userId: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].canvas_user_id,
            token: token,
            loading: false,
            ltiMessageType: this.jwt['https://purl.imsglobal.org/spec/lti/claim/message_type'],
            deepLinkReturnUrl: this.jwt['https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings']?.deep_link_return_url,
            targetLinkUri: this.jwt['https://purl.imsglobal.org/spec/lti/claim/target_link_uri'],
            calendarUrl: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].url,
            timezone: this.jwt['https://purl.imsglobal.org/spec/lti/claim/custom'].person_address_timezone
        })
        this.props.setToken(token)
    }

    proxyGotToken = () => {
        this.setState({prompt: false})
    }

    calendarImportRender = () => {

        const {placement, courseId, courseName, canvasBaseUrl, ltiMessageType, calendarUrl, accountId} = this.state

        if(ltiMessageType === 'LtiDeepLinkingRequest'){
            return <ImportCourseEvents
                token={this.state.token}
                deepLinkReturnUrl={this.state.deepLinkReturnUrl}
                courseName={courseName}
                ltiServer={this.servers.ltiServer}
                targetLinkUri={this.state.targetLinkUri}
                timezone={this.state.timezone}
            />
        }

        if(calendarUrl){
            return <AuthoriseCalendarEvents
                calendarServer={this.servers.calendarServer}
                onMissingToken={() => this.setState({prompt: true})}
                personalCalendarLink={this.state.canvasBaseUrl + '/calendar?include_contexts=user_' + this.state.userId}
                proxyServer={this.servers.proxyServer}
                returnUrl={this.state.returnUrl}
                token={this.state.token}
            />
        }

        if(placement === "user_navigation") {
            return <UserCalendars
                    calendarServer={this.servers.calendarServer}
                    proxyServer={this.servers.proxyServer}
                    token={this.state.token}
                    returnUrl={this.state.returnUrl}
                    canvasUrl={this.state.canvasBaseUrl}
                    userId={this.state.userId}
                    onMissingToken={() => this.setState({prompt: true})}
                />
        }
        if (placement === "account_navigation") {
            return <ContextCalendars
                canvasBaseUrl={canvasBaseUrl}
                contextType='account'
                accountId={accountId}
                servers={this.servers}
                token={this.token}
                handleProxyRefresh={() => this.setState({prompt: true})}
            />
        }
        
        return <ContextCalendars
            canvasBaseUrl={canvasBaseUrl}
            contextType='course'
            courseId={courseId}
            courseName={courseName}
            servers={this.servers}
            token={this.token}
            handleProxyRefresh={() => this.setState({prompt: true})}
        />
    }

    
    render() {
        const {error,  comInstructureBrandConfigJsonUrl, canvasUserPrefersHighContrast} = this.state
        const {servers} = this

        return (
            <LtiTokenRetriever ltiServer={servers.ltiServer} handleJwt={this.updateToken}>
                <LtiApplyTheme url={comInstructureBrandConfigJsonUrl} highContrast={canvasUserPrefersHighContrast}>
                    <LtiHeightLimit>
                        <LaunchOAuth accessToken={this.token} promptUserLogin={this.proxyGotToken}
                                     promptLogin={this.state.prompt} server={{proxyServer: this.servers.proxyServer}}>
                            <View padding="small" as="div">
                            <Error message={error}>
                            {(this.state.loading) ? <Loading/> : <>
                                {this.calendarImportRender()}
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
