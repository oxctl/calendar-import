package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import javax.persistence.PersistenceException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository repository;

  private Tenant tenant;

  @BeforeEach
  public void setUp() {
    tenant = new Tenant();
    tenant.setName("tenant");
    tenant.setUrl("http://example.com/");
    entityManager.persist(tenant);
  }

  @Test
  public void testFindByTenantNameAndUsername() {
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
  public void testTenentIsolation() {
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
  public void testNotFound() {
    Optional<User> missing = this.repository.findByUsernameAndTenant_Name("noUser", "badTenant");
    assertFalse(missing.isPresent());
  }

  @Test
  public void testNoDuplicates() {
     assertThrows(PersistenceException.class, () -> {
       // Check index prevents duplicates
       entityManager.persist(new User(tenant, "username"));
       entityManager.persist(new User(tenant, "username"));
       entityManager.flush();
     });
  }

  @Test
  public void testUpdateUser() {
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
