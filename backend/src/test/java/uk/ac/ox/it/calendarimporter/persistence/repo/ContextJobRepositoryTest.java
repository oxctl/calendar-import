package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import java.time.Instant;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ContextJobRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContextJobRepository repository;
    private Tenant tenant;
    private CalendarImport calendarImport;

    @BeforeEach
    public void setUp() {
        tenant = new Tenant();
        tenant.setName("test");
        tenant.setUrl("http://example.com");
        entityManager.persist(tenant);

        CalendarImport calendarImport = new CalendarImport();
        this.calendarImport = entityManager.persist(calendarImport);
    }

    @Test
    public void testSaveMissingFields() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    ContextJob job = new ContextJob();
                    repository.save(job);
                    entityManager.flush();
                });
    }

    @Test
    public void testSaveLoad() {
        long id;
        {
            ContextJob job = new ContextJob();
            job.setTenant(tenant);
            job.setContext("context_1");
            job.setCreated(Instant.now());
            job.setCalendarImport(calendarImport);
            id = repository.save(job).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            ContextJob job = repository.findById(id).orElseThrow(AssertionError::new);
            assertEquals(tenant, job.getTenant());
            assertEquals("context_1", job.getContext());
            assertNotNull(job.getCreated());
            assertEquals(calendarImport, job.getCalendarImport());
        }
    }

    @Test
    public void testFindByTenentAndContext() {
        long id;
        {
            ContextJob job = new ContextJob();
            job.setTenant(tenant);
            job.setContext("context_1");
            job.setCreated(Instant.now());
            job.setCalendarImport(calendarImport);
            id = repository.save(job).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            Page<ContextJob> jobs =
                    repository.findByTenantAndContextAndHiddenOrderByCreatedDesc(
                            tenant, "context_1", false, Pageable.unpaged());
            assertFalse(jobs.isEmpty());
            Iterator<ContextJob> iterator = jobs.iterator();
            assertTrue(iterator.hasNext());
            ContextJob job = iterator.next();
            assertEquals(id, job.getId());
        }
    }

    @Test
    public void testFindByTenentNameAndContext() {
        long id;
        {
            ContextJob job = new ContextJob();
            job.setTenant(tenant);
            job.setContext("context_1");
            job.setCreated(Instant.now());
            job.setCalendarImport(calendarImport);
            id = repository.save(job).getId();
            entityManager.flush();
            entityManager.clear();
        }
        {
            Page<ContextJob> jobs =
                    repository.findByTenantNameAndContextAndHiddenOrderByCreatedDesc(
                            tenant.getName(), "context_1", false, Pageable.unpaged());
            assertFalse(jobs.isEmpty());
            Iterator<ContextJob> iterator = jobs.iterator();
            assertTrue(iterator.hasNext());
            ContextJob job = iterator.next();
            assertEquals(id, job.getId());
        }
    }
}
