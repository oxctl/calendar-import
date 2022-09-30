package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class CalendarImportRepositoryTest {

    @Autowired
    private CalendarImportRepository repository;

    @Test
    public void testSaveMissingFields() {
        long id;
        {
            CalendarImport calendarImport = new CalendarImport();
            id = repository.save(calendarImport).getId();
        }
        {
            Optional<CalendarImport> calendarImport = repository.findById(id);
            assertTrue(calendarImport.isPresent());
        }
    }

    @Test
    public void testSaveLoad() {
        long id;
        {
            CalendarImport calendarImport = new CalendarImport();
            calendarImport.setContext("context");
            calendarImport.setCreated(Instant.now());
            id = repository.save(calendarImport).getId();
        }
        {
            Optional<CalendarImport> calendarImport = repository.findById(id);
            assertTrue(calendarImport.isPresent());
        }
    }
}
