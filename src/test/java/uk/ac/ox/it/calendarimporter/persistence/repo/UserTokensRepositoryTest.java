package uk.ac.ox.it.calendarimporter.persistence.repo;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens.AccessToken;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens.RefreshToken;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserTokensRepositoryTest {

  public static final String PRINCIPAL = "tenant:principal";
  @Autowired private EntityManager entityManager;

  @Autowired private UserTokensRepository repository;

  @Test
  public void testSaveLoadEmpty() {
    {
      UserTokens userTokens = new UserTokens();
      userTokens.setPrincipal(PRINCIPAL);
      entityManager.persist(userTokens);
      entityManager.flush();
    }
    {
      UserTokens userTokens =
          repository
              .findById(PRINCIPAL)
              .orElseThrow(() -> new AssertionError("Failed to find UserTokens"));
      assertNull(userTokens.getAccessToken());
      assertNull(userTokens.getRefreshToken());
    }
  }

  @Test
  public void testSaveLoad() {
    Instant issued = Instant.now();
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    {
      UserTokens userTokens = new UserTokens();
      userTokens.setPrincipal(PRINCIPAL);
      AccessToken accessToken = new AccessToken("value", issued, expires, "scope1 scope2");
      userTokens.setAccessToken(accessToken);
      entityManager.persist(userTokens);
      entityManager.flush();
    }
    {
      UserTokens userTokens =
          repository
              .findById(PRINCIPAL)
              .orElseThrow(() -> new AssertionError("Failed to find UserTokens"));
      RefreshToken refreshToken = userTokens.getRefreshToken();
      AccessToken accessToken = userTokens.getAccessToken();
      assertNotNull(accessToken);
      assertEquals("value", accessToken.getTokenValue());
      assertEquals(issued, accessToken.getIssuedAt());
      assertEquals(expires, accessToken.getExpiresAt());
      assertEquals("scope1 scope2", accessToken.getScopes());
      assertNull(refreshToken);
    }
  }

  @Test(expected = ConstraintViolationException.class)
  public void testSaveMissingFields() {
    UserTokens userTokens = new UserTokens();
    userTokens.setPrincipal(PRINCIPAL);
    AccessToken accessToken = new AccessToken();
    accessToken.setTokenValue("value");
    userTokens.setAccessToken(accessToken);
    entityManager.persist(userTokens);
    entityManager.flush();
  }

  @Test
  public void testClearingToken() {
    Instant issued = Instant.now();
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    {
      UserTokens userTokens = new UserTokens();
      userTokens.setPrincipal(PRINCIPAL);
      AccessToken accessToken = new AccessToken("value", issued, expires, "scope1 scope2");
      userTokens.setAccessToken(accessToken);
      entityManager.persist(userTokens);
      entityManager.flush();
    }
    {
      UserTokens userTokens =
          repository
              .findById(PRINCIPAL)
              .orElseThrow(() -> new AssertionError("Failed to find UserTokens"));
      assertNotNull(userTokens.getAccessToken());
      userTokens.setAccessToken(null);
      entityManager.persist(userTokens);
      entityManager.flush();
    }
    {
      UserTokens userTokens =
          repository
              .findById(PRINCIPAL)
              .orElseThrow(() -> new AssertionError("Failed to find UserTokens"));
      assertNull(userTokens.getAccessToken());
    }
  }

  @Test
  public void testMultipleSaves() {
    Instant issued = Instant.now();
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    {
      UserTokens userTokens = new UserTokens();
      userTokens.setPrincipal(PRINCIPAL);
      AccessToken accessToken = new AccessToken("value", issued, expires, "scope1 scope2");
      userTokens.setAccessToken(accessToken);
      repository.save(userTokens);
    }
    {
      UserTokens userTokens = new UserTokens();
      userTokens.setPrincipal(PRINCIPAL);
      AccessToken accessToken = new AccessToken("value", issued, expires, "scope1 scope2");
      userTokens.setAccessToken(accessToken);
      repository.save(userTokens);
    }
  }
}
