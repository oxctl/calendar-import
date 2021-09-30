import React, { Fragment } from 'react'
import {Heading} from '@instructure/ui-heading'
import { Checkbox } from '@instructure/ui-checkbox'
import { FormFieldGroup } from '@instructure/ui-form-field'
import {Text} from "@instructure/ui-text";
import Dropdown from './Dropdown'
import { ScreenReaderContent } from '@instructure/ui-a11y-content'
import { Button } from '@instructure/ui-buttons'
import { Flex } from '@instructure/ui-flex'
import {View} from '@instructure/ui-view'
import {fetchImport, load, setPage} from "./actions/imports";
import {connect} from 'react-redux';

// Displays a message across the screen.
class OxfordImportsV2 extends React.Component {

    componentDidMount() {
        this.props.load()
    }

    handleImport(action) {

        const {load} = this.props
        load()
    }

    handleSelectedAddValue(value) {
        const {fetchImport} = this.props
        fetchImport('id', 'add')
    }

    handleSelectedRemoveValue(value) {
        const {fetchImport} = this.props
        fetchImport('id', 'remove')
    }


    getCurrentAcademicYear() {
        // find the current date and year
        let fullDate = new Date();

        // populate all years based on current academic year
        let fullYear = fullDate.getFullYear();
        if(fullDate.getMonth() > 8) {
            fullYear++;
        }
        return fullYear;
    }

  render() {

      
      let yearOptions = (action) => {
        let year = this.getCurrentAcademicYear();
        return [
           `Select academic year to ${action}`,
           `${year}/${++year} Academic Year`,
           `${year}/${++year} Academic Year`,
           `${year}/${++year} Academic Year`, 
           `${year}/${++year} Academic Year`, 
           `${year}/${++year} Academic Year` 
        ]
    }
   
    return <Fragment>
        <Heading level="h1" >University Term Names and Week Numbers</Heading>
        <Text as="p">Add oxford terms / weeks to my personal Canvas calendar</Text>


        <View as="div" margin="small none">
            <Text as="p">Available years: </Text>
            <Dropdown  options={yearOptions('add')} defaultChoice={'Select academic year to add'} onSelectedValue={(val) => this.handleSelectedAddValue(val)}/>
        </View>

        <View as="div">
            <Button color="primary" margin="small none"  > Add selected year to Calendar</Button>
        </View>
        <View as="div">
            <Button color="primary" margin="small none" onClick={() => this.handleImport('add')} > Add all available years to Calendar</Button>
        </View>


        <Text as="p">Remove oxford terms / weeks from my personal Canvas calendar</Text>
        <View as="div" margin="small none">
            <Text as="p">Available years: </Text>
            <Dropdown  options={yearOptions('remove')} defaultChoice={'Select academic year to remove'} onSelectedValue={(val) => this.handleSelectedRemoveValue(val)}/>
        </View>

        <View as="div">
            <Button color="primary" margin="small none"  > Remove selected year from Calendar</Button>
        </View>
        <View as="div">
            <Button color="primary" margin="small none" onClick={() => this.handleImport('remove')} > Remove all available years from Calendar</Button>
        </View>

        <View borderWidth='small none none none' borderColor='primary' as='div'>
            <Flex justifyItems='end' wrap='wrap'>
                <Flex.Item margin='small xx-small'>
                    <Button color="primary" onClick={() => {  }}>
                        Save
                    </Button>
                </Flex.Item>
                <Flex.Item>
                    <Button color="primary" margin="none small"  onClick={() => {  }}>
                        Cancel
                    </Button>
                </Flex.Item>
            </Flex>
        </View>
    </Fragment>
  }
}

const mapStateToProps = state => {
    const {imports: {data, loading}} = state

    console.log('data ', data)
    
    return {
        data,
        loading
    }
}

const mapDispatchToProps = dispatch => {
    return {
        load: () => dispatch(load()),
        fetchImport: () => dispatch(fetchImport()),
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(OxfordImportsV2)