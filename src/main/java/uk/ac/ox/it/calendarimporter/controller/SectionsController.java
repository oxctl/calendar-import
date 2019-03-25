package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import uk.ac.ox.it.calendarimporter.controller.pojo.CourseSection;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

/**
 * This allows loading of sections that are in a course. This is developed as an API so that the
 * page can load quickly and doesn't hang about waiting for an API call from Canvas to come back.
 */
@RestController
@RequestMapping("/{tenant}/{context}/")
public class SectionsController {

  private static final String PREFIX = "course_";

  @Autowired private TenantRepository tenantRepository;

  // TODO This should be cached for 5 minutes so that when the page reloads the sections doesn't
  // load again.
  @GetMapping(
      path = "sections",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public List<CourseSection> getSections(
      @PathVariable("tenant") String tenantName,
      @PathVariable("context") String context,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client)
      throws IOException {

    // TODO exception
    Tenant tenant = tenantRepository.findByName(tenantName).orElseThrow();
    CanvasApiFactory factory = new CanvasApiFactory(tenant.getUrl());
    NonRefreshableOauthToken token =
        new NonRefreshableOauthToken(client.getAccessToken().getTokenValue());

    SectionReader reader = factory.getReader(SectionReader.class, token);
    String courseId = getCourseId(context);
    List<Section> sections = reader.listCourseSections(courseId, Collections.emptyList());
    List<CourseSection> courseSections =
        sections.stream()
            .map(s -> new CourseSection("course_section_" + s.getId(), s.getName()))
            .collect(Collectors.toList());
    return courseSections;
  }

  private String getCourseId(String context) {
    if (context.startsWith(PREFIX)) {
      return context.substring(PREFIX.length());
    }
    // This should get handled by the exception mapping.
    throw new IllegalArgumentException("You can only load sections for courses.");
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public void handleException(HttpServletResponse response) throws IOException {
    response.sendError(400, "Invalid Request");
  }
}
