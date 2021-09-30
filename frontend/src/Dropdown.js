import React, { Fragment } from 'react'
import { SimpleSelect } from '@instructure/ui-simple-select'
import {Alert} from '@instructure/ui-alerts'

class Dropdown extends React.Component {
    state = {
      value: ''
    }
  
    handleSelect = (e, { value }) => {
      const { onSelectedValue, defaultChoice } = this.props
      // if(defaultChoice === "Select academic year to add") {
        onSelectedValue(value)
      // }
      this.setState({ value })
    }
  
    render () {
      const { value } = this.state
      const { defaultChoice } = this.props
  
      return (
        <div>
          <SimpleSelect
            assistiveText="Use arrow keys to navigate options."
            value={value || defaultChoice}
            onChange={this.handleSelect}
          >
            {this.props.options.map((option, ind) => {
              return (
                <SimpleSelect.Option
                  id={`opt-${ind}`}
                  key={ind}
                  value={option}
                  isDisabled={option === value}
                >
                  { option }
                </SimpleSelect.Option>
              )
            })}
          </SimpleSelect>
 
        </div>
      )
    }
  }


  export default Dropdown;