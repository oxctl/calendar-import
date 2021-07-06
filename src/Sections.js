import React from "react"
import {SimpleSelect} from "@instructure/ui-simple-select";
import PropTypes from "prop-types"
import {View} from "@instructure/ui-view";
import {handleErrors, LoginError} from "./utils/fetch";

/**
 * Displays a list of sections in the course and allows the user to select the whole course
 * or one of the sections.
 */
export default class Sections extends React.Component {

    state = {
        loading: false,
        section: '',
        sections: null,
        // Used to display a message to the user if the loading failed
        messages: []
    }

    static propType = {
        proxyServer: PropTypes.string,
        courseId: PropTypes.string,
        courseName: PropTypes.string,
        onChange: PropTypes.func
    }

    componentDidMount() {
        this.loadSections()
    }

    loadSections = () => {
        const {proxyServer, courseId, token} = this.props
        this.setState({loading: true})
        fetch(`${proxyServer}/api/v1/courses/${courseId}/sections`, {
            headers: {
                Authorization: 'Bearer ' + token,
                Accept: 'application/json'
            }
        }).then(response => {
            return handleErrors(response)
        }).then((response) => {
            return response.json()
        }).then((json) => {
            this.setState({
                sections: json
            })
        }).catch( reason => {
            if (reason instanceof LoginError) {
                this.props.handleProxyRefresh()
            } else {
                this.setState({
                    messages: [{text: 'Failed to load sections: '+ reason.message, type: 'error'}]
                })
            }
        }).finally(() => {
            this.setState({
                loading: false
            })
        })
    }
    
    getSectionName = (value) => {
        return this.state.sections.filter(section => section.id.toString() === value).shift()?.name
    }

    render() {
        const {section} = this.state
        return <View as="div" margin='small none'>
            <SimpleSelect renderLabel='Section' label={''}
                          messages={this.state.messages}
                          onChange={(e, {id, value}) => {
                              this.setState({section: value})
                              this.props.onChange({id: value, name: this.getSectionName(id)})
                          }}
                          value={section}>
                {this.renderOptions()}
            </SimpleSelect>
        </View>
    } 

    renderOptions = () => {
        const {loading, sections} = this.state
        const {courseName} = this.props
        if (loading) {
            return <SimpleSelect.Option id='loading' key='loading' value='' isDisabled>Loading....</SimpleSelect.Option>
        } else {
            // If you put multiple words as the children then spaces would have commas added in them ?
            // So 'Course: Test Course' would become 'Course:, Test Course' when displayed (wrapping fixes it).
            const options = [<SimpleSelect.Option id='course' key='course'
                                                  value=''>{`Course: ${courseName}`}</SimpleSelect.Option>]
            if (sections) {
                options.push(...sections.map(section => <SimpleSelect.Option
                    id={section.id.toString()} key={section.id}
                    value={`course_section_${section.id}`}>{`Section: ${section.name}`}</SimpleSelect.Option>)
                )
            }
            return options
        }
    }
}