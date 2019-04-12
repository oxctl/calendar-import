package uk.ac.ox.it.calendarimporter.persistence.repo;

import java.util.Optional;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

@RepositoryRestResource
public interface TenantRepository extends CrudRepository<Tenant, Long> {

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<Tenant> findByName(String name);
}
