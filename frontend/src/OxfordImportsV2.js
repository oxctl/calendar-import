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
import { Select } from '@instructure/ui-select'

// Displays a message across the screen.
class OxfordImportsV2 extends React.Component {
  render() {
   
    return <Fragment>
        <Heading level="h1" >University Term Names and Week Numbers</Heading>
        <Text as="p">Add oxford terms / weeks to my personal Canvas calendar</Text>


        <View as="div" margin="small none">
            <Text as="p">Available years: </Text>
            <Dropdown  options={[
                { id: 'opt1', label: 'Select academic year to add' },
                { id: 'opt2', label: '2021/2022 Academic Year' },
                { id: 'opt3', label: '2022/2023 Academic Year' },
                { id: 'opt4', label: '2023/2024 Academic Year' },
                { id: 'opt5', label: '2024/2025 Academic Year' },
                { id: 'opt6', label: '2025/2026 Academic Year' }
            ]}/>
        </View>

        <View as="div">
            <Button color="primary" margin="small none"  > Add selected year to Calendar</Button>
        </View>
        <View as="div">
            <Button color="primary" margin="small none" > Add all available years to Calendar</Button>
        </View>


        <Text as="p">Remove oxford terms / weeks from my personal Canvas calendar</Text>
        <View as="div" margin="small none">
            <Text as="p">Available years: </Text>
            <Dropdown  options={[
                { id: 'opt1', label: 'Select academic year to remove' },
                { id: 'opt2', label: '2021/2022 Academic Year' },
                { id: 'opt3', label: '2022/2023 Academic Year' },
                { id: 'opt4', label: '2023/2024 Academic Year' },
                { id: 'opt5', label: '2024/2025 Academic Year' },
                { id: 'opt6', label: '2025/2026 Academic Year' }
            ]}/>
        </View>

        <View as="div">
            <Button color="primary" margin="small none"  > Remove selected year from Calendar</Button>
        </View>
        <View as="div">
            <Button color="primary" margin="small none" > Remove all available years from Calendar</Button>
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

export default OxfordImportsV2