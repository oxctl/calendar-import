package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;

import java.util.List;

/**
 * Stores details of each event imported into a calendar. This allows us to remove all the events at
 * a later point.
 */
public interface ImportedEventRepository
        extends CrudRepository<ImportedEvent, ImportedEvent.ImportedEventIdentity> {

    List<ImportedEvent> findByCalendarImport(CalendarImport calendarImport);
}
