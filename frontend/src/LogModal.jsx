import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading, Modal, Spinner } from '@instructure/ui'
import { fetchTextFileWithToken } from './utils/fetch'

export const LogModal = ({ title, url, token, onDismiss }) => {
  const [log, setLog] = useState({ loading: true, content: '', error: '' })

  useEffect(() => {
    const controller = new AbortController()
    setLog({ loading: true, content: '', error: '' })
    fetchTextFileWithToken(url, token, controller.signal).then(content => {
      if (!controller.signal.aborted) {
        setLog({ loading: false, content, error: '' })
      }
    }).catch(error => {
      if (!controller.signal.aborted) {
        setLog({ loading: false, content: '', error: `Failed to display log: ${error.message}` })
      }
    })
    return () => controller.abort()
  }, [url, token])

  return <Modal open onDismiss={onDismiss} label={title || 'Logfile'} size='large'>
    <Modal.Header><Heading>{title || 'Logfile'}</Heading></Modal.Header>
    <Modal.Body>
      {log.loading
        ? <Spinner renderTitle='Loading logfile'/>
        : log.error
          ? <Alert variant='error'>{log.error}</Alert>
          : <pre style={{fontFamily: 'monospace', whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', margin: 0}}>{log.content || 'No log content available.'}</pre>}
    </Modal.Body>
    <Modal.Footer>
      <Button onClick={onDismiss}>Close</Button>
    </Modal.Footer>
  </Modal>
}

export default LogModal
