package uk.ac.ox.it.calendarimporter;

import java.util.Map;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;
import uk.ac.ox.it.calendarimporter.controller.OAuth2AccessDeniedException;

public class CustomErrorAttributes extends DefaultErrorAttributes {

  public CustomErrorAttributes(boolean includeException) {
    super(includeException);
  }

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
    Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, includeStackTrace);
    Throwable error = getError(webRequest);
    if (error instanceof OAuth2AccessDeniedException) {
      String reloginUrl = ((OAuth2AccessDeniedException) error).getReloginUrl();
      if (reloginUrl != null) {
        errorAttributes.put("reloginUrl", reloginUrl);
      }
    }
    return errorAttributes;
  }
}
