package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import javax.persistence.QueryHint;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findBySubjectAndTenantName(String subject, String tenantName);
}
