package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;

public interface UserTokensRepository extends CrudRepository<UserTokens, String> {}
