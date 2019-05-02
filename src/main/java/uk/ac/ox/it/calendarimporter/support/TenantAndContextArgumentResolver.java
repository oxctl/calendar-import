package uk.ac.ox.it.calendarimporter.support;

import javax.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import uk.ac.ox.it.calendarimporter.beans.TenantAndContext;

public class TenantAndContextArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return (parameter.getParameterType().isAssignableFrom(TenantAndContext.class));
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    HttpServletRequest nativeRequest = webRequest.getNativeRequest(HttpServletRequest.class);
    if (nativeRequest != null) {
      String contextPath = nativeRequest.getContextPath();
      String[] split = contextPath.split("/");
      if (split.length == 4) {
        return new TenantAndContext(split[2], split[3]);
      }
    }
    return null;
  }
}
