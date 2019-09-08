package uk.ac.ox.it.calendarimporter;

import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * This configures Spring Session to just use a concurrent hash map to store the sessions. The
 * reason for using this is so that we can have fine control over the path of the cookie that we
 * set. This allows the a tool to have a different session when launched from different
 * instance/courses.
 *
 * <p>We want to have different sessions for each course as the user may have a different role in
 * each course and the skin may be different in each course.
 *
 * <p>This setup is missing sessions events at the moment so there's no indication when the session
 * gets destroyed.
 */
@Configuration
@EnableScheduling
@EnableSpringHttpSession
@Slf4j
public class SessionConfig extends AbstractHttpSessionApplicationInitializer {

  private final ConcurrentHashMap<String, Session> sessions;

  @Autowired(required = false)
  private ServletContext servletContext;

  public SessionConfig() {
    sessions = new ConcurrentHashMap<>();
  }

  @Bean
  public MapSessionRepository sessionRepository() {
    return new MapSessionRepository(sessions);
  }

  @Bean
  public Runnable cleanupTask() {
    return new Runnable() {
      @Override
      @Scheduled(fixedDelay = 60000)
      public void run() {
        sessions.forEach(
            (key, value) -> {
              if ((value.isExpired())) {
                sessions.remove(key);
              }
            });
      }
    };
  }

  @Bean
  public CustomPathCookieSerializer cookieSerializer() {
    CustomPathCookieSerializer cookieSerializer = new CustomPathCookieSerializer();
    // If we try to use the samesite value to protect from CSRF attacks then when we set the cookie in the redirect
    // response to the POST lti launch it doesn't get sent with the following GET which makes it appear that cookies
    // are blocked.
    // When all browser support "None" (safari doesn't at the moment) we will want to switch to that as Chrome is going
    // to make Lax the default.
    cookieSerializer.setSameSite(null);
    if (this.servletContext != null) {
      SessionCookieConfig sessionCookieConfig = null;
      try {
        sessionCookieConfig = this.servletContext.getSessionCookieConfig();
      } catch (UnsupportedOperationException ex) {
        log.warn("Unable to obtain SessionCookieConfig: " + ex.getMessage());
      }
      if (sessionCookieConfig != null) {
        if (sessionCookieConfig.getName() != null) {
          cookieSerializer.setCookieName(sessionCookieConfig.getName());
        }
        if (sessionCookieConfig.getDomain() != null) {
          cookieSerializer.setDomainName(sessionCookieConfig.getDomain());
        }
        if (sessionCookieConfig.getPath() != null) {
          cookieSerializer.setCookiePath(sessionCookieConfig.getPath());
        }
        if (sessionCookieConfig.getMaxAge() != -1) {
          cookieSerializer.setCookieMaxAge(sessionCookieConfig.getMaxAge());
        }
      }
    }
    return cookieSerializer;
  }
}
