package uk.ac.ox.it.calendarimporter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This is a filter that pretends with have multiple contexts without actually having to load the
 * webapp multiple times. This is needed so that a launch from one context doesn't mess with a
 * launch from another context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultiTenancyFilter extends OncePerRequestFilter {

  private static final Pattern pattern =
      Pattern.compile("^(?<contextPath>/t/[^/]+/[^/]+)(?<servletPath>.*)$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Matcher matcher = pattern.matcher(request.getServletPath());
    if (matcher.matches()) {
      final String contextPath = matcher.group("contextPath");
      final String servletPath = matcher.group("servletPath");

      if (servletPath.trim().isEmpty()) {
        response.sendRedirect(contextPath + "/");
        return;
      }

      filterChain.doFilter(
          new HttpServletRequestWrapper(request) {
            @Override
            public String getContextPath() {
              return contextPath;
            }

            @Override
            public String getServletPath() {
              return servletPath;
            }
          },
          response);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  @Override
  protected String getAlreadyFilteredAttributeName() {
    return "multiTenancyFilter" + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX;
  }
}
