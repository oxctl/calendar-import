package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.support.RequestContextUtils;
import uk.ac.ox.it.calendarimporter.controller.pojo.Alert;

import javax.servlet.http.HttpServletRequest;

/**
 * Exceptions in uploads aren't yet mapped to a controller so we can't do this just in the controller that needs it.
 * At the moment it's only the HomeController that needs this.
 */
@ControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleException(HttpServletRequest request, MaxUploadSizeExceededException e, RedirectAttributesModelMap redirectAttributes, LtiSession ltiSession) {
        // TODO This currently means we will also wrongly handle API methods that are too large incorrectly
        redirectAttributes.addFlashAttribute("alert", new Alert(Alert.Type.ERROR, "Upload is too large."));
        RequestContextUtils.getOutputFlashMap(request).put("message", new Alert(Alert.Type.ERROR, "Upload is too large, "));
        // Need to get error page.

        return "redirect:"+ ltiSession.getInitialViewPath();
    }
}
