import React, { Fragment } from 'react'
import { Select } from '@instructure/ui-select'
import {Alert} from '@instructure/ui-alerts'

class Dropdown extends React.Component {
    state = {
      inputValue: this.props.options[0].label,
      isShowingOptions: false,
      highlightedOptionId: null,
      selectedOptionId: this.props.options[0].id,
      announcement: null
    }
  
    getOptionById (queryId) {
      return this.props.options.find(({ id }) => id === queryId)
    }
  
    handleShowOptions = (event) => {
      this.setState({
        isShowingOptions: true
      })
    }
  
    handleHideOptions = (event) => {
      const { selectedOptionId } = this.state
      const option = this.getOptionById(selectedOptionId).label
      this.setState({
        isShowingOptions: false,
        highlightedOptionId: null,
        inputValue: selectedOptionId ? option : '',
        announcement: 'List collapsed.'
      })
    }
  
    handleBlur = (event) => {
      this.setState({
        highlightedOptionId: null
      })
    }
  
    handleHighlightOption = (event, { id }) => {
      event.persist()
      const optionsAvailable = `${this.props.options.length} options available.`
      const nowOpen = !this.state.isShowingOptions ? `List expanded. ${optionsAvailable}` : ''
      const option = this.getOptionById(id).label
      this.setState((state) => ({
        highlightedOptionId: id,
        inputValue: event.type === 'keydown' ? option : state.inputValue,
        announcement: `${option} ${nowOpen}`
      }))
    }
  
    handleSelectOption = (event, { id }) => {
      const option = this.getOptionById(id).label
      this.setState({
        selectedOptionId: id,
        inputValue: option,
        isShowingOptions: false,
        announcement: `"${option}" selected. List collapsed.`
      })
    }
  
    render () {
      const {
        inputValue,
        isShowingOptions,
        highlightedOptionId,
        selectedOptionId,
        announcement
      } = this.state
  
      return (
        <div>
          <Select
            renderLabel="Single Select"
            assistiveText="Use arrow keys to navigate options."
            inputValue={inputValue}
            isShowingOptions={isShowingOptions}
            onBlur={this.handleBlur}
            onRequestShowOptions={this.handleShowOptions}
            onRequestHideOptions={this.handleHideOptions}
            onRequestHighlightOption={this.handleHighlightOption}
            onRequestSelectOption={this.handleSelectOption}
          >
            {this.props.options.map((option) => {
              return (
                <Select.Option
                  id={option.id}
                  key={option.id}
                  isHighlighted={option.id === highlightedOptionId}
                  isSelected={option.id === selectedOptionId}
                >
                  { option.label }
                </Select.Option>
              )
            })}
          </Select>
          <Alert
            liveRegion={() => document.getElementById('flash-messages')}
            liveRegionPoliteness="assertive"
            screenReaderOnly
          >
            { announcement }
          </Alert>
        </div>
      )
    }
  }


  export default Dropdown;