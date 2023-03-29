import React, {Fragment} from "react";
import Messages from "./Messages";
import {Heading} from "@instructure/ui-heading";
import {Text} from "@instructure/ui-text";
import {Link} from "@instructure/ui-link";
import UploadJob from "./UploadJob";
import ImportView from "./ImportView";
import * as PropTypes from "prop-types";
import {addMessage} from "./actions/messages";
import {connect} from "react-redux";

/**
 * Renders the UI for a Course or Account.
 */
class ContextCalendars extends React.Component {
    
    state = {
        messages: []
    }
    
    render() {
        return <Fragment>
            <Messages/>
            <Heading level="h1">Import File</Heading>
            <Text as="p">
                This tool allows you to import a set of events contained in a CSV file into
                the {this.renderCalendarLink()}.
                The file has to be specifically formatted for the importer. An <Link
                href="example.csv">example file</Link> can be downloaded, edited locally and
                then uploaded again to import (You may delete extraneous columns in the
                example
                file, if necessary).
            </Text>
            <UploadJob proxyServer={this.props.servers.proxyServer}
                       calendarServer={this.props.servers.calendarServer}
                       token={this.props.token}
                       handleProxyRefresh={this.props.handleProxyRefresh}
                       contextType={this.props.contextType}
                       courseId={this.props.courseId} courseName={this.props.courseName}
                       onMessage={this.props.onMessage}/>
            <ImportView server={this.props.servers.calendarServer} token={this.props.token}
                        onMessage={this.props.onMessage}/>
        </Fragment>;
    }

    renderCalendarLink() {
        const {contextType, courseId, accountId, canvasBaseUrl} = this.props
        const contextId = (contextType === 'account' ? accountId : courseId)
        return <Link target="_blank"
                     href={`${canvasBaseUrl}/calendar?include_contexts=${contextType}_${contextId}`}
        >{contextType} calendar</Link>;
    }
}

ContextCalendars.propTypes = {
    canvasBaseUrl: PropTypes.any,
    courseId: PropTypes.any,
    accountId: PropTypes.string,
    contextType: PropTypes.string,
    servers: PropTypes.any,
    token: PropTypes.any,
    handleProxyRefresh: PropTypes.func,
    courseName: PropTypes.any,
    onMessage: PropTypes.any
};

const mapStateToProps = state => {
    return {}
}

const mapDispatchToProps = dispatch => {
    return {
        onMessage: (message) => dispatch(addMessage(message))
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(ContextCalendars)