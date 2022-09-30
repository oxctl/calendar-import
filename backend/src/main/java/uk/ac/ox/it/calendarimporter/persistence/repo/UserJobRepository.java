package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;

public interface UserJobRepository extends CrudRepository<UserJob, String> {

}
