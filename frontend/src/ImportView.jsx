import React from 'react'
import {
    Button,
    Flex,
    Heading,
    IconButton,
    IconCalendarMonthLine,
    IconImportLine,
    IconMsExcelLine,
    IconQuestionLine,
    IconResetLine,
    IconTrashLine,
    Link,
    Pagination,
    Spinner,
    Text,
    View
} from '@instructure/ui'
import { getRelativeTime } from './relativeTime'
import { load, setPage } from './actions/imports'
import { connect } from 'react-redux'
import { downloadWithToken, displayFileInBrowser } from './utils/fetch'

class ImportView extends React.Component {

    componentDidMount() {
        this.props.load()
    }

    reload = () => {
        this.props.load()
    }

    handleDelete = async (id) => {
        if (window.confirm('This will remove all events in the calendar created by this import, continue?')) {
            const response = await fetch(`${this.props.server}/api/imports/${id}`, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json',
                    'Authorization': 'Bearer ' + this.props.token
                }
            })
            if (response.ok) {
                this.props.onMessage({type: 'info', text: 'Delete of imported started'})
                this.props.load()
            }
        }
    }

    handleDownloadFile = async (id, filename) => {
        try {
            await downloadWithToken(`${this.props.server}/api/download/${id}`, this.props.token, filename)
        } catch (error) {
            this.props.onMessage({type: 'error', text: `Failed to download file: ${error.message}`})
        }
    }

    handleDownloadLog = async (id, type) => {
        try {
            await displayFileInBrowser(`${this.props.server}/api/log/${id}/${type}`, this.props.token)
        } catch (error) {
            this.props.onMessage({type: 'error', text: `Failed to display log: ${error.message}`})
        }
    }

    renderIcon = (type) => {
        switch (type) {
            case 'CSV':
                return <IconMsExcelLine/>
            case 'ICAL':
                return <IconCalendarMonthLine/>
            default:
                return <IconQuestionLine/>
        }
    }

    renderAgo = (isoDateTime) => {
        return getRelativeTime(Date.parse(isoDateTime))
    }

    renderStatus = (status) => {
        return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    }

    renderDelete = (id, calendarImport) => {
        // Only render when import is stopped and if delete hasn't run ok
        const deleteStatus = calendarImport.delete?.status;
        const importStatus = calendarImport.load?.status;

        const deleteNotRun = !deleteStatus || deleteStatus.status === 'FAILED'

        const importEnded = importStatus === 'COMPLETED' || importStatus === 'FAILED' ||
            importStatus === 'ERRORED' || importStatus === 'PROBLEMS'
        if (deleteNotRun && importEnded) {
            return <IconButton screenReaderLabel='Delete' renderIcon={<IconTrashLine/>}
                               onClick={() => this.handleDelete(id)}/>
        }
    }

    renderItems = () => {
        if (this.props.data?.content.length === 0) {
            return <View as='div' background='primary' margin='medium' borderWidth='small' padding='small' textAlign='center'>
                  <Heading level='h3'>No previous imports found</Heading>
              </View>
            return 
        }
        return this.props.data?.content.map(({id, calendarImport}) => {
                return <React.Fragment key={id}>
                    <View as='div' background='primary' margin='small' borderWidth='small' padding='small'>
                        {this.renderIcon(calendarImport.type)}
                        <View as='div' display='inline-block' margin='none small'>
                            <Text as='div' weight='bold'>
                                {calendarImport.user.name} imported <Link as='button' onClick={() => this.handleDownloadFile(id, calendarImport.filename)}>
                                {calendarImport.filename}</Link> into {calendarImport.destinationName ? 'the section: ' + calendarImport.destinationName : 'the calendar'}
                            </Text>
                            <Text as='div'>Created: {this.renderAgo(calendarImport.created)}</Text>
                        </View>
                    </View>
                    {calendarImport.load &&
                    <View as='div' background='primary' margin='small small small large' borderWidth='small'
                          padding='small'>
                        <Flex>
                            <Flex.Item>
                                <IconImportLine/>
                            </Flex.Item>
                            <Flex.Item as='div' margin='none small' shouldGrow shouldShrink>
                                <Text as='div' weight='bold'>Import: {this.renderStatus(calendarImport.load.status)}</Text>
                                <View as='div' margin='0 0 0 small'>
                                    <Text weight='bold'>Last message:</Text> {calendarImport.load.lastMessage}
                                </View>
                                <View as='div' margin='0 0 0 small'>
                                    <Text weight='bold'>Logfile:</Text> <Link as='button' onClick={() => this.handleDownloadLog(id, 'load')}>logfile</Link>
                                </View>
                            </Flex.Item>
                            <Flex.Item as='div' margin='none small'>
                                {this.renderDelete(id, calendarImport)}
                            </Flex.Item>
                        </Flex>
                    </View>
                    }
                    {calendarImport.delete &&
                    <View as='div' background='primary' margin='small small small large' borderWidth='small'
                          padding='small'>
                        <Flex>
                            <Flex.Item>
                                <IconTrashLine/>
                            </Flex.Item>
                            <Flex.Item as='div' margin='none small' shouldGrow shouldShrink>
                                <Text as='div'
                                      weight='bold'>Delete: {this.renderStatus(calendarImport.delete.status)}
                                </Text>
                                <View as='div' margin='0 0 0 small'>
                                    <Text weight='bold'>Last message:</Text> {calendarImport.delete.lastMessage}
                                </View>
                                <View as='div' margin='0 0 0 small'>
                                    <Text weight='bold'>Logfile:</Text> <Link as='button' onClick={() => this.handleDownloadLog(id, 'delete')}>logfile</Link>
                                </View>
                            </Flex.Item>
                        </Flex>
                    </View>
                    }
                </React.Fragment>
            }
        )
    }
    
    handlePage = (page) => {
        this.props.setPage(page)
        // We want to scroll to the top when changing pages.
        window.parent.postMessage({ subject: "lti.scrollToTop" }, "*")
    }

    renderPagination = () => {
        const pages = Array.from(Array(this.props.page.total)).map((v, i) => <Pagination.Page
            key={i}
            onClick={() => this.handlePage(i)}
            current={i === this.props.page.current}>
            {i + 1}
        </Pagination.Page>)
        return <Pagination
            as="nav"
            margin="small"
            variant="compact"
            labelNext="Next Page"
            labelPrev="Previous Page"
        >
            {pages}
        </Pagination>
    }

    renderSpinner = () => {
        return <Spinner as='div' renderTitle='Loading previous imports'/>
    }

    render() {
        return <View as='div' background='secondary' padding='small' borderWidth='small'>
            <Flex>
                <Flex.Item shouldGrow>
                    <Heading>Previous Imports</Heading>
                </Flex.Item>
                <Flex.Item>
                    <Button renderIcon={<IconResetLine/>} onClick={this.reload}>Reload</Button>
                </Flex.Item>
            </Flex>
            {this.props.loading ? this.renderSpinner() : <>{this.renderItems()}{this.renderPagination()}</>}
        </View>
    }
}

const mapStateToProps = state => {
    const {imports: {data, loading}} = state
    return {
        data,
        loading,
        page: {
            current: state.imports.page,
            total: state.imports.data?.totalPages || 0
        }
    }
}

const mapDispatchToProps = dispatch => {
    return {
        load: () => dispatch(load()),
        setPage: (page) => {
            dispatch(setPage(page));
            dispatch(load())
        },
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(ImportView)