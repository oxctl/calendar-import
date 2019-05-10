package uk.ac.ox.it.calendarimporter.service;

import java.util.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import uk.ac.ox.it.calendarimporter.controller.ImportType;
import uk.ac.ox.it.calendarimporter.controller.pojo.CourseSection;

@AllArgsConstructor
@Builder
@Getter
public class ImportConfig {

  private final ImportType type;
  private final String url;
  private final String filename;
  private final OAuth2AuthorizedClient client;
  private final Long userId;
  /**
   * The context in which the import is to be done. Typically this is the course although in the
   * future we may wish to support importing into a user's calendar. Example: course_123.
   */
  private final String context;

  private final CourseSection into;
  private final TimeZone timeZone;
}
