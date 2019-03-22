package uk.ac.ox.it.calendarimporter.persistence.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

public interface TenantRepository extends CrudRepository<Tenant, Long> {

  Optional<Tenant> findByName(String name);
}
