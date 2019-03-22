package uk.ac.ox.it.calendarimporter.persistence.model;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;
import static uk.ac.ox.it.calendarimporter.persistence.model.UserTokens.AccessToken;
import static uk.ac.ox.it.calendarimporter.persistence.model.UserTokens.RefreshToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

public class UserTokensTest {

  @Test
  public void testConvertRefresh() {
    Instant issuedAt = Instant.now();
    OAuth2RefreshToken oauth2RefreshToken = new OAuth2RefreshToken("value", issuedAt);
    RefreshToken refreshToken = new RefreshToken(oauth2RefreshToken);
    OAuth2RefreshToken converted = refreshToken.toOAuth2RefreshToken();
    assertEquals(oauth2RefreshToken, converted);
  }

  @Test
  public void testConvertAccess() {
    Instant issuedAt = Instant.now();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    Set<String> scope = Collections.singleton("scope");
    OAuth2AccessToken oauth2AccessToken =
        new OAuth2AccessToken(BEARER, "value", issuedAt, expiresAt, scope);
    AccessToken accessToken = new AccessToken(oauth2AccessToken);
    OAuth2AccessToken converted = accessToken.toOAuth2AccessToken();
    assertEquals(oauth2AccessToken, converted);
  }

  @Test
  public void testConvertAccessMultipleScopes() {
    Instant issuedAt = Instant.now();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    Set<String> scopes = new HashSet<>(Arrays.asList("scope1", "scope2"));
    OAuth2AccessToken oauth2AccessToken =
        new OAuth2AccessToken(BEARER, "value", issuedAt, expiresAt, scopes);
    AccessToken accessToken = new AccessToken(oauth2AccessToken);
    OAuth2AccessToken converted = accessToken.toOAuth2AccessToken();
    assertEquals(oauth2AccessToken, converted);
    assertEquals(2, converted.getScopes().size());
  }

  @Test
  public void testConvertAccessNoScopes() {
    Instant issuedAt = Instant.now();
    Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
    Set<String> scopes = Collections.emptySet();
    OAuth2AccessToken oauth2AccessToken =
        new OAuth2AccessToken(BEARER, "value", issuedAt, expiresAt, scopes);
    AccessToken accessToken = new AccessToken(oauth2AccessToken);
    OAuth2AccessToken converted = accessToken.toOAuth2AccessToken();
    assertEquals(oauth2AccessToken, converted);
    assertEquals(0, converted.getScopes().size());
  }
}
