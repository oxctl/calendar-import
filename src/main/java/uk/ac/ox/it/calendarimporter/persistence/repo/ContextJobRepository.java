package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

public interface ContextJobRepository extends CrudRepository<ContextJob, Long> {

  Page<ContextJob> findByTenantAndContextOrderByCreatedDesc(
      Tenant tenant, String context, Pageable pageable);
}
