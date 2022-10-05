package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.canvas.model.CalendarEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ImportEventServiceTest {

    @Autowired
    ImportEventService importEventService;

    @Autowired
    ImportedEventRepository importedEventRepository;

    @Autowired
    CalendarImportRepository calendarImportRepository;

    @Test
    public void testEventCreated(){
        CalendarImport calendarImport = new CalendarImport();
        calendarImportRepository.save(calendarImport);
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(1);
        importEventService.eventCreated(123L, calendarImport, calendarEvent);
        ImportedEvent importedEvent = importedEventRepository.findByCalendarImport(calendarImport).get(0);
        assertNotNull(importedEvent);
        assertEquals(123L, importedEvent.getIdentity().getTenant());
    }

    @Test
    public void testEventDeleted(){
        CalendarImport calendarImport = new CalendarImport();
        calendarImportRepository.save(calendarImport);
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(1);
        importEventService.eventCreated(123L, calendarImport, calendarEvent);

        importEventService.eventDeleted(123L, calendarEvent);
        ImportedEvent importedEvent = importedEventRepository.findByCalendarImport(calendarImport).get(0);
        assertNotNull(importedEvent);
        assertEquals(123L, importedEvent.getIdentity().getTenant());
        assertEquals(ImportedEvent.Status.DELETED, importedEvent.getStatus());
    }

    @Test
    public void testEventMissing(){
        CalendarImport calendarImport = new CalendarImport();
        calendarImportRepository.save(calendarImport);
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(1);
        importEventService.eventCreated(123L, calendarImport, calendarEvent);

        importEventService.eventMissing(123L, calendarEvent);
        ImportedEvent importedEvent = importedEventRepository.findByCalendarImport(calendarImport).get(0);
        assertNotNull(importedEvent);
        assertEquals(123L, importedEvent.getIdentity().getTenant());
        assertEquals(ImportedEvent.Status.MISSING, importedEvent.getStatus());
    }
}
