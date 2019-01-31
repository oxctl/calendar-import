package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import java.util.Optional;

public interface TenantRepository extends CrudRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);

}
