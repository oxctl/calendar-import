package uk.ac.ox.it.calendarimporter.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.security.test.context.support.WithMockLtiUser;
import uk.ac.ox.it.calendarimporter.service.CanvasApiCreator;

@RunWith(SpringRunner.class)
@WebMvcTest(SectionsController.class)
@TestPropertySource(locations = "classpath:test.properties")
public class SectionsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OAuth2AuthorizedClientRepository oauth2Repository;

  @MockBean private ClientRegistrationRepository clientRepository;

  @MockBean private LtiLoginService ltiLoginService;

  @MockBean private ToolConsumerService toolConsumerService;

  @MockBean private TenantRepository tenantRepository;

  @MockBean private CanvasApiCreator canvasApiCreator;

  @Test
  @WithMockLtiUser(username = "canvas", roles = "LTI_USER")
  public void testGetSectionsOne() throws Exception {

    LtiSession session = new LtiSession();
    session.setApplicationName("instance");
    session.setCanvasCourseId("1");
    when(ltiLoginService.getLtiSession()).thenReturn(session);

    OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);

    when(oauth2Repository.loadAuthorizedClient(eq("instance"), any(), any())).thenReturn(client);
    Tenant tenant = new Tenant();
    tenant.setUrl("https://example.com");
    when(tenantRepository.findByName("instance")).thenReturn(Optional.of(tenant));

    CanvasApiFactory factory = mock(CanvasApiFactory.class);
    SectionReader reader = mock(SectionReader.class);
    OauthToken token = mock(OauthToken.class);
    when(canvasApiCreator.getToken(any(), eq(client))).thenReturn(token);
    when(canvasApiCreator.getInstance(anyString())).thenReturn(factory);
    when(factory.getReader(SectionReader.class, token)).thenReturn(reader);

    Section section = new Section();
    section.setId((long) 1);
    section.setName("Example Section");
    when(reader.listCourseSections("1", Collections.emptyList()))
        .thenReturn(Collections.singletonList(section));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/app/sections").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[{'sectionId': 'course_section_1', 'name': 'Example Section'}]"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
  }

  @Test
  @WithMockLtiUser(username = "canvas", roles = "LTI_USER")
  public void testGetSectionsTwo() throws Exception {

    LtiSession session = new LtiSession();
    session.setApplicationName("instance");
    session.setCanvasCourseId("1");
    when(ltiLoginService.getLtiSession()).thenReturn(session);

    OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);

    when(oauth2Repository.loadAuthorizedClient(eq("instance"), any(), any())).thenReturn(client);
    Tenant tenant = new Tenant();
    tenant.setUrl("https://example.com");
    when(tenantRepository.findByName("instance")).thenReturn(Optional.of(tenant));

    CanvasApiFactory factory = mock(CanvasApiFactory.class);
    SectionReader reader = mock(SectionReader.class);
    OauthToken token = mock(OauthToken.class);
    when(canvasApiCreator.getToken(any(), eq(client))).thenReturn(token);
    when(canvasApiCreator.getInstance(anyString())).thenReturn(factory);
    when(factory.getReader(SectionReader.class, token)).thenReturn(reader);

    List<Section> sections = new ArrayList<>();
    {
      Section section = new Section();
      section.setId((long) 1);
      section.setName("Example Section");
      sections.add(section);
    }
    {
      Section section = new Section();
      section.setId((long) 2);
      section.setName("Example Section");
      sections.add(section);
    }

    when(reader.listCourseSections("1", Collections.emptyList())).thenReturn(sections);

    mockMvc
        .perform(MockMvcRequestBuilders.get("/app/sections").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content().json("[{'sectionId': 'course_section_1'},{'sectionId': 'course_section_2'}]"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
  }

  @Test
  @WithMockLtiUser(username = "canvas", roles = "LTI_USER")
  public void testGetSectionsEmpty() throws Exception {

    LtiSession session = new LtiSession();
    session.setApplicationName("instance");
    session.setCanvasCourseId("1");
    when(ltiLoginService.getLtiSession()).thenReturn(session);

    OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);

    when(oauth2Repository.loadAuthorizedClient(eq("instance"), any(), any())).thenReturn(client);
    Tenant tenant = new Tenant();
    tenant.setUrl("https://example.com");
    when(tenantRepository.findByName("instance")).thenReturn(Optional.of(tenant));

    CanvasApiFactory factory = mock(CanvasApiFactory.class);
    SectionReader reader = mock(SectionReader.class);
    OauthToken token = mock(OauthToken.class);
    when(canvasApiCreator.getToken(any(), eq(client))).thenReturn(token);
    when(canvasApiCreator.getInstance(anyString())).thenReturn(factory);
    when(factory.getReader(SectionReader.class, token)).thenReturn(reader);

    when(reader.listCourseSections("1", Collections.emptyList()))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(MockMvcRequestBuilders.get("/app/sections").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
  }

  @Test
  public void testGetSectionsNoAuth() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/app/sections").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(403));
  }
}
