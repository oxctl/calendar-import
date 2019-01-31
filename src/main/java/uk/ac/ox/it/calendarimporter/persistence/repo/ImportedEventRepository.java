package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEventIdentity;

public interface ImportedEventRepository extends CrudRepository<ImportedEvent, ImportedEventIdentity> {

}
