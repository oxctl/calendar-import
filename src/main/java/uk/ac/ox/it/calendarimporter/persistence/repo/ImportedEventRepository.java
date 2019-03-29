package uk.ac.ox.it.calendarimporter.persistence.repo;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;

public interface ImportedEventRepository
    extends CrudRepository<ImportedEvent, ImportedEvent.ImportedEventIdentity> {

  List<ImportedEvent> findByCalendarImport(CalendarImport calendarImport);
}
