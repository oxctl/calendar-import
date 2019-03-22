package uk.ac.ox.it.calendarimporter.persistence.repo;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEventIdentity;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ImportedEventRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ImportedEventRepository repository;

  @Test
  public void testSaveEvent() throws Exception {
    CalendarImport calendarImport = new CalendarImport();
    calendarImport = entityManager.persist(calendarImport);
    ImportedEvent event =
        new ImportedEvent(
            new ImportedEventIdentity(1L, 1), calendarImport, ImportedEvent.Status.CREATED);
    entityManager.persist(event);
    entityManager.flush();

    Optional<ImportedEvent> optional = repository.findById(new ImportedEventIdentity(1L, 1));
    assertTrue(optional.isPresent());
    ImportedEvent loaded = optional.get();
    assertEquals(ImportedEvent.Status.CREATED, loaded.getStatus());
    assertNotNull(loaded.getCalendarImport());
  }
}
