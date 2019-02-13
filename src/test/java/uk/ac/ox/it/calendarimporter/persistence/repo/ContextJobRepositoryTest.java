package uk.ac.ox.it.calendarimporter.persistence.repo;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Iterator;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ContextJobRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContextJobRepository repository;
    private Tenant tenant;
    private CalendarImport calendarImport;

    @Before
    public void setUp() {
        tenant = new Tenant();
        tenant.setName("test");
        tenant.setUrl("http://example.com");
        entityManager.persist(tenant);

        calendarImport = new CalendarImport();
        entityManager.persist(calendarImport);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testSaveMissingFields() {
        ContextJob job = new ContextJob();
        repository.save(job);
        entityManager.flush();
    }

    @Test
    public void testSaveLoad() throws Exception {
        long id;
        {
            ContextJob job = new ContextJob();
            job.setTenant(tenant);
            job.setContext("context_1");
            job.setCreated(Instant.now());
            job.setCalendarImport(calendarImport);
            id = repository.save(job).getId();
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
        }
        {
            Page<ContextJob> jobs = repository.findByTenantAndContextOrderByCreatedDesc(tenant, "context_1", null);
            assertFalse(jobs.isEmpty());
            Iterator<ContextJob> iterator = jobs.iterator();
            assertTrue(iterator.hasNext());
            ContextJob job = iterator.next();
            assertEquals(id, job.getId());
        }
    }


}
