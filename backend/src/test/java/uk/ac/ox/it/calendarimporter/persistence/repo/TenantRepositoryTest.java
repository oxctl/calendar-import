package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DirtiesContext
public class TenantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantRepository repository;

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
                DataIntegrityViolationException.class,
                () -> {
                    Tenant other = new Tenant();
                    repository.save(other);
                });
    }

    @Test
    public void testSaveLoad() {
        long id;
        {
            Tenant other = new Tenant();
            other.setName("other");
            other.setUrl("http://example.com");
            id = repository.save(other).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            Tenant tenant =
                    this.repository
                            .findById(id)
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("other", tenant.getName());
            assertEquals(id, tenant.getId());
            assertEquals("http://example.com", tenant.getUrl());
        }
    }

    @Test
    public void testFindByName() {
        long id;
        {
            Tenant other = new Tenant();
            other.setName("other");
            other.setUrl("http://example.com");
            id = repository.save(other).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            Tenant tenant =
                    this.repository
                            .findByName("other")
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("other", tenant.getName());
            assertEquals(id, tenant.getId());
            assertEquals("http://example.com", tenant.getUrl());
        }
    }

    @Test
    public void testFindByLtiCleintId() {
        long id;
        {
            Tenant other = new Tenant();
            other.setName("other");
            other.setLtiClientId("ltiClientId");
            other.setUrl("http://example.com");
            id = repository.save(other).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            Tenant tenant =
                    this.repository
                            .findByLtiClientId("ltiClientId")
                            .orElseThrow(AssertionFailedError::new);
            assertEquals("other", tenant.getName());
            assertEquals(id, tenant.getId());
            assertEquals("ltiClientId", tenant.getLtiClientId());
            assertEquals("http://example.com", tenant.getUrl());
        }
    }
}
