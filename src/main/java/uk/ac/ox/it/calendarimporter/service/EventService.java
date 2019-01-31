package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.canvas.model.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEventIdentity;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;

/**
 * When an event gets posted to Canvas this service should get called to do any additional local processing.
 */
@Component
public class EventService {

    @Autowired
    private ImportedEventRepository importedEventRepository;

    public void eventCreated(Long tenantId, CalendarImport calendarImport, CalendarEvent calendarEvent) {
        ImportedEventIdentity identity = new ImportedEventIdentity(tenantId, calendarEvent.getId());
        ImportedEvent event = new ImportedEvent(identity, calendarImport, ImportedEvent.Status.CREATED);
        importedEventRepository.save(event);
    }

}
