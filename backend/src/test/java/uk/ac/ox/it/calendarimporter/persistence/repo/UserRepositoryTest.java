package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import jakarta.validation.ConstraintViolationException;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import jakarta.persistence.PersistenceException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DirtiesContext
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository repository;

    private Tenant tenant;

    @BeforeEach
    public void setUp() {
        tenant = new Tenant();
        tenant.setName("tenant");
        tenant.setUrl("http://example.com/");
        entityManager.persist(tenant);
    }

    @Test
    public void testSaveMissingFields() {
        assertThrows(
                ConstraintViolationException.class,
                () -> {
                    repository.save(new User(null, "subject", "username"));
                    entityManager.flush();
                    entityManager.clear();
                });
    }

    @Test
    public void testSaveLoad() {
        long id;
        {
            id = repository.save(new User(tenant, "subject", "username")).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            User user =
                    this.repository
                            .findById(id)
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("tenant", user.getTenant().getName());
            assertEquals("subject", user.getSubject());
            assertNotNull(user.getId());
        }
    }

    @Test
    public void testFindByTenantNameAndSubject() {
        entityManager.persist(new User(tenant, "subject", "username"));
        entityManager.flush();
        entityManager.clear();
        User user =
                this.repository
                        .findBySubjectAndTenantName("subject", "tenant")
                        .orElseThrow(AssertionFailedError::new);
        assertEquals("tenant", user.getTenant().getName());
        assertEquals("subject", user.getSubject());
        assertNotNull(user.getId());
    }

    @Test
    public void testTenentIsolation() {
        Tenant other = new Tenant();
        other.setName("other");
        other.setUrl("http://example.com");
        entityManager.persist(other);
        entityManager.persist(new User(tenant, "subject", "username"));
        entityManager.persist(new User(other, "subject", "username"));
        entityManager.flush();
        entityManager.clear();

        {
            User user =
                    repository
                            .findBySubjectAndTenantName("subject", "tenant")
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("tenant", user.getTenant().getName());
            assertEquals("subject", user.getSubject());
        }
        {
            User user =
                    repository
                            .findBySubjectAndTenantName("subject", "other")
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("other", user.getTenant().getName());
            assertEquals("subject", user.getSubject());
        }
    }

    @Test
    public void testNotFound() {
        Optional<User> missing = this.repository.findBySubjectAndTenantName("noUser", "badTenant");
        assertFalse(missing.isPresent());
    }

    @Test
    public void testNoDuplicates() {
        assertThrows(
                PersistenceException.class,
                () -> {
                    // Check index prevents duplicates
                    entityManager.persist(new User(tenant, "subject", "username"));
                    entityManager.persist(new User(tenant, "subject", "username"));
                    entityManager.flush();
                });
    }

    @Test
    public void testDuplicateUsernames() {
        // Check we can have duplicate username as this happens when the same tool is launched from different
        // placements.
        entityManager.persist(new User(tenant, "subject1", "username"));
        entityManager.persist(new User(tenant, "subject2", "username"));
        entityManager.flush();
    }

    @Test
    public void testUpdateUser() {
        entityManager.persist(new User(tenant, "subject", "username"));
        entityManager.flush();
        {
            User user =
                    this.repository
                            .findBySubjectAndTenantName("subject", "tenant")
                            .orElseThrow(AssertionFailedError::new);
            assertNotNull(user.getId());
            user.setEmail("email@example.com");
            user.setName("Display Name");
            this.repository.save(user);
        }
        {
            User user =
                    this.repository
                            .findBySubjectAndTenantName("subject", "tenant")
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("email@example.com", user.getEmail());
            assertEquals("Display Name", user.getName());
        }
    }
}
