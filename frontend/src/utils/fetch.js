/**
 * Handle generic errors when performing a fetch from the proxy.
 *
 * @param response The response to the fetch.
 * @returns {{ok}|*|Promise<T>}
 */
export const handleErrors = async (response) => {
  if (!response.ok) {
    if (response.status === 403) {
      // This will happen the first time someone uses the tool.
      throw new LoginError('Proxy doesn\'t have token for user')
    } else if (response.status === 401) {
      const authHeader = response.headers.get('WWW-Authenticate')
      if (authHeader) {
        const headerData = parseAuthHeader(authHeader);
        if (headerData.realm !== 'proxy') {
          // This will typically happen when someone has deleted their token
          throw new LoginError('Your token isn\'t valid any more')
        }
        if (headerData.error === 'invalid_token') {
          throw new Error(headerData.error_description || 'Your session has expired, please try relaunching the tool');
        }
      }
      // If there's no auth header look in the JSON
      return response.json().then(error => {
        if (error.status === 'unauthorized') {
          // Attempting an operation for something they don't have access to.
          throw new UnauthorizedError()
        } else {
          // This is probably because you need to renew you're token because the scopes have changed.
          throw new LoginError('Token isn\'t valid for this operation.')
        }
      })
    } else if (response.status === 400) {
      const err = await response.text()
      console.error(err)
      throw new Error(`${response.status} error`)
    } else {
      throw new Error(`${response.status} error`)
    }
  }
  return response
}

function parseAuthHeader(authHeader) {
  const parts = authHeader.split(',').map(part => part.trim());
  const parsed = {};

  parts.forEach(part => {
    const match = part.match(/(\w+)="([^"]+)"/);
    if (match) {
      parsed[match[1]] = match[2];
    }
  });

  return parsed;
}

/**
 * Checks to see that we had an ok response and throws an error if not.
 * @param response The response.
 */
export const checkOK = (response) => {
  if (!response.ok) {
    const {statusMessage, status} = response
    throw new Error(`Request failed (${status}): ${statusMessage}`)
  }
  return response
}

/**
 * A custom error type for failures that should be resolved by an OAuth Login.
 * This is useful so that we can send the user to grant access and not display an error.
 */
export class LoginError extends Error {
  constructor(message) {
    super(message);
    this.name = 'Login Error';
  }
}

/**
 * A custom error type for failures that are because the user doesn't have permission.
 * This is useful so that we can display an error about the operation.
 */
export class UnauthorizedError extends Error {
  constructor(message) {
    super(message);
    this.name = 'Unauthorized Error';
  }
}

/**
 * Download a file from the server using a Bearer token in the Authorization header.
 * 
 * @param {string} url - The API URL to fetch from
 * @param {string} token - The Bearer token for authentication
 * @param {string} filename - The filename to save as (optional, will use response header if not provided)
 * @returns {Promise<void>}
 */
export const downloadWithToken = async (url, token, filename = null) => {
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      throw new Error(`Download failed with status ${response.status}`)
    }

    // Get filename from Content-Disposition header if not provided
    let downloadFilename = filename
    if (!downloadFilename) {
      const contentDisposition = response.headers.get('content-disposition')
      if (contentDisposition) {
        const matches = contentDisposition.match(/filename="?([^"]+)"?/)
        if (matches) {
          downloadFilename = matches[1]
        }
      }
    }

    // Convert response to blob and trigger download
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = downloadFilename || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
  } catch (error) {
    console.error('Download error:', error)
    throw error
  }
}

/**
 * Display a file from the server in the browser (new tab/window) using a Bearer token in the Authorization header.
 * This is useful for displaying log files and other text content.
 * 
 * @param {string} url - The API URL to fetch from
 * @param {string} token - The Bearer token for authentication
 * @returns {Promise<void>}
 */
export const displayFileInBrowser = async (url, token) => {
  // Open synchronously to avoid popup blockers
  const newWindow = window.open('', '_blank', 'noopener,noreferrer')
  if (!newWindow) {
    throw new Error('Popup was blocked')
  }

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (!response.ok) {
      throw new Error(`Failed to fetch file with status ${response.status}`)
    }

    const blob = await response.blob()
    const displayUrl = window.URL.createObjectURL(blob)
    newWindow.location.href = displayUrl

    // Revoke when the window is closed (best-effort)
    const timer = window.setInterval(() => {
      if (newWindow.closed) {
        window.clearInterval(timer)
        window.URL.revokeObjectURL(displayUrl)
      }
    }, 1000)
  } catch (error) {
    newWindow.close()
    console.error('Display error:', error)
    throw error
  }
}