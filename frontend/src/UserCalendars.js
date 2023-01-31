import React, { Fragment } from 'react'
import { Heading } from '@instructure/ui-heading'
import { Checkbox } from '@instructure/ui-checkbox'
import { Text } from '@instructure/ui-text'
import { Button } from '@instructure/ui-buttons'
import { Flex } from '@instructure/ui-flex'
import { View } from '@instructure/ui-view'
import { Spinner } from '@instructure/ui-spinner'
import { Alert } from '@instructure/ui-alerts'
import { checkOK } from './utils/fetch'
import { Link } from '@instructure/ui-link'
import PropTypes from 'prop-types'
import { CalendarError } from './CalendarError'

class UserCalendars extends React.Component {

  state = {
    current: false,
    currentRunning: false,
    currentCalendar: {title: 'unknown'},
    currentImport: {delete: true},
    currentImportId: null,
    next: false,
    nextRunning: false,
    nextCalendar: {title: 'unknown'},
    nextImport: {delete: true},
    nextImportId: null,
    alerts: [],
    loading: false
  }

  static propTypes = {
    token: PropTypes.string.isRequired,
    proxyServer: PropTypes.string.isRequired,
    calendarServer: PropTypes.string.isRequired,
    canvasId: PropTypes.string.isRequired,
    userId: PropTypes.string.isRequired,
    returnUrl: PropTypes.string.isRequired,
    onMissingToken: PropTypes.func.isRequired,
    // The current date. This allows easy faking of the current time without having to setup fake timers.
    date: PropTypes.func
  }

  static defaultProps = {
    date: () => new Date()
  }

  /**
   * fetch, but adds on our Authorization token and Accept header.
   */
  fetch = (resource, init = {}) => {
    const {token} = this.props
    const headers = {
      Authorization: 'Bearer ' + token,
      Accept: 'application/json',
      ...init.headers
    }
    const options = {...init, headers}
    return fetch(resource, options)
  }


  componentDidMount() {
    const {proxyServer} = this.props
    this.setState({loading: true})
    this.fetch(proxyServer + '/tokens/refresh?force=true', {
      // We need this so that we don't get redirected to grant access, but instead detect that we need to go to the grant access page.
      redirect: 'manual'
    }).then(response => {
      // When expired we will get a 401 back.
      if (response.status !== 200) {
        if (response.status === 401 || response.type === 'opaqueredirect') {
          this.props.onMissingToken()
        } else {
          checkOK(response)
        }
      }
    }).then(
        this.loadData
    ).catch(reason => {
      this.addAlert("Failed to refresh token. Please refresh the page.", 'error')
    }).finally(() => {
      this.setState({loading: false})
    })
  }

  loadData = () => {
    const {calendarServer} = this.props
    const predefinedFetch = this.fetch(calendarServer + "/api/predefined")
        .then((response) => {
          if (!response.ok) {
            throw new CalendarError(response.status)
          }
          return response.json()
        })


    const importsFetch = this.fetch(calendarServer + "/api/imports")
        .then((response) => {
          if (!response.ok) {
            throw new CalendarError(response.status)
          }
          return response.json()
        })

    // Wait for both promises to resolve before working things out.
    return Promise.all([predefinedFetch, importsFetch])
        .then(([predefinedJson, importsJson]) => {
          // Use proper Dates
          predefinedJson.forEach(calendar => {
            calendar.properties.start = new Date(calendar.properties.start)
            calendar.properties.end = new Date(calendar.properties.end)
          });
          // To allow testing
          const now = this.props.date()

          // Find the current calendar and next year's calendar.
          const current = predefinedJson.find(calendar => calendar.properties.start < now && calendar.properties.end > now);
          const future = new Date(this.props.date().setFullYear(this.props.date().getFullYear() + 1))
          const next = predefinedJson.find(calendar => calendar.properties.start < future && calendar.properties.end > future);
          if (!current || !next ) {
            throw new CalendarError()
          }
          this.setState({
            currentCalendar: current,
            nextCalendar: next
          })

          // Check to see if we have any imports for the current/next calendars
          importsJson.content.forEach(({id, calendarImport}) => {
            if (calendarImport.filename === current.filename) {
              // If we have a delete then ignore the load
              const running = (calendarImport.delete) ?
                  this.isRunning(calendarImport.delete.status) :
                  this.isRunning(calendarImport.load.status)
              if (running) {
                this.setState({currentRunning: true})
                this.doPoll(id, "current")
              }
              const imported = !calendarImport.delete
              this.setState({
                currentImportId: id,
                currentImport: calendarImport,
                current: imported
              })
            }
            if (calendarImport.filename === next.filename) {
              // If we have a delete then ignore the load
              const running = (calendarImport.delete) ?
                  this.isRunning(calendarImport.delete.status) :
                  this.isRunning(calendarImport.load.status)
              if (running) {
                this.setState({nextRunning: true})
                this.doPoll(id, "next")
              }
              const imported = !calendarImport.delete
              this.setState({
                nextImportId: id,
                nextImport: calendarImport,
                next: imported
              })
            }
          })
        }).catch((error) => {
          if(error.status === 401){
            this.addAlert('Session has timed out, please relaunch the tool. Error: ' + error.status, 'error')
          }else if(error.status === 500){
            this.addAlert('Network request failed. Error: ' + error.status, 'error')
          }else{
            this.addAlert('Failed to get data, status: ' + error, 'error')
          }
        })
  }

  /**
   * Is the job running.
   * @param status
   * @return {boolean}
   */
  isRunning = (status) => {
    switch (status) {
      case "QUEUED":
        return true
      case "PROCESSING":
        return true
      case "COMPLETED":
        return false
      case "PROBLEMS":
        return false
      case "FAILED":
        return false
      case "ERRORED":
        return false
      default:
        return false
    }
  }

  doSave = async () => {
    try{
      const {current, currentCalendar, currentImport, currentImportId} = this.state
      // Is it already imported?
      const currentAlready = currentImport && !currentImport.delete
      if (current) {
        if (!currentAlready) {
          // If there's already an import hide it, this is so that the list of imports doesn't grow
          if (currentImportId) {
            await this.doHide(currentImportId)
          }
          const filename = currentCalendar.filename
          const blob = await this.doDownload(filename)
          await this.doImport(filename, blob, "current")
        }
      } else {
        if (currentAlready) {
          await this.doDelete(currentImportId, "current")
        }
      }

      const {next, nextCalendar, nextImport, nextImportId} = this.state
      const nextAlready = nextImport && !nextImport.delete
      if (next) {
        if (!nextAlready) {
          // If there's already an import hide it.
          if (nextImportId) {
            await this.doHide(nextImportId)
          }
          const filename = nextCalendar.filename
          const blob = await this.doDownload(filename)
          await this.doImport(filename, blob, "next")
        }
      } else {
        if (nextAlready) {
          await this.doDelete(nextImportId, "next")
        }
      }
    }catch(error){
      if(error.status === 401){
        this.addAlert('Session has timed out, please relaunch the tool. Error: ' + error.status, 'error')
      }else if(error.status === 500){
        this.addAlert('Network request failed. Error: ' + error.status, 'error')
      }else{
        this.addAlert('Failed to get data, status: ' + error, 'error')
      }
    }finally{
      this.setState(({currentRunning: false, nextRunning: false}))
    }
  }

  doHide = async (id) => {
    const {calendarServer} = this.props
    await this.fetch(calendarServer + "/api/imports/" + id + "/hide", {
      method: 'POST'
    }).catch(reason => {
      this.addAlert('Failed to hide import', 'warning')
      // we don't throw the error as we can continue (although further things will go wrong).
    })
  }


  doDelete = async (id, calendar) => {
    const {calendarServer} = this.props
    this.setCalendarState(calendar, true)
    await this.fetch(calendarServer + "/api/imports/" + id, {
      method: 'DELETE'
    }).then((response) => {
      if(!response.ok){
        throw new CalendarError(response.status)
      }
    })
    this.doPoll(id, calendar)
  }

  doImport = async (filename, blob, calendar) => {
    const {calendarServer} = this.props
    this.setCalendarState(calendar, true)
    const formData = new FormData();
    formData.append("file", blob, filename)

    await this.fetch(calendarServer + "/api/run", {
      body: formData,
      method: 'POST'
    }).then((response) => {
      if(!response.ok){
        throw new CalendarError(response.status)
      }
      return response.json()
    }).then((json) => {
      return this.doPoll(json.id, calendar)
    })
  }

  doPoll = async (id, calendar) => {
    const {calendarServer} = this.props
    return await this.fetch(calendarServer + "/api/imports/" + id).then((response) => {
      if(!response.ok){
        throw new CalendarError(response.status)
      }
      return response.json()
    }).then((json) => {
      const {calendarImport} = json
      const newState = {}
      newState[calendar + "Import"] = calendarImport
      newState[calendar + "ImportId"] = id
      newState[calendar] = !calendarImport.delete
      this.setState(newState)
      const running = (calendarImport.delete) ?
          this.isRunning(calendarImport.delete.status) :
          this.isRunning(calendarImport.load.status)
      if (running) {
        setTimeout(() => this.doPoll(id, calendar), 5000)
      } else {
        this.addAlert("Update of "+ calendar+ " year calendar complete.", 'success')
      }
    }).finally(() => {
      this.setCalendarState(calendar, false)
    })
  }

  setCalendarState = (calendar, running) => {
    const newState = {}
    newState[calendar + "Running"] = running
    this.setState(newState)
  }

  doDownload = async (filename) => {
    const {calendarServer} = this.props
    return await this.fetch(calendarServer + "/api/predefined/" + filename, {
      headers: {Accept: 'text/csv'}
    }).then((response) => {
      if(!response.ok){
        throw new CalendarError(response.status)
      }
      return response.blob()
    })
  }

  addAlert = (message, variant = 'info')  => {
    this.setState((state) => ({alerts: [...state.alerts, {message, variant}]}))
  }

  renderAlerts() {
    return <View>
      {this.state.alerts.map((alert, idx) => <Alert key={idx} variant={alert.variant}>{alert.message}</Alert>)}
    </View>
  }

  renderSaveButton(running) {
    // Only enable it when a value has changed
    const disabled = running ||
        (this.state.next === !(!!this.state.nextImport.delete) && this.state.current === !(!!this.state.currentImport.delete))
    return <Button color="primary" interaction={disabled ? 'disabled' : 'enabled'} onClick={this.doSave}>
      Save
    </Button>;
  }

  render() {

    const running = this.state.nextRunning || this.state.currentRunning

    const personalCalendarLink = `${this.props.canvasUrl}/calendar?include_contexts=user_${this.props.userId}`

    return <Fragment>
      <Heading level="h1">University Terms</Heading>
      {this.renderAlerts()}
      <Text as="p">Show University Term Names and Week Numbers in my <Link target='_top' href={personalCalendarLink}>personal Canvas calendar</Link>.
        Selecting a year will import the Oxford Terms and Weeks for the academic year into your personal Canvas calendar.
        If you deselect a year this will remove the Oxford Terms and Weeks for the academic year from your personal Canvas calendar.
      </Text>
      {this.state.loading ?
          <Spinner renderTitle='Loading calendars'/> :
          <>
            <View as="div" margin="small none">
              <Checkbox label={this.state.currentCalendar.title + " (current)"} variant="toggle"
                        labelPlacement="end"
                        checked={this.state.current} disabled={running}
                        onChange={() => this.setState((state) => ({current: !state.current}))}/>
            </View>
            <View as="div" margin="small none">
              <Checkbox label={this.state.nextCalendar.title + " (next)"} variant="toggle"
                        labelPlacement="end"
                        checked={this.state.next} disabled={running}
                        onChange={() => this.setState((state) => ({next: !state.next}))}/>
            </View>
          </>
      }

      <View borderWidth='small none none none' borderColor='primary' as='div'>
        <Flex justifyItems='end' wrap='wrap'>
          <Flex.Item margin='small xx-small'>
            {running &&
                <Spinner size='x-small' renderTitle='Updating calendar'/>}
          </Flex.Item>
          <Flex.Item margin='small xx-small'>
            {this.renderSaveButton(running)}
          </Flex.Item>
          <Flex.Item>
            <Button interaction={this.props.returnUrl ? 'enabled' : 'disabled'} color="secondary"
                    margin="none small" onClick={() => window.location = this.props.returnUrl}>
              Cancel
            </Button>
          </Flex.Item>
        </Flex>
      </View>
    </Fragment>
  }

}

export default UserCalendars