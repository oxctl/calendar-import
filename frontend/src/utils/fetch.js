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
        if (!authHeader.includes('proxy')) {
          // This will typically happen when someone has deleted their token
          throw new LoginError('Your token isn\'t valid any more')
        }
        // TODO We should have better parsing of the header
        if (authHeader.includes('invalid_token')) {
          throw new Error('Your session has expired, please try relaunching the tool')
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