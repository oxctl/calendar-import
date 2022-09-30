package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.jpa.JpaSystemException;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ImportedEventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ImportedEventRepository repository;

    @Test
    public void testSaveMissingFields() {
        assertThrows(
                JpaSystemException.class,
                () -> {
                    ImportedEvent event =
                            new ImportedEvent(
                                    null,
                                    null,
                                    ImportedEvent.Status.CREATED);
                    repository.save(event);
                });
    }

    @Test
    public void testSaveLoad() {
        long id;
        {
            CalendarImport calendarImport = new CalendarImport();
            calendarImport = entityManager.persist(calendarImport);
            ImportedEvent event =
                    new ImportedEvent(
                            new ImportedEvent.ImportedEventIdentity(1L, 1),
                            calendarImport,
                            ImportedEvent.Status.CREATED);
            id = repository.save(event).getId();
        }
        {
            Optional<ImportedEvent> optional =
                    repository.findById(new ImportedEvent.ImportedEventIdentity(1L, 1));
            assertTrue(optional.isPresent());
            ImportedEvent loaded = optional.get();
            assertEquals(ImportedEvent.Status.CREATED, loaded.getStatus());
            assertNotNull(loaded.getCalendarImport());
        }
    }

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
    
    @Test
    public void testFindByCalendarImportAndStatusIn() {
        CalendarImport calendarImport = new CalendarImport();
        calendarImport = entityManager.persist(calendarImport);
        ImportedEvent created = new ImportedEvent(
                new ImportedEvent.ImportedEventIdentity(1L, 1),
                calendarImport,
                ImportedEvent.Status.CREATED
        );
        created = entityManager.persist(created);
        ImportedEvent deleted = new ImportedEvent(
                new ImportedEvent.ImportedEventIdentity(1L, 2),
                calendarImport,
                ImportedEvent.Status.DELETED
        );
        deleted = entityManager.persist(deleted);
        ImportedEvent missing = new ImportedEvent(
                new ImportedEvent.ImportedEventIdentity(1L, 3),
                calendarImport,
                ImportedEvent.Status.MISSING
        );
        missing = entityManager.persist(missing);

        {
            CalendarImport other = new CalendarImport();
            other = entityManager.persist(other);
            ImportedEvent otherCreated = new ImportedEvent(
                    new ImportedEvent.ImportedEventIdentity(1L, 4),
                    other,
                    ImportedEvent.Status.CREATED
            );
            otherCreated = entityManager.persist(otherCreated);
        }
        
        entityManager.flush();
        
        List<ImportedEvent> events = repository.findByCalendarImportAndStatusIn(calendarImport, ImportedEvent.Status.CREATED);
        assertThat(events).containsExactly(created);
    }

    @Test
    public void testFindByCalendarImport() {
        CalendarImport calendarImport = new CalendarImport();
        calendarImport = entityManager.persist(calendarImport);
        ImportedEvent created = new ImportedEvent(
                new ImportedEvent.ImportedEventIdentity(1L, 1),
                calendarImport,
                ImportedEvent.Status.CREATED
        );
        created = entityManager.persist(created);
        entityManager.flush();

        List<ImportedEvent> events = repository.findByCalendarImport(calendarImport);
        assertThat(events).containsExactly(created);
    }
}
