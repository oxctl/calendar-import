package uk.ac.ox.it.calendarimporter.persistence.repo;

import java.util.Optional;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<User> findByUsernameAndTenant_Name(String username, String tenantName);

  default Optional<User> findByOAuth2AuthenticationToken(OAuth2AuthenticationToken token) {
    return findByUsernameAndTenant_Name(token.getName(), token.getAuthorizedClientRegistrationId());
  }
}
