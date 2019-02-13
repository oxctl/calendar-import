package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEventIdentity;

import java.util.List;

public interface ImportedEventRepository extends CrudRepository<ImportedEvent, ImportedEventIdentity> {

    List<ImportedEvent> findByCalendarImport(CalendarImport calendarImport);

}
