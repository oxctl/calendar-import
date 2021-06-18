package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ImportedEventRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private ImportedEventRepository repository;

  @Test
  public void testSaveEvent() {
    CalendarImport calendarImport = new CalendarImport();
    calendarImport = entityManager.persist(calendarImport);
    ImportedEvent event =
        new ImportedEvent(
            new ImportedEvent.ImportedEventIdentity(1L, 1),
            calendarImport,
            ImportedEvent.Status.CREATED);
    entityManager.persist(event);
    entityManager.flush();

    Optional<ImportedEvent> optional =
        repository.findById(new ImportedEvent.ImportedEventIdentity(1L, 1));
    assertTrue(optional.isPresent());
    ImportedEvent loaded = optional.get();
    assertEquals(ImportedEvent.Status.CREATED, loaded.getStatus());
    assertNotNull(loaded.getCalendarImport());
  }
}
