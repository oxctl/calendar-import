package uk.ac.ox.it.calendarimporter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportService;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {HomeController.class, BasicErrorController.class})
public class HomeControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ClientRegistrationRepository clientRegistrationRepository;

  @MockBean private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @MockBean private LtiLoginService ltiLoginService;

  @MockBean private ToolConsumerService toolConsumerService;

  @MockBean private UserRepository userRepository;

  @MockBean private ImportService importService;

  @MockBean private DepositService depositService;

  @MockBean private CalendarImportRepository calendarImportRepository;

  @Before
  public void setUp() {
    LtiSession ltiSession = new LtiSession();
    LtiLaunchData ltiLaunchData = new LtiLaunchData();
    ltiSession.setLtiLaunchData(ltiLaunchData);
    when(ltiLoginService.getLtiSession()).thenReturn(ltiSession);
    when(importService.getJobs(any(), any(), any()))
        .thenReturn(new PageImpl<>(Collections.emptyList()));
  }

  @Test
  public void testRequiresAuthentication() throws Exception {
    mvc.perform(get("/app/")).andExpect(status().isForbidden());
  }
}
