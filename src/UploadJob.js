import React from "react"
import {Button} from "@instructure/ui-buttons";
import {FileDrop} from "@instructure/ui-file-drop";
import {View} from "@instructure/ui-view";
import {IconUploadSolid} from "@instructure/ui-icons";
import {Text} from "@instructure/ui-text";
import { FormField} from "@instructure/ui-form-field";
import Sections from "./Sections";
import {Flex} from "@instructure/ui-flex";
import {Spinner} from "@instructure/ui-spinner";
import {connect} from "react-redux";
import {load} from "./actions/imports";

class UploadJob extends React.Component {

    changeHandler = (event) => {
        this.setState({
            file: event.target.files[0]
        })
    }
    
    state = {
        file: null,
        section: {
            id: "",
            name: null
        },
        uploading: false,
        messages: []
    }

    submitHandler = () => {
        if (!this.state.file) {
            this.setState({messages: [{ text: 'You must provide a file', type: 'error' }]})
            return
        }
        const formData = new FormData();

        formData.append('file', this.state.file);
        formData.append('sectionId', this.state.section.id)
        formData.append('sectionName', this.state.section.name)

        this.setState({
            uploading: true,
            file: null,
            messages: []
        })
        fetch(
            this.props.server+ '/api/run',
            {
                method: 'POST',
                body: formData,
                headers: {
                    'Authorization': 'Bearer '+ this.props.token
                }
            }
        ).then((response) => {
            if (response.ok) {
                this.props.onMessage({text: 'Calendar import started, click update button to follow its progress.', type: 'info'})
                this.props.load()
            } else {
                this.props.onMessage({text: 'Failed to start import, status: '+ response.status, type: 'error'})
            }
        }).finally(() => {
            this.setState({uploading:false})
        })
    }
    
    updateFile = ([file]) => {
        this.setState({
            file: file,
            messages: []
        })
    }
    
    render() {
        const {courseId, courseName, server, token} = this.props
        
        return <View as="div" margin='small none'>
            <Sections 
                server={server} 
                token={token} 
                proxyServer='https://localhost:18443' 
                courseId={courseId} 
                courseName={courseName}
                onChange={(section) => this.setState({section})}
                handleProxyRefresh={this.props.handleProxyRefresh}
            />
            <FormField id='upload' label='File'>
            <FileDrop
                accept=".csv,.ics"
                onDropAccepted={this.updateFile}
                onDropRejected={([file]) => { this.setState({messages: [{ text: 'Invalid file type', type: 'error' }]}) }}
                messages={this.state.messages}
                renderLabel={
                    <View background="secondary" as="div" textAlign="center" padding="x-small small">
                        <IconUploadSolid/>
                        <Text as="div" weight="bold">
                            Upload Event CSV
                        </Text>
                        <Text>Drag and drop or <Text color="brand">browse your files</Text></Text>
                        {this.renderSelectedFile()}
                    </View>
                }
                display="block"
            />
                <Flex margin="x-small none x-small" direction="row-reverse">
                    <Flex.Item>
                        <Button interaction={this.state.uploading?'disabled':'enabled'} color="primary" margin="x-small" onClick={this.submitHandler}>Import</Button>
                    </Flex.Item>
                    <Flex.Item>
                        {this.state.uploading && <Spinner size='small' renderTitle='Uploading calendar import'/>}
                    </Flex.Item>
                </Flex>
            </FormField>
        </View>
    }

    renderSelectedFile = () => {
        const { file } = this.state
        if (file) {
            return <Text size="small" as="div" lineHeight="double">Selected: {file.name}</Text>
        }
        return <Text size="small" as="div" lineHeight="double">No file selected</Text>;
    }
}

const mapStateToProps = state => {
    return {
    }
}

const mapDispatchToProps = dispatch => {
    return {
        load: () => dispatch(load())
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(UploadJob)