package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.oauth.*;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;

/**
 * We want to store refreshed tokens. This has the problem that if we allow tokens to be used from
 * live on beta/test. Then when the tokens get updated live will break.
 */
@Service
public class CanvasApiCreator {

  public static final String PROTOCOL_SEP = "://";

  @Autowired private UserTokensRepository userTokensRepository;

  /**
   * Get a new CanvasApiFactory.
   *
   * @param url The URL of the Canvas instance.
   * @return A new CanvasApiFactory.
   */
  public CanvasApiFactory getInstance(String url) {
    return new CanvasApiFactory(url);
  }

  public OauthTokenRefresher getRefresher(
      String principal, String clientId, String clientSecret, String url) {
    return new StoredOauthTokenRefresher(principal, clientId, clientSecret, url);
  }

  public OauthToken getToken(Authentication authentication, OAuth2AuthorizedClient client) {
    ClientRegistration registration = client.getClientRegistration();
    if (client.getRefreshToken() != null && client.getRefreshToken().getTokenValue() != null) {
      // Need to extract
      String tokenUri = registration.getProviderDetails().getTokenUri();
      String url = removeLocalPart(tokenUri);
      StoredOauthTokenRefresher refresher =
          new StoredOauthTokenRefresher(
              authentication.getPrincipal().toString(),
              registration.getClientId(),
              registration.getClientSecret(),
              url);
      return new RefreshableOauthToken(
          refresher,
          client.getRefreshToken().getTokenValue(),
          client.getAccessToken().getTokenValue());
    }
    return new NonRefreshableOauthToken(client.getAccessToken().getTokenValue());
  }

  public OauthToken getToken(
      Tenant tenant, String principal, String access_token, String refresh_token) {
    if (refresh_token != null) {
      StoredOauthTokenRefresher refresher =
          new StoredOauthTokenRefresher(
              principal, tenant.getOauth2Id(), tenant.getOauth2Secret(), tenant.getUrl());
      return new RefreshableOauthToken(refresher, refresh_token, access_token);
    } else {
      return new NonRefreshableOauthToken(access_token);
    }
  }

  /**
   * Just removes the local part from a URL. This is just needed so we don't need more
   * configuration. In the long run we should update the canvas-api library to take the full token
   * URL.
   *
   * @param url The full URL for the OAuth token refresh.
   * @return The URL without a local part (eg just return protocol, hostname and port).
   */
  String removeLocalPart(String url) {
    int hostnameStart = url.indexOf(PROTOCOL_SEP);
    if (hostnameStart == -1) {
      throw new IllegalArgumentException("Failed to find " + PROTOCOL_SEP + " in " + url);
    }
    int endHostname = url.indexOf("/", hostnameStart + PROTOCOL_SEP.length());
    if (endHostname == -1) {
      return url;
    }
    return url.substring(0, endHostname);
  }

  /** This refresher updates the stored tokens when we get a new one. */
  public class StoredOauthTokenRefresher extends OauthTokenRefresher {

    private String principal;

    public StoredOauthTokenRefresher(
        String principal, String clientId, String clientSecret, String canvasUrl) {
      super(clientId, clientSecret, canvasUrl);
      this.principal = principal;
    }

    @Override
    public TokenRefreshResponse getNewToken(String refreshToken) throws IOException {
      TokenRefreshResponse newToken = super.getNewToken(refreshToken);
      if (newToken != null) {
        //
        Optional<UserTokens> optUserTokens = userTokensRepository.findById(principal);
        if (optUserTokens.isPresent()) {
          UserTokens userTokens = optUserTokens.get();
          Instant expires = Instant.now().plus(newToken.getExpiresIn(), ChronoUnit.SECONDS);
          Instant issued = Instant.now();
          newToken.getAccessToken();
          UserTokens.AccessToken accessToken = userTokens.getAccessToken();
          accessToken.setTokenValue(newToken.getAccessToken());
          accessToken.setExpiresAt(expires);
          accessToken.setIssuedAt(issued);
          userTokensRepository.save(userTokens);
        }
        // Update in DB.
      }
      return newToken;
    }
  }
}
