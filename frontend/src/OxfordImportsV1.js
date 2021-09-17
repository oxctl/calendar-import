import React, { Fragment } from 'react'
import {Heading} from '@instructure/ui-heading'
import { Checkbox } from '@instructure/ui-checkbox'
import { FormFieldGroup } from '@instructure/ui-form-field'
import {Text} from "@instructure/ui-text";
import { ScreenReaderContent } from '@instructure/ui-a11y-content'
import { Button } from '@instructure/ui-buttons'
import { Flex } from '@instructure/ui-flex'
import {View} from '@instructure/ui-view'

// Displays a message across the screen.
class OxfordImportsV1 extends React.Component {
  render() {
   
    return <Fragment>
        <Heading level="h1" >Toggles</Heading>
        <Text as="p">Add University Term Names and Week Numbers to my Personal Canvas Calendar</Text>


        <View as="div" margin="small none">
            <Checkbox label="Current Academic Year" variant="toggle" labelPlacement="end" defaultChecked />
        </View>
        <View as="div" margin="small none">
            <Checkbox label="Next Academic Year" variant="toggle" labelPlacement="end" defaultChecked />
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

export default OxfordImportsV1