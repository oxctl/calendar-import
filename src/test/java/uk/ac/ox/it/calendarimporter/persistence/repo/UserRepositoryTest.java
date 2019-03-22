package uk.ac.ox.it.calendarimporter.persistence.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.Optional;
import javax.persistence.PersistenceException;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository repository;

  private Tenant tenant;

  @Before
  public void setUp() throws Exception {
    tenant = new Tenant();
    tenant.setName("tenant");
    tenant.setUrl("http://example.com/");
    entityManager.persist(tenant);
  }

  @Test
  public void testFindByTenantNameAndUsername() throws Exception {
    entityManager.persist(new User(tenant, "username"));
    entityManager.flush();
    User user =
        this.repository
            .findByUsernameAndTenant_Name("username", "tenant")
            .orElseThrow(AssertionFailedError::new);
    assertEquals("tenant", user.getTenant().getName());
    assertEquals("username", user.getUsername());
    assertNotNull(user.getId());
  }

  @Test
  public void testTenentIsolation() throws Exception {
    Tenant other = new Tenant();
    other.setName("other");
    other.setUrl("http://example.com");
    entityManager.persist(other);
    entityManager.persist(new User(tenant, "username"));
    entityManager.persist(new User(other, "username"));

    {
      User user =
          repository
              .findByUsernameAndTenant_Name("username", "tenant")
              .orElseThrow(AssertionFailedError::new);
      assertEquals("tenant", user.getTenant().getName());
      assertEquals("username", user.getUsername());
    }
    {
      User user =
          repository
              .findByUsernameAndTenant_Name("username", "other")
              .orElseThrow(AssertionFailedError::new);
      assertEquals("other", user.getTenant().getName());
      assertEquals("username", user.getUsername());
    }
  }

  @Test
  public void testFindByOAuth2AuthenticationToken() throws Exception {
    entityManager.persist(new User(tenant, "username"));
    entityManager.flush();
    OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
    when(token.getAuthorizedClientRegistrationId()).thenReturn("tenant");
    when(token.getName()).thenReturn("username");
    User user =
        repository.findByOAuth2AuthenticationToken(token).orElseThrow(AssertionFailedError::new);
    assertEquals("tenant", user.getTenant().getName());
    assertEquals("username", user.getUsername());
    assertNotNull(user.getId());
  }

  @Test
  public void testNotFound() throws Exception {
    Optional<User> missing = this.repository.findByUsernameAndTenant_Name("noUser", "badTenant");
    assertFalse(missing.isPresent());
  }

  @Test(expected = PersistenceException.class)
  public void testNoDuplicates() throws Exception {
    // Check index prevents duplicates
    entityManager.persist(new User(tenant, "username"));
    entityManager.persist(new User(tenant, "username"));
    entityManager.flush();
  }

  @Test
  public void testUpdateUser() throws Exception {
    entityManager.persist(new User(tenant, "username"));
    entityManager.flush();
    {
      User user =
          this.repository
              .findByUsernameAndTenant_Name("username", "tenant")
              .orElseThrow(AssertionFailedError::new);
      assertNotNull(user.getId());
      user.setEmail("email@example.com");
      user.setName("Display Name");
      this.repository.save(user);
    }
    {
      User user =
          this.repository
              .findByUsernameAndTenant_Name("username", "tenant")
              .orElseThrow(AssertionFailedError::new);
      assertEquals("email@example.com", user.getEmail());
      assertEquals("Display Name", user.getName());
    }
  }
}
