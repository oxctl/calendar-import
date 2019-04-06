package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * If the user denies the application access to their account the OAuth2AuthorizationCodeGrantFilter just
 * passes the request through. Without this controller the user gets a 404 error. This allows us to present
 * a nicer error back to the user and allow them to try logging in.
 */
@Controller
public class OAuth2ErrorController {

    private final Logger log = LoggerFactory.getLogger(OAuth2ErrorController.class);

    // This is the same sort of cache as OAuth2AuthorizationCodeGrantFilter which is why it works.
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @GetMapping("/login/oauth2/code/{instance}")
    public void handleError(@RequestParam(value = "error", required = false) String errorCode, HttpServletRequest request, HttpServletResponse response) throws OAuth2AccessDeniedException {
        // We need a better way to get the URL to re-try the autenticate with.
        if ("access_denied".equals(errorCode)) {
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String redirectUrl = (savedRequest != null)?savedRequest.getRedirectUrl(): null;
            throw new OAuth2AccessDeniedException("You must allow access to this tool to use it.", redirectUrl);
        } else {
            throw new RuntimeException("Unknown error, this should not happen.");
        }

    }
}
