import React from "react"
import {Flex} from "@instructure/ui-flex";
import {Button, IconButton} from "@instructure/ui-buttons";
import {View} from "@instructure/ui-view";
import {Heading} from "@instructure/ui-heading";
import {Text} from "@instructure/ui-text";
import {
    IconCalendarMonthLine,
    IconImportLine,
    IconMsExcelLine,
    IconQuestionLine,
    IconResetLine,
    IconTrashLine
} from "@instructure/ui-icons";
import {getRelativeTime} from "./relativeTime";
import {Link} from "@instructure/ui-link";
import {Spinner} from "@instructure/ui-spinner";
import {load, setPage} from "./actions/imports";
import {connect} from "react-redux";
import {Pagination} from "@instructure/ui-pagination";

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
                                {calendarImport.user.name} imported <Link
                                href={`${this.props.server}/api/download/${id}?access_token=${this.props.token}`}>
                                {calendarImport.filename}</Link> into {calendarImport.destinationName ? 'the section: ' + calendarImport.destinationName : 'the course'}
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
                                    <Text weight='bold'>Logfile:</Text> <Link target='_blank'
                                                                              href={`${this.props.server}/api/log/${id}/load?access_token=${this.props.token}`}>logfile</Link>
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
                                    <Text weight='bold'>Logfile:</Text> <Link target='_blank'
                                                                              href={`${this.props.server}/api/log/${id}/delete?access_token=${this.props.token}`}>logfile</Link>
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