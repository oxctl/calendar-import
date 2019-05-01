package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(ConfigController.class)
public class ConfigControllerTest {

    private Map<String, String> namespaces = Map.of(
            "blti", "http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuth2AuthorizedClientRepository oauth2Repository;

    @MockBean
    private ClientRegistrationRepository clientRepository;

    @MockBean
    private LtiLoginService ltiLoginService;

    @MockBean
    private ToolConsumerService toolConsumerService;

    @Test
    public void testSimple() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/config.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(xpath("//blti:title", namespaces, null).string("Calendar Import"))
                .andExpect(xpath("//blti:launch_url", namespaces, null).string("http://localhost/launch"))
        ;
    }
}
