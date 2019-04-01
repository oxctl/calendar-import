package uk.ac.ox.it.calendarimporter.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;

/**
 * This persists the OAuth2 tokens in the DB, this means we don't have to get the user to
 * authenticate each time they use the tool.
 * This should probably be cacheable.
 */
@Component
public class UserOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

  @Autowired private UserTokensRepository userTokensRepository;

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Override
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
      String clientRegistrationId, Authentication authentication, HttpServletRequest request) {
    OAuth2AuthorizedClient oAuth2AuthorizedClient = null;
    ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
    if (clientRegistration != null) {
      String principal = authentication.getPrincipal().toString();
      oAuth2AuthorizedClient =
          userTokensRepository
              .findById(authentication.getPrincipal().toString())
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
    UserTokens userTokens = new UserTokens(authentication, authorizedClient);
    userTokensRepository.save(userTokens);
  }

  @Override
  public void removeAuthorizedClient(
      String clientRegistrationId,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    userTokensRepository.deleteById(authentication.getPrincipal().toString());
  }
}
