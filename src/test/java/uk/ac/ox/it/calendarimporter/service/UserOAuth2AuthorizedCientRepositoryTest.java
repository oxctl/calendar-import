package uk.ac.ox.it.calendarimporter.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.service.SimpleToolConsumer;
import edu.ksu.lti.launch.service.ToolConsumer;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;

public class UserOAuth2AuthorizedCientRepositoryTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Mock private UserTokensRepository userTokensRepository;

  @Mock private HttpServletRequest request;

  private ToolConsumer toolConsumer;

  private UserOAuth2AuthorizedClientRepository clientRepo;
  private ClientRegistration testClient;

  public ClientRegistrationRepository clientRegistrationRepository() {
    testClient =
        ClientRegistration.withRegistrationId("test")
            .clientId("client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUriTemplate("{baseUrl}/oauth2/login")
            .authorizationUri("https://example.com/login/auth")
            .tokenUri("https://example.com/login/token")
            .build();

    ClientRegistrationRepository clientRegistrationRepository =
        new InMemoryClientRegistrationRepository(testClient);
    return clientRegistrationRepository;
  }

  @Before
  public void setUp() {
    clientRepo =
        new UserOAuth2AuthorizedClientRepository(
            userTokensRepository, clientRegistrationRepository());
    toolConsumer = new SimpleToolConsumer("test", "Test TC", "https://example.com");
  }

  @Test
  public void testLoadMissingRegistration() {
    Authentication auth = new TestingAuthenticationToken("test-user", "token");
    assertNull(clientRepo.loadAuthorizedClient("missing", auth, request));
  }

  @Test
  public void testLoadNoToken() {
    LtiPrincipal ltiPrincipal = new LtiPrincipal(toolConsumer, "username");
    Authentication auth = new TestingAuthenticationToken(ltiPrincipal, "token");
    assertNull(clientRepo.loadAuthorizedClient("test", auth, request));
  }

  @Test
  public void testLoadToken() {
    LtiPrincipal ltiPrincipal = new LtiPrincipal(toolConsumer, "username");
    Authentication auth = new TestingAuthenticationToken(ltiPrincipal, "token");
    OAuth2AccessToken token = Mockito.mock(OAuth2AccessToken.class);
    when(token.getTokenValue()).thenReturn("token value");

    Optional<UserTokens> userTokens =
        Optional.of(
            new UserTokens(
                "test:username", new OAuth2AuthorizedClient(testClient, "test:username", token)));
    when(userTokensRepository.findById("test:username")).thenReturn(userTokens);
    OAuth2AuthorizedClient test = clientRepo.loadAuthorizedClient("test", auth, request);
    assertNotNull(test);
  }
}
