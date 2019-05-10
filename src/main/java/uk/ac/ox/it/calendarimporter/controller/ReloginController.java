package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import uk.ac.ox.it.calendarimporter.service.UserOAuth2AuthorizedClientRepository;

@Controller
@RequestMapping("/app/")
public class ReloginController {

  private final UserOAuth2AuthorizedClientRepository clientRepository;

  public ReloginController(UserOAuth2AuthorizedClientRepository clientRepository) {
    this.clientRepository = clientRepository;
  }

  /**
   * This is used to force a relogin to occur, this is useful when a user has revoked their tokens
   * in Canvas but we still hold them and don't yet know they aren't valid. This is used by the
   * sections loader to get a new token if it fails toe load the sections.
   *
   * @param client The client.
   * @param ltiAuthenticationToken Our LTI Authentication.
   * @param httpServletRequest The current request (not actually used).
   * @param response The current response (not actuall used).
   * @return A redirect so that the normal rules about requiring a OAuth2 client kick in.
   */
  @PostMapping("relogin")
  public ModelAndView relogin(
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      LtiAuthenticationToken ltiAuthenticationToken,
      HttpServletRequest httpServletRequest,
      HttpServletResponse response) {
    clientRepository.removeAuthorizedClient(
        client.getClientRegistration().getRegistrationId(),
        ltiAuthenticationToken,
        httpServletRequest,
        response);
    return new ModelAndView("redirect:.");
  }
}
