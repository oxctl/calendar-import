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
  private final String context;
  private final CourseSection into;
  private final TimeZone timeZone;
}
