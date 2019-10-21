package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.lti.launch.oauth.LtiPrincipal;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;

/**
 * This persists the OAuth2 tokens in the DB, this means we don't have to get the user to
 * authenticate each time they use the tool.
 */
@Service
public class UserOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

  private final UserTokensRepository userTokensRepository;

  private final ClientRegistrationRepository clientRegistrationRepository;

  public UserOAuth2AuthorizedClientRepository(
      UserTokensRepository userTokensRepository,
      ClientRegistrationRepository clientRegistrationRepository) {
    this.userTokensRepository = userTokensRepository;
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  @Override
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
      String clientRegistrationId, Authentication authentication, HttpServletRequest request) {
    OAuth2AuthorizedClient oAuth2AuthorizedClient = null;
    ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
    if (clientRegistration != null) {
      String principal = toPrincipal(authentication);
      oAuth2AuthorizedClient =
          // Second level caching should catch this lookup by ID.
          userTokensRepository
              .findById(principal)
              .map(userTokens -> userTokens.toOAuth2AuthorizedClient(clientRegistration, principal))
              .orElse(null);
    }
    return (T) oAuth2AuthorizedClient;
  }

  @Override
  public void saveAuthorizedClient(
      OAuth2AuthorizedClient authorizedClient,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    UserTokens userTokens = new UserTokens(toPrincipal(authentication), authorizedClient);
    userTokensRepository.save(userTokens);
  }

  @Override
  public void removeAuthorizedClient(
      String clientRegistrationId,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    userTokensRepository.deleteById(toPrincipal(authentication));
  }

  private String toPrincipal(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    String name = authentication.toString();
    if ((principal instanceof Principal)) {
      Principal authPrincipal = (Principal) principal;
      name = authPrincipal.getName();
      if (authPrincipal instanceof LtiPrincipal) {
        name = ((LtiPrincipal) authPrincipal).getTenant() + ":" + name;
      }
    }
    return name;
  }
}
