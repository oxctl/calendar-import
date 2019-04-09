/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ox.it.calendarimporter;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.WebAttributes;

/**
 * Special access denied handler for LTI tools. As there is no login page we just send the user to
 * the error page with the exception and the status. It does however attempt to detect when the user is
 * blocking cookies.
 */
public class LtiHandlerImpl implements AuthenticationEntryPoint {
  // ~ Static fields/initializers
  // =====================================================================================

  protected static final Log logger = LogFactory.getLog(LtiHandlerImpl.class);

  // ~ Instance fields
  // ================================================================================================

  private String errorPage;

  // ~ Methods
  // ========================================================================================================

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    if (!response.isCommitted()) {
      if (errorPage != null) {
        if (request.getParameter("login") != null) {
          request.setAttribute("javax.servlet.error.exception", new NoCookiesException("You are blocking cookies, please allow cookies.", authException));
        } else {
          // Put exception into request scope (perhaps of use to a view)
          request.setAttribute("javax.servlet.error.exception",
                  authException);
        }

        // Set the 403 status code.
        request.setAttribute("javax.servlet.error.status_code", HttpStatus.FORBIDDEN.value());

        // forward to error page.
        RequestDispatcher dispatcher = request.getRequestDispatcher(errorPage);
        dispatcher.forward(request, response);
      } else {
        response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
      }
    }
  }

  /**
   * The error page to use. Must begin with a "/" and is interpreted relative to the current context
   * root.
   *
   * @param errorPage the dispatcher path to display
   * @throws IllegalArgumentException if the argument doesn't comply with the above limitations
   */
  public void setErrorPage(String errorPage) {
    if ((errorPage != null) && !errorPage.startsWith("/")) {
      throw new IllegalArgumentException("errorPage must begin with '/'");
    }

    this.errorPage = errorPage;
  }
}
