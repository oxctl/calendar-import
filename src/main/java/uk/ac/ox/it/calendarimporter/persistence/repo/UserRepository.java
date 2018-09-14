package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

public interface UserRepository extends CrudRepository<User, Long> {
}
