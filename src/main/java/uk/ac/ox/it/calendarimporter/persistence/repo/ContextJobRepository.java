package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;

public interface ContextJobRepository extends CrudRepository<ContextJob, String> {

    Page<ContextJob> findByContextOrderByCreatedDesc(String context, Pageable pageable);

}
